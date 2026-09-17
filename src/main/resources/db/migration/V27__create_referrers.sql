-- Table des recommandateurs
CREATE TABLE referrers (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    phone             VARCHAR(30),
    email             VARCHAR(150),
    code              VARCHAR(40) NOT NULL UNIQUE,
    notification_mode VARCHAR(30),
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_referrers_code ON referrers(code);

-- Codes de recommandation liés aux comptes, événements et paiements
ALTER TABLE users    ADD COLUMN referral_code VARCHAR(40);
ALTER TABLE events   ADD COLUMN referral_code VARCHAR(40);
ALTER TABLE payments ADD COLUMN referral_code VARCHAR(40);

CREATE INDEX idx_users_referral_code    ON users(referral_code);
CREATE INDEX idx_events_referral_code   ON events(referral_code);
CREATE INDEX idx_payments_referral_code ON payments(referral_code);