-- V25 : ajout des champs de contact sur la table usernews
-- Permet de stocker les messages envoyés depuis le formulaire de contact public
-- et de notifier l'admin via WhatsApp ou Email selon le choix de l'utilisateur.

ALTER TABLE usernews
    ADD COLUMN IF NOT EXISTS message       TEXT,
    ADD COLUMN IF NOT EXISTS reply_channel VARCHAR(20),    -- 'WHATSAPP' | 'EMAIL'
    ADD COLUMN IF NOT EXISTS reply_contact VARCHAR(200),   -- numéro WA ou adresse email de réponse
    ADD COLUMN IF NOT EXISTS user_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW();
