package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import will.dev.smart_invite_v3.dto.auth.request.*;
import will.dev.smart_invite_v3.dto.auth.response.*;
import will.dev.smart_invite_v3.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, tokens et reset password")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Inscription",
            description = "Crée un compte utilisateur et envoie un code OTP par email (valable 10 min)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Inscription réussie"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email déjà utilisé", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.message()));
    }

    @Operation(
            summary = "Vérification email",
            description = "Valide le code OTP reçu par email pour activer le compte"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email vérifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Code OTP invalide ou expiré", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email vérifié avec succès"));
    }

    @Operation(
            summary = "Connexion",
            description = "Authentifie l'utilisateur et retourne un Access Token (15 min) + Refresh Token (7 jours)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Compte non activé ou bloqué", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Connexion réussie"));
    }

    @Operation(
            summary = "Renouvellement du token",
            description = "Génère un nouvel Access Token à partir d'un Refresh Token valide"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token renouvelé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request), "Token renouvelé"));
    }

    @Operation(
            summary = "Déconnexion",
            description = "Invalide le Refresh Token dans Redis",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token manquant", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie"));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.googleLogin(request), "OK"));
    }

    @Operation(
            summary = "Mot de passe oublié",
            description = "Envoie un lien de réinitialisation par email (valable 10 min)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email envoyé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email introuvable", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Lien de réinitialisation envoyé"));
    }

    @Operation(
            summary = "Réinitialisation du mot de passe",
            description = "Définit un nouveau mot de passe via le token reçu par email"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mot de passe modifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token invalide ou expiré", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès"));
    }
}
