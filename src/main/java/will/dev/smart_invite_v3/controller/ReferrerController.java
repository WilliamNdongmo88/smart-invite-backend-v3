package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.referrer.ReferralCheckResponse;
import will.dev.smart_invite_v3.dto.referrer.ReferrerRequest;
import will.dev.smart_invite_v3.dto.referrer.ReferrerResponse;
import will.dev.smart_invite_v3.service.ReferrerService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Recommandateurs", description = "Programme de recommandation Smart Invite")
public class ReferrerController {

    private final ReferrerService referrerService;

    @PostMapping("/admin/referrers")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Créer un recommandateur et générer son code")
    public ResponseEntity<ApiResponse<ReferrerResponse>> create(
            @Valid @RequestBody ReferrerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                referrerService.create(request),
                "Recommandateur créé — code généré"));
    }

    @GetMapping("/admin/referrers")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Lister les recommandateurs avec leurs statistiques")
    public ResponseEntity<ApiResponse<List<ReferrerResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                referrerService.findAll(),
                "Recommandateurs récupérés"));
    }

    @PatchMapping("/admin/referrers/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[ADMIN] Activer / désactiver un code de recommandation")
    public ResponseEntity<ApiResponse<ReferrerResponse>> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                referrerService.toggleActive(id),
                "Statut mis à jour"));
    }

    @GetMapping("/referrers/validate")
    @Operation(summary = "Vérifier un code de recommandation (public)",
            description = "Retourne valid=true si le code existe et est actif")
    public ResponseEntity<ApiResponse<ReferralCheckResponse>> validate(
            @RequestParam String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                referrerService.check(code),
                "Vérification effectuée"));
    }
}