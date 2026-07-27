CREATE TABLE notifications (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,

    title VARCHAR(255),

    message TEXT,

    type VARCHAR(50),

    is_read BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notifications_event ON notifications(event_id);