CREATE TABLE visitor_sessions (

    id BIGSERIAL PRIMARY KEY,

    visitor_id BIGINT NOT NULL,

    started_at TIMESTAMP,

    ended_at TIMESTAMP,

    duration_seconds INTEGER,

    CONSTRAINT fk_session_visitor
        FOREIGN KEY(visitor_id)
        REFERENCES visitors(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_sessions_visitor ON visitor_sessions(visitor_id);