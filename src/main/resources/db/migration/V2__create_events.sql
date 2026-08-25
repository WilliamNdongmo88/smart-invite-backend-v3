CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) DEFAULT 'PLANNED',
    budget VARCHAR(100),
    max_guests INTEGER,
    concerned_names VARCHAR(150),
    event_date TIMESTAMP,
    date_label VARCHAR(150),
    venue_name VARCHAR(255),
    venue_city VARCHAR(100),
    religious_location TEXT,
    religious_time TIMESTAMP,
    civil_location TEXT,
    civil_time TIMESTAMP,
    banquet_location TEXT,
    banquet_time TIMESTAMP,
    couple_photo_url TEXT,
    show_wedding_religious_location BOOLEAN DEFAULT FALSE,
    is_model_card BOOLEAN DEFAULT FALSE,
    wedding_storage_key VARCHAR(100),
    bride_first_name VARCHAR(100),
    groom_first_name VARCHAR(100),
    wedding_page_slug VARCHAR(150),
    wedding_details_content JSONB NULL,
    conference_details_content JSONB NULL,
    gala_details_content JSONB NULL,
    ceremonie_details_content JSONB NULL,
    thank_you_template JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_user
        FOREIGN KEY (organizer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_type ON events(type);
