package will.dev.smart_invite_v3.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse;
import will.dev.smart_invite_v3.dto.analytics.AnalyticsStatsResponse.*;
import will.dev.smart_invite_v3.dto.analytics.VisitorRow;
import will.dev.smart_invite_v3.repository.VisitorPageViewRepository;
import will.dev.smart_invite_v3.repository.VisitorRepository;
import will.dev.smart_invite_v3.repository.VisitorSessionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'agrégation des statistiques analytiques.
 *
 * Toutes les requêtes SQL natives sont regroupées ici pour faciliter
 * les tests et la maintenance.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    @PersistenceContext
    private EntityManager em;

    private final VisitorRepository         visitorRepo;
    private final VisitorSessionRepository  sessionRepo;
    private final VisitorPageViewRepository pageViewRepo;

    // ─────────────────────────────────────────────────────────────────
    //  Agrégat principal
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalyticsStatsResponse getStats(int topPagesLimit) {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime day30 = now.minusDays(30);
        LocalDateTime day7  = now.minusDays(7);

        return new AnalyticsStatsResponse(
                uniqueVisitorsByDay(30),
                uniqueVisitorsByWeek(12),
                uniqueVisitorsByMonth(12),
                topPages(day30, now, topPagesLimit),
                byCountry(),
                byDevice(),
                avgDurationByOs(),
                avgDurationByBrowser(),
                bounceRate(day30, now),
                visitorRepo.count(),
                sessionRepo.count(),
                pageViewRepo.count()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    //  Visiteurs uniques par période
    // ─────────────────────────────────────────────────────────────────

    /** Nombre de visiteurs uniques par jour sur les N derniers jours */
    @Transactional(readOnly = true)
    public List<DailyCount> uniqueVisitorsByDay(int days) {
        String sql = """
                SELECT TO_CHAR(s.started_at, 'YYYY-MM-DD') AS period,
                       COUNT(DISTINCT s.visitor_id)          AS cnt
                FROM visitor_sessions s
                WHERE s.started_at >= NOW() - INTERVAL ':days days'
                GROUP BY period
                ORDER BY period ASC
                """.replace(":days", String.valueOf(days));

        return toList(em.createNativeQuery(sql).getResultList());
    }

    /** Nombre de visiteurs uniques par semaine sur les N dernières semaines */
    @Transactional(readOnly = true)
    public List<DailyCount> uniqueVisitorsByWeek(int weeks) {
        String sql = """
                SELECT TO_CHAR(DATE_TRUNC('week', s.started_at), 'YYYY-"W"IW') AS period,
                       COUNT(DISTINCT s.visitor_id)                              AS cnt
                FROM visitor_sessions s
                WHERE s.started_at >= NOW() - INTERVAL ':weeks weeks'
                GROUP BY period
                ORDER BY period ASC
                """.replace(":weeks", String.valueOf(weeks));

        return toList(em.createNativeQuery(sql).getResultList());
    }

    /** Nombre de visiteurs uniques par mois sur les N derniers mois */
    @Transactional(readOnly = true)
    public List<DailyCount> uniqueVisitorsByMonth(int months) {
        String sql = """
                SELECT TO_CHAR(DATE_TRUNC('month', s.started_at), 'YYYY-MM') AS period,
                       COUNT(DISTINCT s.visitor_id)                           AS cnt
                FROM visitor_sessions s
                WHERE s.started_at >= NOW() - INTERVAL ':months months'
                GROUP BY period
                ORDER BY period ASC
                """.replace(":months", String.valueOf(months));

        return toList(em.createNativeQuery(sql).getResultList());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Top pages
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PageViewCount> topPages(LocalDateTime from, LocalDateTime to, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT pv.page_url,
                       COUNT(*) AS views
                FROM visitor_page_views pv
                WHERE pv.viewed_at BETWEEN :from AND :to
                GROUP BY pv.page_url
                ORDER BY views DESC
                LIMIT :limit
                """)
                .setParameter("from",  from)
                .setParameter("to",    to)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(r -> new PageViewCount(str(r[0]), longVal(r[1])))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Répartitions
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LabelCount> byCountry() {
        return labelCount("""
                SELECT COALESCE(v.country, 'Unknown') AS label,
                       COUNT(DISTINCT v.id)            AS cnt
                FROM visitors v
                GROUP BY label
                ORDER BY cnt DESC
                LIMIT 20
                """);
    }

    @Transactional(readOnly = true)
    public List<LabelCount> byDevice() {
        return labelCount("""
                SELECT COALESCE(v.device, 'Unknown') AS label,
                       COUNT(DISTINCT v.id)           AS cnt
                FROM visitors v
                GROUP BY label
                ORDER BY cnt DESC
                """);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Durée moyenne par OS / Browser
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LabelValue> avgDurationByOs() {
        return labelValue("""
                SELECT COALESCE(v.os, 'Unknown')       AS label,
                       AVG(s.duration_seconds)          AS avg_sec
                FROM visitor_sessions s
                JOIN visitors v ON v.id = s.visitor_id
                WHERE s.duration_seconds IS NOT NULL
                GROUP BY label
                ORDER BY avg_sec DESC
                """);
    }

    @Transactional(readOnly = true)
    public List<LabelValue> avgDurationByBrowser() {
        return labelValue("""
                SELECT COALESCE(v.browser, 'Unknown')  AS label,
                       AVG(s.duration_seconds)          AS avg_sec
                FROM visitor_sessions s
                JOIN visitors v ON v.id = s.visitor_id
                WHERE s.duration_seconds IS NOT NULL
                GROUP BY label
                ORDER BY avg_sec DESC
                """);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Taux de rebond
    // ─────────────────────────────────────────────────────────────────

    /**
     * Taux de rebond = sessions avec exactement 1 page vue / total sessions
     * sur la période donnée.
     */
    @Transactional(readOnly = true)
    public double bounceRate(LocalDateTime from, LocalDateTime to) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    COUNT(*)                                                  AS total_sessions,
                    COUNT(*) FILTER (WHERE page_count = 1)                   AS bounced
                FROM (
                    SELECT s.id, COUNT(pv.id) AS page_count
                    FROM visitor_sessions s
                    LEFT JOIN visitor_page_views pv ON pv.session_id = s.id
                    WHERE s.started_at BETWEEN :from AND :to
                    GROUP BY s.id
                ) sub
                """)
                .setParameter("from", from)
                .setParameter("to",   to)
                .getResultList();

        if (rows.isEmpty()) return 0.0;
        Object[] r     = rows.get(0);
        long total     = longVal(r[0]);
        long bounced   = longVal(r[1]);
        if (total == 0) return 0.0;
        return Math.round((bounced * 100.0 / total) * 10.0) / 10.0;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Liste des visiteurs (tableau admin avec filtres)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retourne la liste des visiteurs avec leurs statistiques agrégées,
     * en appliquant les filtres optionnels.
     *
     * @param search   recherche libre sur IP, pays, ville, navigateur
     * @param country  filtre exact sur le pays
     * @param city     filtre exact sur la ville
     * @param device   filtre exact sur le device
     * @param browser  filtre exact sur le navigateur
     * @param dateFrom date de début (première visite)
     * @param dateTo   date de fin (première visite)
     */
    @Transactional(readOnly = true)
    public List<VisitorRow> getVisitors(
            String search,
            String country,
            String city,
            String device,
            String browser,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    v.id,
                    v.ip_address,
                    COALESCE(v.country, 'Inconnu')  AS country,
                    COALESCE(v.city, 'Inconnue')    AS city,
                    COALESCE(v.device, 'Unknown')   AS device,
                    COALESCE(v.os, 'Unknown')       AS os,
                    COALESCE(v.browser, 'Unknown')  AS browser,
                    MIN(s.started_at)               AS first_visit,
                    COUNT(DISTINCT pv.id)           AS total_page_views,
                    COUNT(DISTINCT s.id)            AS total_sessions,
                    (SELECT s2.duration_seconds
                     FROM visitor_sessions s2
                     WHERE s2.visitor_id = v.id
                       AND s2.duration_seconds IS NOT NULL
                     ORDER BY s2.started_at DESC
                     LIMIT 1)                       AS last_duration,
                    CASE WHEN COUNT(DISTINCT s.id) > 1 THEN 'returning' ELSE 'new' END AS visitor_type
                FROM visitors v
                LEFT JOIN visitor_sessions s      ON s.visitor_id = v.id
                LEFT JOIN visitor_page_views pv   ON pv.session_id = s.id
                WHERE 1=1
                """);

        if (isSet(search)) {
            sql.append(" AND (v.ip_address ILIKE :search OR v.country ILIKE :search OR v.city ILIKE :search OR v.browser ILIKE :search)");
        }
        if (isSet(country)) sql.append(" AND v.country = :country");
        if (isSet(city))    sql.append(" AND v.city = :city");
        if (isSet(device))  sql.append(" AND v.device = :device");
        if (isSet(browser)) sql.append(" AND v.browser = :browser");
        if (dateFrom != null) sql.append(" AND s.started_at >= :dateFrom");
        if (dateTo   != null) sql.append(" AND s.started_at <= :dateTo");

        sql.append(" GROUP BY v.id ORDER BY first_visit DESC NULLS LAST LIMIT 500");

        var query = em.createNativeQuery(sql.toString());

        if (isSet(search))  query.setParameter("search",   "%" + search + "%");
        if (isSet(country)) query.setParameter("country",  country);
        if (isSet(city))    query.setParameter("city",     city);
        if (isSet(device))  query.setParameter("device",   device);
        if (isSet(browser)) query.setParameter("browser",  browser);
        if (dateFrom != null) query.setParameter("dateFrom", dateFrom);
        if (dateTo   != null) query.setParameter("dateTo",   dateTo);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(r -> new VisitorRow(
                longVal(r[0]),
                str(r[1]),
                str(r[2]),
                str(r[3]),
                str(r[4]),
                str(r[5]),
                str(r[6]),
                r[7] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime() : null,
                longVal(r[8]),
                longVal(r[9]),
                r[10] instanceof Number n ? n.intValue() : null,
                str(r[11])
        )).toList();
    }

    /**
     * KPIs rapides pour l'en-tête de la page Visiteurs :
     * visiteurs uniques, récurrents, pages vues, durée moyenne.
     */
    @Transactional(readOnly = true)
    public VisitorStatsKpi getVisitorStatsKpi() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    COUNT(DISTINCT v.id)                                           AS total_visitors,
                    COUNT(DISTINCT v.id) FILTER (WHERE session_counts.cnt > 1)    AS returning_visitors,
                    COUNT(pv.id)                                                   AS total_page_views,
                    ROUND(AVG(s.duration_seconds))                                 AS avg_duration
                FROM visitors v
                LEFT JOIN visitor_sessions s    ON s.visitor_id = v.id
                LEFT JOIN visitor_page_views pv ON pv.session_id = s.id
                LEFT JOIN (
                    SELECT visitor_id, COUNT(*) AS cnt
                    FROM visitor_sessions
                    GROUP BY visitor_id
                ) session_counts ON session_counts.visitor_id = v.id
                """).getResultList();

        if (rows.isEmpty()) return new VisitorStatsKpi(0, 0, 0, 0);
        Object[] r = rows.get(0);
        return new VisitorStatsKpi(
                longVal(r[0]),
                longVal(r[1]),
                longVal(r[2]),
                r[3] instanceof Number n ? (int) Math.round(n.doubleValue()) : 0
        );
    }

    public record VisitorStatsKpi(
            long totalVisitors,
            long returningVisitors,
            long totalPageViews,
            int  avgDurationSeconds
    ) {}

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    private boolean isSet(String s) { return s != null && !s.isBlank(); }

    @SuppressWarnings("unchecked")
    private List<DailyCount> toList(List<?> rows) {
        return ((List<Object[]>) rows).stream()
                .map(r -> new DailyCount(str(r[0]), longVal(r[1])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<LabelCount> labelCount(String sql) {
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        return rows.stream()
                .map(r -> new LabelCount(str(r[0]), longVal(r[1])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<LabelValue> labelValue(String sql) {
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        return rows.stream()
                .map(r -> new LabelValue(str(r[0]), doubleVal(r[1])))
                .toList();
    }

    private String str(Object o)       { return o != null ? o.toString() : "Unknown"; }
    private long   longVal(Object o)   { return o instanceof Number n ? n.longValue() : 0L; }
    private double doubleVal(Object o) { return o instanceof Number n ? Math.round(n.doubleValue() * 10.0) / 10.0 : 0.0; }
}
