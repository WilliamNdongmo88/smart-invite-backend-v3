package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.entity.Visitor;
import will.dev.smart_invite_v3.entity.VisitorPageView;
import will.dev.smart_invite_v3.entity.VisitorSession;
import will.dev.smart_invite_v3.repository.VisitorPageViewRepository;
import will.dev.smart_invite_v3.repository.VisitorRepository;
import will.dev.smart_invite_v3.repository.VisitorSessionRepository;

import java.time.LocalDateTime;

/**
 * Service de tracking des visiteurs.
 *
 * Responsabilités :
 *  - Identifier/créer un visiteur à partir de son IP + User-Agent
 *  - Démarrer / fermer une session
 *  - Enregistrer les pages vues
 *  - Nettoyer les sessions inactives (job planifié)
 *
 * Note RGPD : l'IP est stockée telle quelle. Pour anonymiser,
 * appliquer ipAddress.replaceAll("\\.[^.]+$", ".0") avant persistance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorTrackingService {

    private static final int INACTIVE_TIMEOUT_MINUTES = 30;

    private final VisitorRepository         visitorRepo;
    private final VisitorSessionRepository  sessionRepo;
    private final VisitorPageViewRepository pageViewRepo;
    private final UserAgentParserService    uaParser;

    // ─────────────────────────────────────────────────────────────────
    //  identifyVisitor
    // ─────────────────────────────────────────────────────────────────

    /**
     * Identifie le visiteur via IP + User-Agent.
     * Si le couple (ip, device) existe déjà, retourne le visiteur existant.
     * Sinon, crée un nouveau visiteur en base.
     *
     * @param ip        adresse IP extraite de la requête HTTP
     * @param userAgent valeur de l'en-tête User-Agent
     * @return le visiteur identifié ou créé
     */
    @Transactional
    public Visitor identifyVisitor(String ip, String userAgent) {
        UserAgentParserService.ParsedUA parsed = uaParser.parse(userAgent);

        // Anonymisation RGPD : tronquer le dernier octet IPv4
        String anonymizedIp = anonymize(ip);

        return visitorRepo.findByIpAddressAndDevice(anonymizedIp, parsed.device())
                .orElseGet(() -> visitorRepo.save(
                        Visitor.builder()
                                .ipAddress(anonymizedIp)
                                .device(parsed.device())
                                .os(parsed.os())
                                .browser(parsed.browser())
                                // country/city : enrichissement externe optionnel (GeoIP)
                                .country(null)
                                .city(null)
                                .build()
                ));
    }

    // ─────────────────────────────────────────────────────────────────
    //  startSession
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crée une nouvelle session pour le visiteur donné.
     *
     * @return l'ID de la session créée
     */
    @Transactional
    public Long startSession(Visitor visitor) {
        VisitorSession session = VisitorSession.builder()
                .visitor(visitor)
                .startedAt(LocalDateTime.now())
                .build();
        return sessionRepo.save(session).getId();
    }

    // ─────────────────────────────────────────────────────────────────
    //  trackPageView
    // ─────────────────────────────────────────────────────────────────

    /**
     * Enregistre une page vue dans la session donnée.
     */
    @Transactional
    public void trackPageView(Long sessionId, String pageUrl) {
        VisitorSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        pageViewRepo.save(
                VisitorPageView.builder()
                        .session(session)
                        .pageUrl(pageUrl)
                        .viewedAt(LocalDateTime.now())
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    //  endSession
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ferme une session et calcule sa durée en secondes.
     */
    @Transactional
    public void endSession(Long sessionId) {
        sessionRepo.findById(sessionId).ifPresent(session -> {
            if (session.getEndedAt() != null) return; // déjà fermée

            LocalDateTime now = LocalDateTime.now();
            session.setEndedAt(now);

            if (session.getStartedAt() != null) {
                long seconds = java.time.Duration.between(session.getStartedAt(), now).getSeconds();
                session.setDurationSeconds((int) Math.max(0, seconds));
            }

            sessionRepo.save(session);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  heartbeat  (maintenir la session active)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Le heartbeat ne met pas à jour ended_at (la session reste ouverte).
     * Il sert uniquement à confirmer que la session existe encore.
     * Le timeout de 30 min est réinitialisé côté job grâce aux page views.
     */
    @Transactional(readOnly = true)
    public boolean sessionExists(Long sessionId) {
        return sessionRepo.existsById(sessionId);
    }

    // ─────────────────────────────────────────────────────────────────
    //  cleanupInactiveSessions  — job planifié toutes les 5 minutes
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ferme automatiquement les sessions ouvertes depuis plus de 30 minutes.
     * Planifié toutes les 5 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1_000)
    @Transactional
    public void cleanupInactiveSessions() {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime limit = now.minusMinutes(INACTIVE_TIMEOUT_MINUTES);
        int closed = sessionRepo.closeInactiveSessions(now, limit);
        if (closed > 0) {
            log.info("[Tracking] {} session(s) inactive(s) fermée(s)", closed);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Anonymise une adresse IPv4 en tronquant le dernier octet (RGPD).
     * Ex : "192.168.1.42" → "192.168.1.0"
     * Les adresses IPv6 sont tronquées au préfixe /64.
     */
    private String anonymize(String ip) {
        if (ip == null) return null;
        if (ip.contains(":")) {
            // IPv6 : garder les 4 premiers groupes
            String[] parts = ip.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":0:0:0:0";
            }
            return ip;
        }
        // IPv4 : remplacer le dernier octet par 0
        return ip.replaceAll("\\.[^.]+$", ".0");
    }
}
