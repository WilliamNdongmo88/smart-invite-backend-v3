CREATE TABLE checkins (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,

    guest_id BIGINT NOT NULL,

    invitation_id BIGINT NOT NULL,

    scanned_by BIGINT,

    scan_status VARCHAR(30),

    checkin_time TIMESTAMP,

    CONSTRAINT fk_checkin_event
        FOREIGN KEY(event_id)
        REFERENCES events(id),

    CONSTRAINT fk_checkin_guest
        FOREIGN KEY(guest_id)
        REFERENCES guests(id),

    CONSTRAINT fk_checkin_invitation
        FOREIGN KEY(invitation_id)
        REFERENCES invitations(id),

    CONSTRAINT fk_checkin_user
        FOREIGN KEY(scanned_by)
        REFERENCES users(id)
);

CREATE INDEX idx_checkins_event ON checkins(event_id);