package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse;
import will.dev.smart_invite_v3.dto.analytics.VisitorRow;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.service.impl.AnalyticsService;
import will.dev.smart_invite_v3.service.impl.AnalyticsService.VisitorStatsKpi;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints privés de statistiques analytiques — réservés ADMIN.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Analytics", description = "Statistiques de tracking visiteurs")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Retourne toutes les statistiques agrégées en un seul appel.
     */
    @GetMapping("/stats")
    @Operation(summary = "Statistiques complètes des visiteurs")
    public ResponseEntity<ApiResponse<AnalyticsStatsResponse>> getStats(
            @RequestParam(defaultValue = "10") int topPages) {

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getStats(topPages),
                "Statistiques récupérées"
        ));
    }

    /**
     * Liste des visiteurs avec filtres — alimente le tableau de la page /admin/visitors.
     */
    @GetMapping("/visitors")
    @Operation(summary = "Liste des visiteurs avec filtres")
    public ResponseEntity<ApiResponse<List<VisitorRow>>> getVisitors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String browser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        List<VisitorRow> rows = analyticsService.getVisitors(
                search, country, city, device, browser, dateFrom, dateTo
        );
        return ResponseEntity.ok(ApiResponse.success(rows, "Visiteurs récupérés"));
    }

    /**
     * KPIs rapides pour l'en-tête de la page Visiteurs.
     */
    @GetMapping("/visitors/kpi")
    @Operation(summary = "KPIs visiteurs (uniques, récurrents, pages vues, durée moyenne)")
    public ResponseEntity<ApiResponse<VisitorStatsKpi>> getVisitorKpi() {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getVisitorStatsKpi(),
                "KPIs récupérés"
        ));
    }
}
