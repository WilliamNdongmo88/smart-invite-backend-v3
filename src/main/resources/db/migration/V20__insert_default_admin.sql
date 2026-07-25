INSERT INTO users (name, email, password, role, is_active, is_blocked, created_at, updated_at)
VALUES (
    'Admin',
    'admin@gmail.com',
    '$2a$10$THaIViY1L7jqQMyR7aJonelWvqk4nJHShrwah51D9g.vHpkLf2Sjq',
    'ADMIN',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
