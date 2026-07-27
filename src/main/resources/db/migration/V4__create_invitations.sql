CREATE TABLE invitations (

    id BIGSERIAL PRIMARY KEY,

    guest_id BIGINT NOT NULL UNIQUE,

    token VARCHAR(255) NOT NULL UNIQUE,

    qr_code_url TEXT,

    status VARCHAR(30),

    chat_id VARCHAR(150),

    is_invitation_sent BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invitation_guest
        FOREIGN KEY(guest_id)
        REFERENCES guests(id)
        ON DELETE CASCADE
);
