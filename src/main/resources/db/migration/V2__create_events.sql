CREATE TABLE events (

    id BIGSERIAL PRIMARY KEY,

    organizer_id BIGINT NOT NULL,

    title VARCHAR(255) NOT NULL,

    type VARCHAR(50),

    status VARCHAR(30),

    max_guests INTEGER,

    religious_location TEXT,
    religious_time TIMESTAMP,

    civil_location TEXT,
    civil_time TIMESTAMP,

    banquet_location TEXT,
    banquet_time TIMESTAMP,

    concerned_names VARCHAR(150),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_user
        FOREIGN KEY (organizer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_events_organizer ON events(organizer_id);