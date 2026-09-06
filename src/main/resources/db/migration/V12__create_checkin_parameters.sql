CREATE TABLE checkin_parameters (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT UNIQUE NOT NULL,

    automatic_capture BOOLEAN DEFAULT TRUE,

    confirmation_sound BOOLEAN DEFAULT TRUE,

    total_scans INTEGER DEFAULT 0,

    valid_scans INTEGER DEFAULT 0,

    invalid_scans INTEGER DEFAULT 0,

    duplicate_scans INTEGER DEFAULT 0,

    CONSTRAINT fk_parameter_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);