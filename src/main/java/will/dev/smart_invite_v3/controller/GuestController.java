package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.guest.request.AddGuestRequest;
import will.dev.smart_invite_v3.dto.guest.request.BulkDeleteRequest;
import will.dev.smart_invite_v3.dto.guest.request.UpdateGuestRequest;
import will.dev.smart_invite_v3.dto.guest.response.GuestResponse;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.GuestService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Guests", description = "Gestion des invités")
@SecurityRequirement(name = "bearerAuth")
public class GuestController {

    private final GuestService guestService;

    @PostMapping("/api/events/{eventId}/guests")
    @Operation(summary = "US-015 — Ajouter un invité à un événement")
    public ResponseEntity<ApiResponse<GuestResponse>> add(
            @PathVariable Long eventId,
            @Valid @RequestBody AddGuestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                guestService.add(eventId, request, userDetails.getUser().getId()),
                "Invité ajouté"));
    }

    @GetMapping("/api/events/{eventId}/guests")
    @Operation(summary = "Liste des invités avec pagination et filtres")
    public ResponseEntity<ApiResponse<Page<GuestResponse>>> list(
            @PathVariable Long eventId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RsvpStatus rsvp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guestService.list(eventId, search, rsvp,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()),
                        userDetails.getUser().getId()),
                "Liste récupérée"));
    }

    @PutMapping("/api/guests/{guestId}")
    @Operation(summary = "US-017 — Modifier un invité")
    public ResponseEntity<ApiResponse<GuestResponse>> update(
            @PathVariable Long guestId,
            @RequestBody UpdateGuestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guestService.update(guestId, request, userDetails.getUser().getId()),
                "Invité mis à jour"));
    }

    @DeleteMapping("/api/guests/{guestId}")
    @Operation(summary = "US-018 — Supprimer un invité")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long guestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        guestService.delete(guestId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Invité supprimé"));
    }

    @DeleteMapping("/api/guests/bulk")
    @Operation(summary = "US-018 — Suppression multiple d'invités")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(
            @Valid @RequestBody BulkDeleteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        guestService.bulkDelete(request, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Invités supprimés"));
    }

    @PostMapping("/api/guests/{guestId}/reminder")
    @Operation(summary = "US-019 — Envoyer un rappel à un invité")
    public ResponseEntity<ApiResponse<Void>> sendReminder(
            @PathVariable Long guestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        guestService.sendReminder(guestId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Rappel envoyé"));
    }
}
