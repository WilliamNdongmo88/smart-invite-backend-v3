package will.dev.smart_invite_v3.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse;
import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service de statistiques financières.
 *
 * Toutes les requêtes portent sur la table {@code payments}.
 * Seuls les paiements au statut {@code APPROVED} comptent comme revenus.
 */
@Service
@RequiredArgsConstructor
public class FinanceService {

    @PersistenceContext
    private EntityManager em;

    // ─────────────────────────────────────────────────────────────────────────
    //  Agrégat principal
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinanceStatsResponse getStats() {

        BigDecimal totalRevenue    = scalar("""
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE status = 'APPROVED'
                """);

        BigDecimal revenueThisMonth = scalar("""
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE status = 'APPROVED'
                  AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
                """);

        BigDecimal revenueLastMonth = scalar("""
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE status = 'APPROVED'
                  AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW() - INTERVAL '1 month')
                """);

        double growthPct = computeGrowth(revenueLastMonth, revenueThisMonth);

        long totalApproved = scalarLong("""
                SELECT COUNT(*) FROM payments WHERE status = 'APPROVED'
                """);

        long totalPending = scalarLong("""
                SELECT COUNT(*) FROM payments WHERE status IN ('PENDING', 'UNDER_REVIEW')
                """);

        BigDecimal pendingAmount = scalar("""
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE status IN ('PENDING', 'UNDER_REVIEW')
                """);

        BigDecimal avgOrderValue = totalApproved > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalApproved), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PeriodAmount> byMonth     = revenueByMonth(12);
        List<PeriodAmount> byYear      = revenueByYear();
        List<PeriodCount>  countByMonth = countApprovedByMonth(12);

        return new FinanceStatsResponse(
                totalRevenue,
                revenueThisMonth,
                revenueLastMonth,
                growthPct,
                totalApproved,
                totalPending,
                pendingAmount,
                avgOrderValue,
                byMonth,
                byYear,
                countByMonth
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Séries temporelles
    // ─────────────────────────────────────────────────────────────────────────

    /** Revenus APPROVED par mois sur les N derniers mois */
    @Transactional(readOnly = true)
    public List<PeriodAmount> revenueByMonth(int months) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS period,
                       COALESCE(SUM(amount), 0)                             AS total
                FROM payments
                WHERE status = 'APPROVED'
                  AND created_at >= NOW() - (INTERVAL '1 month' * :months)
                GROUP BY period
                ORDER BY period ASC
                """)
                .setParameter("months", months)
                .getResultList();

        return rows.stream()
                .map(r -> new PeriodAmount(str(r[0]), decimal(r[1])))
                .toList();
    }

    /** Revenus APPROVED par année (toutes années disponibles) */
    @Transactional(readOnly = true)
    public List<PeriodAmount> revenueByYear() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT TO_CHAR(DATE_TRUNC('year', created_at), 'YYYY') AS period,
                       COALESCE(SUM(amount), 0)                          AS total
                FROM payments
                WHERE status = 'APPROVED'
                GROUP BY period
                ORDER BY period ASC
                """)
                .getResultList();

        return rows.stream()
                .map(r -> new PeriodAmount(str(r[0]), decimal(r[1])))
                .toList();
    }

    /** Nombre de paiements APPROVED par mois sur les N derniers mois */
    @Transactional(readOnly = true)
    public List<PeriodCount> countApprovedByMonth(int months) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS period,
                       COUNT(*)                                              AS cnt
                FROM payments
                WHERE status = 'APPROVED'
                  AND created_at >= NOW() - (INTERVAL '1 month' * :months)
                GROUP BY period
                ORDER BY period ASC
                """)
                .setParameter("months", months)
                .getResultList();

        return rows.stream()
                .map(r -> new PeriodCount(str(r[0]), longVal(r[1])))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal scalar(String sql) {
        Object result = em.createNativeQuery(sql).getSingleResult();
        return decimal(result);
    }

    private long scalarLong(String sql) {
        Object result = em.createNativeQuery(sql).getSingleResult();
        return result instanceof Number n ? n.longValue() : 0L;
    }

    private double computeGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        double pct = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .doubleValue() * 100.0;
        return Math.round(pct * 10.0) / 10.0;
    }

    private BigDecimal decimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private String str(Object o)     { return o != null ? o.toString() : ""; }
    private long   longVal(Object o) { return o instanceof Number n ? n.longValue() : 0L; }
}
