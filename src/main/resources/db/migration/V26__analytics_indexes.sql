-- ============================================================
-- V26 : Index supplémentaires pour le tracking analytique
-- ============================================================
-- Ces index accélèrent les requêtes d'agrégation du tableau
-- de bord admin (filtres par date, groupements par URL, pays, etc.)
-- ============================================================

-- visitors : recherche par IP + device (identifyVisitor)
CREATE INDEX IF NOT EXISTS idx_visitors_ip_device
    ON visitors (ip_address, device);

-- visitor_sessions : filtrage par date de démarrage
CREATE INDEX IF NOT EXISTS idx_sessions_started_at
    ON visitor_sessions (started_at);

-- visitor_sessions : fermeture des sessions inactives
CREATE INDEX IF NOT EXISTS idx_sessions_ended_at_null
    ON visitor_sessions (ended_at)
    WHERE ended_at IS NULL;

-- visitor_page_views : filtrage par date de vue
CREATE INDEX IF NOT EXISTS idx_pageviews_viewed_at
    ON visitor_page_views (viewed_at);

-- visitor_page_views : groupement par URL (top pages)
CREATE INDEX IF NOT EXISTS idx_pageviews_page_url
    ON visitor_page_views (page_url);
