package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.contact.ContactRequest;
import will.dev.smart_invite_v3.dto.contact.ContactResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.ContactService;

/**
 * Endpoint public de contact.
 * Accessible sans authentification (visiteurs anonymes et utilisateurs connectés).
 * POST /api/contact
 */
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Formulaire de contact public")
public class ContactController {

    private final ContactService contactService;

    /**
     * Soumet un message de contact.
     * - Persiste le message dans la table usernews.
     * - Notifie l'admin via WhatsApp ou Email selon le canal choisi.
     * - L'utilisateur connecté est automatiquement rattaché s'il envoie un JWT valide.
     */
    @PostMapping
    @Operation(summary = "Envoyer un message de contact (public)")
    public ResponseEntity<ApiResponse<ContactResponse>> submit(
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUser().getId() : null;
        ContactResponse response = contactService.submitContact(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Message envoyé avec succès"));
    }
}
