package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse.DailyCount;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse.LabelCount;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse.LabelValue;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse.PageViewCount;
import will.dev.smart_invite_v3.dto.analytics.VisitorOverviewResponse;
import will.dev.smart_invite_v3.dto.analytics.VisitorRow;
import will.dev.smart_invite_v3.service.impl.AnalyticsService.VisitorStatsKpi;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrat du service d'agrégation des statistiques analytiques (audit BE-F1).
 */
public interface AnalyticsServicePort {

    AnalyticsStatsResponse getStats(int topPagesLimit);

    List<DailyCount> uniqueVisitorsByDay(int days);
    List<DailyCount> uniqueVisitorsByWeek(int weeks);
    List<DailyCount> uniqueVisitorsByMonth(int months);

    List<PageViewCount> topPages(LocalDateTime from, LocalDateTime to, int limit);
    List<LabelCount>    byCountry();
    List<LabelCount>    byDevice();
    List<LabelValue>    avgDurationByOs();
    List<LabelValue>    avgDurationByBrowser();
    double              bounceRate(LocalDateTime from, LocalDateTime to);

    VisitorOverviewResponse getOverview();

    List<VisitorRow> getVisitors(
            String search, String country, String city,
            String device, String browser,
            LocalDateTime dateFrom, LocalDateTime dateTo);

    VisitorStatsKpi getVisitorStatsKpi();
}
