INSERT INTO users (name, email, password, role,phone,notification_mode, is_active, is_blocked, created_at, updated_at)
VALUES (
    'System Admin',
    'williamndongmo899@gmail.com',
    '$2a$12$eGe7EIZWElhYbQJgDLtB5OvGdAI2L0Pqjryw559n.V7nHXEcsYZdK',
    'ADMIN',
    '+237000000000',
    'EMAIL',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
