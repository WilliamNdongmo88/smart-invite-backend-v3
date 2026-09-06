package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.admin.ContactReplyRequest;
import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.UserNewsResponse;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Gestion des utilisateurs par l'administrateur")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/organizers")
    @Operation(summary = "Récupérer tous les organisateurs avec leurs événements et statuts de paiement")
    public ResponseEntity<ApiResponse<List<OrganizerSummaryResponse>>> getAllOrganizers() {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAllOrganizers(),
                "Organisateurs récupérés"));
    }

    @PatchMapping("/users/{userId}/block")
    @Operation(summary = "Bloquer un utilisateur")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long userId) {
        adminService.blockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur bloqué"));
    }

    @PatchMapping("/users/{userId}/unblock")
    @Operation(summary = "Débloquer un utilisateur")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur débloqué"));
    }

    @PatchMapping("/users/{userId}/activate")
    @Operation(summary = "Activer un utilisateur")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur activé"));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé"));
    }

    // ──────────── Messages de contact (usernews) ────────────────────────

    @GetMapping("/contacts")
    @Operation(summary = "Lister tous les messages de contact reçus (table usernews)")
    public ResponseEntity<ApiResponse<List<UserNewsResponse>>> getAllContacts() {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAllContacts(),
                "Messages récupérés"));
    }

    @PostMapping("/contacts/{id}/reply")
    @Operation(summary = "Répondre à un message de contact via WhatsApp ou Email")
    public ResponseEntity<ApiResponse<Void>> replyToContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyRequest request) {
        adminService.replyToContact(id, request);
        return ResponseEntity.ok(ApiResponse.success("Réponse envoyée"));
    }
}
