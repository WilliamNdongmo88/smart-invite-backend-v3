-- ══════════════════════════════════════════════════════════════════
-- V24 — Ajout des colonnes JSONB pour Conference, Gala et Cérémonie
--       et des métadonnées de lieux / dates label
-- ══════════════════════════════════════════════════════════════════

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS date_label VARCHAR(150) NULL,
    ADD COLUMN IF NOT EXISTS venue_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS venue_city VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS conference_details_content JSONB NULL,
    ADD COLUMN IF NOT EXISTS gala_details_content JSONB NULL,
    ADD COLUMN IF NOT EXISTS ceremonie_details_content JSONB NULL;

COMMENT ON COLUMN events.conference_details_content IS
    'Contenu JSON complet de la page ConferenceDetails : hero, about, agenda, speakers, sponsors, faq, rsvp, gallery, backgrounds, footer.';

COMMENT ON COLUMN events.gala_details_content IS
    'Contenu JSON complet de la page GalaDetails : hero, about, program, performers, dressCode, faq, rsvp, gallery, backgrounds, footer.';

COMMENT ON COLUMN events.ceremonie_details_content IS
    'Contenu JSON complet de la page CeremonieDetails : hero, about, program, keyGuests, dressCode, faq, rsvp, gallery, backgrounds, footer.';
