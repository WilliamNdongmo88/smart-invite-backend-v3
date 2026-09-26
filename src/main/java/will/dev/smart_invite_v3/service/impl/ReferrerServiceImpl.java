package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.referrer.ReferralCheckResponse;
import will.dev.smart_invite_v3.dto.referrer.ReferrerRequest;
import will.dev.smart_invite_v3.dto.referrer.ReferrerResponse;
import will.dev.smart_invite_v3.entity.Referrer;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.exception.ReferralCodeException;
import will.dev.smart_invite_v3.repository.ReferrerRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.ReferrerService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferrerServiceImpl implements ReferrerService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ReferrerRepository      referrerRepository;
    private final EmailService            emailService;
    private final WhatsAppService         whatsAppService;
    private final NotificationAlertService alertService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.phone}")
    private String adminPhone;

    // ── Création ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReferrerResponse create(ReferrerRequest request) {
        Referrer referrer = Referrer.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .code(generateUniqueCode())
                .notificationMode(request.notificationMode() != null
                        ? request.notificationMode()
                        : NotificationMode.EMAIL)
                .isActive(true)
                .build();
        Referrer saved = referrerRepository.save(referrer);
        notifyNewReferrer(saved);
        return toResponse(saved);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerResponse> findAll() {
        return referrerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Mise à jour ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReferrerResponse update(Long id, ReferrerRequest request) {
        Referrer referrer = referrerRepository.findById(id)
                .orElseThrow(() -> new ReferralCodeException("Recommandateur introuvable"));
        referrer.setName(request.name());
        referrer.setPhone(request.phone());
        referrer.setEmail(request.email());
        if (request.notificationMode() != null) {
            referrer.setNotificationMode(request.notificationMode());
        }
        return toResponse(referrerRepository.save(referrer));
    }

    // ── Toggle actif/inactif ──────────────────────────────────────────────────

    @Override
    @Transactional
    public ReferrerResponse toggleActive(Long id) {
        Referrer referrer = referrerRepository.findById(id)
                .orElseThrow(() -> new ReferralCodeException("Recommandateur introuvable"));
        referrer.setIsActive(!referrer.getIsActive());
        return toResponse(referrerRepository.save(referrer));
    }

    // ── Validation publique ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReferralCheckResponse check(String code) {
        if (code == null || code.isBlank()) {
            return new ReferralCheckResponse(false, null);
        }
        return referrerRepository.findByCode(code.trim())
                .map(r -> new ReferralCheckResponse(Boolean.TRUE.equals(r.getIsActive()), r.getName()))
                .orElse(new ReferralCheckResponse(false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Referrer getActiveByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ReferralCodeException("Le code de recommandation est obligatoire");
        }
        Referrer referrer = referrerRepository.findByCode(code.trim())
                .orElseThrow(() -> new ReferralCodeException("Ce code de recommandation est invalide"));
        if (!Boolean.TRUE.equals(referrer.getIsActive())) {
            throw new ReferralCodeException("Ce code de recommandation est désactivé");
        }
        return referrer;
    }

    // ── Suppression ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void delete(Long id) {
        Referrer referrer = referrerRepository.findById(id)
                .orElseThrow(() -> new ReferralCodeException("Recommandateur introuvable"));
        referrerRepository.delete(referrer);
    }

    // ── Notification de bienvenue ─────────────────────────────────────────────

    /**
     * Envoie une notification de bienvenue au nouveau recommandateur.
     * Si la notification échoue, l'admin est alerté par email et une exception
     * est propagée — ce qui déclenche le rollback de la transaction de création.
     */
    private void notifyNewReferrer(Referrer referrer) {
        NotificationMode mode = referrer.getNotificationMode() != null
                ? referrer.getNotificationMode()
                : NotificationMode.EMAIL;

        if ((mode == NotificationMode.EMAIL || mode == NotificationMode.BOTH)
                && referrer.getEmail() != null && !referrer.getEmail().isBlank()) {
            try {
                emailService.sendReferrerWelcomeNotification(
                        referrer.getEmail(), referrer.getName(), referrer.getCode());
            } catch (Exception e) {
                log.warn("[Referrer] Échec notification email de bienvenue pour {} : {}",
                        referrer.getName(), e.getMessage());
                alertService.alertOnEmailFailure(
                        "notification email de bienvenue — referral " + referrer.getName(),
                        referrer.getEmail(), e);
                throw new ReferralCodeException(
                        "Le recommandateur a été créé mais l'email de notification n'a pas pu être envoyé " +
                        "à " + referrer.getEmail() + ". Veuillez vérifier l'adresse et réessayer.");
            }
        }

        if ((mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH)
                && referrer.getPhone() != null && !referrer.getPhone().isBlank()) {
            try {
                whatsAppService.sendReferrerWelcomeMessage(
                        referrer.getPhone(), referrer.getName(), referrer.getCode());
            } catch (Exception e) {
                log.warn("[Referrer] Échec notification WhatsApp de bienvenue pour {} : {}",
                        referrer.getName(), e.getMessage());
                alertService.alertOnWhatsAppFailure(
                        "notification WhatsApp de bienvenue — referral " + referrer.getName(),
                        referrer.getPhone(), e);
                throw new ReferralCodeException(
                        "Impossible d'envoyer le message WhatsApp au " + referrer.getPhone() +
                        ". Vérifiez que ce numéro possède un compte WhatsApp actif et réessayez.");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReferrerResponse toResponse(Referrer referrer) {
        return new ReferrerResponse(
                referrer.getId(),
                referrer.getName(),
                referrer.getPhone(),
                referrer.getEmail(),
                referrer.getCode(),
                referrer.getNotificationMode(),
                referrer.getIsActive(),
                referrer.getCreatedAt(),
                referrerRepository.countRegistrationsByCode(referrer.getCode()),
                referrerRepository.countEventsByCode(referrer.getCode()),
                referrerRepository.sumApprovedAmountByCode(referrer.getCode(), PaymentStatus.APPROVED)
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("REF-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (referrerRepository.existsByCode(code));
        return code;
    }
}