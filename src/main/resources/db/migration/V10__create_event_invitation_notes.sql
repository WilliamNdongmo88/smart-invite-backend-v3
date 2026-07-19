CREATE TABLE event_invitation_notes (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT UNIQUE NOT NULL,

    title VARCHAR(255),

    main_message TEXT,

    title_color VARCHAR(20),

    top_band_color VARCHAR(20),

    bottom_band_color VARCHAR(20),

    text_color VARCHAR(20),

    pdf_url TEXT,

    has_invitation_model_card BOOLEAN DEFAULT FALSE,

    logo_url TEXT,

    heart_icon_url TEXT,

    code VARCHAR(100),

    CONSTRAINT fk_note_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);