package will.dev.smart_invite_v3.dto.analytics;

import java.util.List;

/**
 * DTO principal retourné par GET /api/admin/analytics/stats
 * Agrège toutes les métriques en un seul appel.
 */
public record AnalyticsStatsResponse(

        /** Visiteurs uniques par période */
        List<DailyCount>    uniqueVisitorsByDay,
        List<DailyCount>    uniqueVisitorsByWeek,
        List<DailyCount>    uniqueVisitorsByMonth,

        /** Top 10 pages les plus vues */
        List<PageViewCount> topPages,

        /** Répartition par pays */
        List<LabelCount>    byCountry,

        /** Répartition par device */
        List<LabelCount>    byDevice,

        /** Durée moyenne de session par OS */
        List<LabelValue>    avgDurationByOs,

        /** Durée moyenne de session par browser */
        List<LabelValue>    avgDurationByBrowser,

        /** Taux de rebond global (%) */
        double              bounceRate,

        /** Résumé rapide */
        long                totalVisitors,
        long                totalSessions,
        long                totalPageViews

) {
    public record DailyCount(String period, long count) {}
    public record PageViewCount(String url, long views) {}
    public record LabelCount(String label, long count) {}
    public record LabelValue(String label, double avgSeconds) {}
}
