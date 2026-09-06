CREATE TABLE payments (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,

    organizer_id BIGINT NOT NULL,

    quota INTEGER,

    paid_quota INTEGER,

    sent_invitations INTEGER,

    amount NUMERIC(12,2),

    status VARCHAR(30),

    proof_url TEXT,

    proof_code VARCHAR(100),

    rejection_reason TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_event
        FOREIGN KEY(event_id)
        REFERENCES events(id),

    CONSTRAINT fk_payment_user
        FOREIGN KEY(organizer_id)
        REFERENCES users(id)
);


CREATE INDEX idx_payments_event ON payments(event_id);