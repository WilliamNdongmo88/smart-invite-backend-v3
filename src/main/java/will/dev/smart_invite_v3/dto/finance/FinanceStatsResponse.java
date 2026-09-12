package will.dev.smart_invite_v3.dto.finance;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO principal retourné par GET /api/admin/finance/stats.
 * Agrège tous les indicateurs financiers en un seul appel.
 */
public record FinanceStatsResponse(

        // ── KPIs globaux ──────────────────────────────────────────────────────

        /** Revenu total de tous les paiements APPROVED */
        BigDecimal totalRevenue,

        /** Revenu du mois en cours (APPROVED) */
        BigDecimal revenueThisMonth,

        /** Revenu du mois précédent (APPROVED) */
        BigDecimal revenueLastMonth,

        /** Tendance : différence en % entre ce mois et le précédent */
        double monthlyGrowthPct,

        /** Nombre total de paiements APPROVED */
        long totalApproved,

        /** Nombre de paiements en attente (PENDING + UNDER_REVIEW) */
        long totalPending,

        /** Montant total en attente de validation */
        BigDecimal pendingAmount,

        /** Panier moyen (totalRevenue / totalApproved) */
        BigDecimal avgOrderValue,

        // ── Séries temporelles ────────────────────────────────────────────────

        /** Revenus APPROVED agrégés par mois (12 derniers mois) */
        List<PeriodAmount> byMonth,

        /** Revenus APPROVED agrégés par année */
        List<PeriodAmount> byYear,

        /** Nombre de paiements APPROVED par mois (12 derniers mois) */
        List<PeriodCount> countByMonth

) {

    /** Montant agrégé pour une période (ex : "2026-08", "2026") */
    public record PeriodAmount(String period, BigDecimal amount) {}

    /** Nombre de paiements pour une période */
    public record PeriodCount(String period, long count) {}
}
