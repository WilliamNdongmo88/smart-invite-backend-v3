CREATE TABLE links (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,

    token VARCHAR(255) UNIQUE NOT NULL,

    used_count INTEGER DEFAULT 0,

    limit_count INTEGER,

    date_limit_link TIMESTAMP,

    CONSTRAINT fk_link_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_links_event ON links(event_id);