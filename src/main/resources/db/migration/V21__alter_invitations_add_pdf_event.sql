ALTER TABLE invitations
    ADD COLUMN IF NOT EXISTS pdf_url TEXT,
    ADD COLUMN IF NOT EXISTS event_id BIGINT;

UPDATE invitations i
SET event_id = g.event_id
FROM guests g
WHERE i.guest_id = g.id;

CREATE INDEX IF NOT EXISTS idx_invitations_event ON invitations(event_id);
