package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.profile.request.ChangePasswordRequest;
import will.dev.smart_invite_v3.dto.profile.request.UpdateProfileRequest;
import will.dev.smart_invite_v3.dto.profile.response.ProfileResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile", description = "Gestion du profil utilisateur")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "US-006 — Récupérer le profil")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(userDetails.getUser().getId()),
                "Profil récupéré"));
    }

    @PutMapping
    @Operation(summary = "US-006 — Mettre à jour le profil")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateProfile(userDetails.getUser().getId(), request),
                "Profil mis à jour"));
    }

    @PostMapping("/avatar")
    @Operation(summary = "US-006 — Uploader un avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String url = profileService.uploadAvatar(userDetails.getUser().getId(), file);
        return ResponseEntity.ok(ApiResponse.success(url, "Avatar mis à jour"));
    }

    @PutMapping("/password")
    @Operation(summary = "US-006 — Changer le mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        profileService.changePassword(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié"));
    }

    @DeleteMapping
    @Operation(summary = "US-007 — Supprimer son compte")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        profileService.deleteAccount(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Compte supprimé définitivement"));
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "US-007 — Supprimer un compte (admin)")
    public ResponseEntity<ApiResponse<Void>> deleteAccountByAdmin(
            @PathVariable Long userId) {
        profileService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Compte supprimé par l'admin"));
    }
}
