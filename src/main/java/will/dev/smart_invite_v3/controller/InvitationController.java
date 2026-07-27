package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.invitation.request.BulkGenerateRequest;
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.request.RsvpRequest;
import will.dev.smart_invite_v3.dto.invitation.response.BulkGenerateResponse;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;
import will.dev.smart_invite_v3.dto.invitation.response.PublicInvitationResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.InvitationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Gestion des invitations numeriques")
public class InvitationController {

    private final InvitationService invitationService;

    // ---- Organisateur (authentifié) ----

    @PostMapping("/api/events/{eventId}/invitations/generate")
    @Operation(summary = "US-020 — Générer une invitation pour un invité")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<InvitationResponse>> generate(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateGuestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                invitationService.generate(eventId, request, userDetails.getUser().getId()),
                "Invitation générée"));
    }

    @PostMapping("/api/invitations/bulk-generate")
    @Operation(summary = "US-021 — Génération en masse d'invitations")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BulkGenerateResponse>> bulkGenerate(
            @Valid @RequestBody BulkGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.bulkGenerate(request, userDetails.getUser().getId()),
                "Génération en masse terminée"));
    }

    @GetMapping("/api/events/{eventId}/invitations")
    @Operation(summary = "Liste des invitations d'un événement")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> list(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.listByEvent(eventId, userDetails.getUser().getId()),
                "Liste récupérée"));
    }

    @DeleteMapping("/api/invitations/{id}")
    @Operation(summary = "US-024 — Supprimer une invitation")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        invitationService.delete(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation supprimée"));
    }

    // ---- Public (sans auth) ----

    @GetMapping("/api/invitations/{token}")
    @Operation(summary = "US-022 — Vue publique d'une invitation (sans authentification)")
    public ResponseEntity<ApiResponse<PublicInvitationResponse>> getPublic(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.getPublic(token),
                "Invitation récupérée"));
    }

    @PostMapping("/api/invitations/{token}/rsvp")
    @Operation(summary = "Réponse RSVP de l'invité")
    public ResponseEntity<ApiResponse<PublicInvitationResponse>> rsvp(
            @PathVariable String token,
            @Valid @RequestBody RsvpRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.rsvp(token, request),
                "RSVP enregistré"));
    }
}
