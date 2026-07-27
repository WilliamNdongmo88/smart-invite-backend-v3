CREATE TABLE whatsapp_tracking (

    id BIGSERIAL PRIMARY KEY,

    whatsapp_message_id VARCHAR(255),

    event_id BIGINT,

    guest_id BIGINT,

    invitation_id BIGINT,

    chat_id VARCHAR(150),

    CONSTRAINT fk_whatsapp_event
        FOREIGN KEY(event_id)
        REFERENCES events(id),

    CONSTRAINT fk_whatsapp_guest
        FOREIGN KEY(guest_id)
        REFERENCES guests(id),

    CONSTRAINT fk_whatsapp_invitation
        FOREIGN KEY(invitation_id)
        REFERENCES invitations(id)
);

CREATE INDEX idx_whatsapp_guest ON whatsapp_tracking(guest_id);

CREATE INDEX idx_whatsapp_invitation ON whatsapp_tracking(invitation_id);