package will.dev.smart_invite_v3.dto.analytics;

import java.util.List;

/**
 * DTO retourné par GET /api/admin/analytics/overview.
 * Alimente les 6 stats cards de la page /admin/visitors.
 */
public record VisitorOverviewResponse(

        /** Top pays par nombre de visiteurs uniques */
        List<LabelCount> byCountry,

        /** Répartition des navigateurs */
        List<LabelCount> byBrowser,

        /** Répartition des systèmes d'exploitation */
        List<LabelCount> byOs,

        /** Répartition des types d'appareils (Desktop / Mobile / Tablet) */
        List<LabelCount> byDevice,

        /** Top 10 pages les plus vues (30 derniers jours) */
        List<PageViewCount> topPages,

        /** Visites uniques par jour sur les 30 derniers jours */
        List<DailyCount> visitsByDay

) {
    public record LabelCount(String label, long count) {}
    public record PageViewCount(String url, long views) {}
    public record DailyCount(String period, long count) {}
}
