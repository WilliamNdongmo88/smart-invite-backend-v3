CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    role VARCHAR(50) NOT NULL,

    phone VARCHAR(30),

    notification_mode VARCHAR(30),

    attendance_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    thank_notifications      BOOLEAN NOT NULL DEFAULT TRUE,
    event_reminders          BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_emails         BOOLEAN NOT NULL DEFAULT FALSE,
    notify_me                BOOLEAN NOT NULL DEFAULT FALSE,

    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,

    refresh_token TEXT,

    reset_code VARCHAR(20),
    reset_code_expires TIMESTAMP,

    avatar_url TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);
