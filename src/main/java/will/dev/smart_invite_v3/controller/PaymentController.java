package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.payment.request.InitPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.request.ReviewPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.response.PaymentPlanResponse;
import will.dev.smart_invite_v3.dto.payment.response.PaymentResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.PaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestion des paiements et quotas d'invités")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/plans")
    @Operation(summary = "Calculer le montant pour un quota donné (quota × 52 XAF)")
    public ResponseEntity<ApiResponse<PaymentPlanResponse>> getPlan(
            @RequestParam @Min(1) int quota
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.calculatePlan(quota),
                "Calcul effectué"));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Initialiser un paiement pour un quota d'invités")
    public ResponseEntity<ApiResponse<PaymentResponse>> subscribe(
            @Valid @RequestBody InitPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                paymentService.initPayment(request, userDetails.getUser().getId()),
                "Paiement initialisé avec statut PENDING"));
    }

    @PostMapping(value = "/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Soumettre une preuve de paiement (PDF/PNG/JPG)")
    public ResponseEntity<ApiResponse<PaymentResponse>> submitProof(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.submitProof(id, file, userDetails.getUser().getId()),
                "Preuve soumise — statut passé à UNDER_REVIEW"));
    }

    @GetMapping("/history")
    @Operation(summary = "Historique des paiements de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getHistory(userDetails.getUser().getId()),
                "Historique récupéré"));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Tous les paiements")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getAllPayments(), "Paiements récupérés"));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Valider ou rejeter une preuve de paiement")
    public ResponseEntity<ApiResponse<PaymentResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewPaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.reviewPayment(id, request),
                request.approved() ? "Paiement approuvé" : "Paiement rejeté"));
    }
}
