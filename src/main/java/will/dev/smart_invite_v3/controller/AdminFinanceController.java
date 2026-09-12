package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse;
import will.dev.smart_invite_v3.service.impl.FinanceService;

/**
 * Endpoints privés de statistiques financières — réservés ADMIN.
 */
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Finance", description = "Statistiques des revenus de l'application")
@SecurityRequirement(name = "bearerAuth")
public class AdminFinanceController {

    private final FinanceService financeService;

    /**
     * Retourne tous les KPIs et séries temporelles financières en un seul appel.
     */
    @GetMapping("/stats")
    @Operation(summary = "KPIs revenus + tendances par mois et par année")
    public ResponseEntity<ApiResponse<FinanceStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(
                financeService.getStats(),
                "Statistiques financières récupérées"
        ));
    }
}
