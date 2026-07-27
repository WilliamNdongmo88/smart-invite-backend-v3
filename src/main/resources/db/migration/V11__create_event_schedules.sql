CREATE TABLE event_schedules (

    id BIGSERIAL PRIMARY KEY,

    event_id BIGINT UNIQUE NOT NULL,

    scheduled_for TIMESTAMP,

    executed BOOLEAN DEFAULT FALSE,

    is_checkin_executed BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_schedule_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);