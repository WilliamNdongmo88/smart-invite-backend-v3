-- V30 : Corrections de schéma issues de l'audit (BE-BD7, DB integrity)
-- 1. Fix max_guests NOT NULL avec valeur par défaut si null
UPDATE events SET max_guests = 100 WHERE max_guests IS NULL;
ALTER TABLE events ALTER COLUMN max_guests SET DEFAULT 100;
ALTER TABLE events ALTER COLUMN max_guests SET NOT NULL;

-- 2. Clé étrangère invitations.event_id -> events(id) ON DELETE CASCADE
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invitation_event'
    ) THEN
        ALTER TABLE invitations
            ADD CONSTRAINT fk_invitation_event
            FOREIGN KEY (event_id)
            REFERENCES events(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- 3. ON DELETE CASCADE sur payments(event_id) pour débloquer la suppression d'événement
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_event'
    ) THEN
        ALTER TABLE payments DROP CONSTRAINT fk_payment_event;
    END IF;

    ALTER TABLE payments
        ADD CONSTRAINT fk_payment_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE;
END $$;

-- 4. Index sur invitations(is_invitation_sent)
CREATE INDEX IF NOT EXISTS idx_invitations_sent ON invitations(is_invitation_sent);
