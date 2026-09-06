-- ══════════════════════════════════════════════════════════════════
-- V23 — Ajout de la colonne JSONB pour stocker le contenu complet
--       de la page WeddingDetails (10 sections éditables)
-- ══════════════════════════════════════════════════════════════════

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS wedding_details_content JSONB NULL;

COMMENT ON COLUMN events.wedding_details_content IS
    'Contenu JSON complet de la page WeddingDetails : hero, couple, story, '
    'program, dressCode, faq, rsvp, gallery, backgrounds, footer. '
    'Sérialisé depuis WeddingDetailsContent (TypeScript/Java).';

-- Index GIN optionnel pour la recherche dans le JSON (si besoin futur)
CREATE INDEX IF NOT EXISTS idx_events_wedding_content_gin
    ON events USING GIN (wedding_details_content)
    WHERE wedding_details_content IS NOT NULL;
