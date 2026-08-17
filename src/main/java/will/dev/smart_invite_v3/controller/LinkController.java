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
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;
import will.dev.smart_invite_v3.dto.link.request.CreateLinkRequest;
import will.dev.smart_invite_v3.dto.link.request.UpdateLinkRequest;
import will.dev.smart_invite_v3.dto.link.response.LinkPreviewResponse;
import will.dev.smart_invite_v3.dto.link.response.LinkResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.LinkService;

import java.util.List;

@RestController
@RequestMapping("/api/link")
@RequiredArgsConstructor
@Tag(name = "Links", description = "Liens d'auto-inscription pour invités")
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/add-link")
    @Operation(summary = "US-035 — Créer un lien d'auto-inscription")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<LinkResponse>> create(
            @Valid @RequestBody CreateLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                linkService.create(request, userDetails.getUser().getId()),
                "Lien créé"));
    }

    @GetMapping("/get-links")
    @Operation(summary = "US-036 — Liste des liens d'un événement")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<LinkResponse>>> getLinks(
            @RequestParam Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                linkService.getByEvent(eventId, userDetails.getUser().getId()),
                "Liens récupérés"));
    }

    @PutMapping("/edit-link/{linkId}")
    @Operation(summary = "US-036 — Modifier un lien (limite, expiration)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<LinkResponse>> update(
            @PathVariable Long linkId,
            @RequestBody UpdateLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                linkService.update(linkId, request, userDetails.getUser().getId()),
                "Lien mis à jour"));
    }

    @DeleteMapping("/delete-link/{linkId}")
    @Operation(summary = "US-036 — Supprimer un lien")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long linkId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        linkService.delete(linkId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Lien supprimé"));
    }

    @GetMapping("/preview/{token}")
    @Operation(summary = "Preview public d'un lien (sans auth)")
    public ResponseEntity<ApiResponse<LinkPreviewResponse>> preview(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(linkService.preview(token), "Preview récupéré"));
    }

    @PostMapping("/join/{token}")
    @Operation(summary = "Auto-inscription via lien public (sans auth)")
    public ResponseEntity<ApiResponse<InvitationResponse>> join(
            @PathVariable String token,
            @Valid @RequestBody CreateGuestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                linkService.join(token, request),
                "Inscription réussie — invitation générée"));
    }
}
