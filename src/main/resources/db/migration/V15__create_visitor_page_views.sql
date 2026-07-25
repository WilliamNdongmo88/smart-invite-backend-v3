CREATE TABLE visitor_page_views (

    id BIGSERIAL PRIMARY KEY,

    session_id BIGINT NOT NULL,

    page_url TEXT,

    viewed_at TIMESTAMP,

    CONSTRAINT fk_pageview_session
        FOREIGN KEY(session_id)
        REFERENCES visitor_sessions(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_pageviews_session ON visitor_page_views(session_id);