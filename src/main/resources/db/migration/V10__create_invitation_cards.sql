CREATE TABLE invitation_cards (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT UNIQUE NOT NULL,

    title VARCHAR(255),

    main_message TEXT,

    main_message_part1 TEXT,

    main_message_part2 TEXT,

    sous_main_message TEXT,

    event_theme VARCHAR(200),

    priority_colors VARCHAR(300),

    qr_instructions TEXT,

    dress_code_message TEXT,

    thanks_message1 TEXT,

    closing_message TEXT,

    title_color VARCHAR(20),

    top_band_color VARCHAR(20),

    bottom_band_color VARCHAR(20),

    text_color VARCHAR(20),

    logo_url TEXT,

    heart_icon_url TEXT,

    pdf_url TEXT,

    has_invitation_model_card BOOLEAN DEFAULT FALSE,

    code VARCHAR(100),

    CONSTRAINT fk_invitation_card_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);
