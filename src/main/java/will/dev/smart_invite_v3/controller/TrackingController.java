package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.analytics.EndSessionRequest;
import will.dev.smart_invite_v3.dto.analytics.HeartbeatRequest;
import will.dev.smart_invite_v3.dto.analytics.TrackPageViewRequest;
import will.dev.smart_invite_v3.dto.analytics.TrackResponse;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.entity.Visitor;
import will.dev.smart_invite_v3.service.impl.VisitorTrackingService;

/**
 * Endpoints publics de tracking — aucune authentification requise.
 * Les routes /api/track/** sont whitelistées dans SecurityConfig.
 */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
@Tag(name = "Tracking", description = "Tracking anonyme des visiteurs")
public class TrackingController {

    private final VisitorTrackingService trackingService;

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/track/pageview
    // ─────────────────────────────────────────────────────────────────

    /**
     * Enregistre une page vue.
     *
     * Si {@code sessionId} est null, une nouvelle session est créée.
     * Retourne toujours le sessionId actif pour que le client le stocke.
     */
    @PostMapping("/pageview")
    @Operation(summary = "Enregistrer une page vue")
    public ResponseEntity<ApiResponse<TrackResponse>> trackPageView(
            @Valid @RequestBody TrackPageViewRequest req,
            HttpServletRequest httpReq) {

        String ip        = extractIp(httpReq);
        String userAgent = httpReq.getHeader("User-Agent");

        Long sessionId = req.sessionId();

        // Nouvelle session si pas de sessionId fourni
        if (sessionId == null) {
            Visitor visitor = trackingService.identifyVisitor(ip, userAgent);
            sessionId = trackingService.startSession(visitor);
        }

        trackingService.trackPageView(sessionId, req.pageUrl());

        return ResponseEntity.ok(ApiResponse.success(
                TrackResponse.of(sessionId),
                "Page vue enregistrée"
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/track/heartbeat
    // ─────────────────────────────────────────────────────────────────

    /**
     * Maintient la session active (évite la fermeture par le job de nettoyage).
     * Le client appelle cet endpoint toutes les ~2 minutes.
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "Maintenir la session active")
    public ResponseEntity<ApiResponse<TrackResponse>> heartbeat(
            @Valid @RequestBody HeartbeatRequest req) {

        boolean exists = trackingService.sessionExists(req.sessionId());
        if (!exists) {
            return ResponseEntity.ok(ApiResponse.success(
                    new TrackResponse(null, "session_expired"),
                    "Session expirée"
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(
                TrackResponse.of(req.sessionId()),
                "Session active"
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/track/end
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ferme explicitement une session (au moment où l'utilisateur quitte la page).
     */
    @PostMapping("/end")
    @Operation(summary = "Terminer une session")
    public ResponseEntity<ApiResponse<Void>> endSession(
            @Valid @RequestBody EndSessionRequest req) {

        trackingService.endSession(req.sessionId());
        return ResponseEntity.ok(ApiResponse.success("Session terminée"));
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extrait l'IP réelle en tenant compte des proxies/CDN.
     * Priorité : X-Forwarded-For → X-Real-IP → remoteAddr
     */
    private String extractIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr();
    }
}
