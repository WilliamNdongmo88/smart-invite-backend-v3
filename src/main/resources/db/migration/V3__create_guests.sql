CREATE TABLE guests (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,

    full_name VARCHAR(200) NOT NULL,

    email VARCHAR(150),

    phone_number VARCHAR(30),

    rsvp_status VARCHAR(30),

    table_number INTEGER,

    notification_mode VARCHAR(30),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_guest_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_guests_event ON guests(event_id);