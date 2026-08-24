-- ══════════════════════════════════════════════════════════════════
-- V22 — Mise à jour des types d'événement et ajout des champs
--       spécifiques au WeddingDetailsComponent
-- ══════════════════════════════════════════════════════════════════

-- ── 1. Renommer les anciens types vers les nouveaux ────────────────
-- Tous les événements FIANCAILLES → MARIAGE (même logique éditoriale)
UPDATE events SET type = 'MARIAGE'   WHERE type = 'FIANCAILLES';
-- ANNIVERSAIRE_MARIAGE et ANNIVERSAIRE → CEREMONIE
UPDATE events SET type = 'CEREMONIE' WHERE type IN ('ANNIVERSAIRE_MARIAGE', 'ANNIVERSAIRE');
-- EVENEMENT_PROFESSIONNEL → CONFERENCE
UPDATE events SET type = 'CONFERENCE' WHERE type = 'EVENEMENT_PROFESSIONNEL';

-- ── 2. Champs visuels WeddingDetails stockés en base ──────────────
-- Le WeddingDetailsComponent persiste certaines métadonnées clés
-- en base pour qu'elles soient accessibles à l'API (recherche, stats).
-- Le contenu visuel complet (biographies, galerie, etc.) reste en
-- localStorage côté client jusqu'à l'implémentation du stockage Cloud.

ALTER TABLE events
    -- Clé localStorage associée à la page mariage (si_wedding_{id})
    ADD COLUMN IF NOT EXISTS wedding_storage_key VARCHAR(100) NULL,
    -- Prénom de la mariée (dénormalisé depuis concernedNames pour accès rapide)
    ADD COLUMN IF NOT EXISTS bride_first_name    VARCHAR(100) NULL,
    -- Prénom du marié
    ADD COLUMN IF NOT EXISTS groom_first_name    VARCHAR(100) NULL,
    -- URL de la page WeddingDetails publique (slug futur)
    ADD COLUMN IF NOT EXISTS wedding_page_slug   VARCHAR(150) NULL;

-- ── 3. Index pour la recherche par slug ───────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_events_wedding_slug
    ON events(wedding_page_slug)
    WHERE wedding_page_slug IS NOT NULL;

-- ── 4. Commentaires de documentation ──────────────────────────────
COMMENT ON COLUMN events.wedding_storage_key IS
    'Clé localStorage (si_wedding_{id}) utilisée par WeddingDetailsComponent';
COMMENT ON COLUMN events.bride_first_name IS
    'Prénom de la mariée, extrait de concerned_names pour les mariages';
COMMENT ON COLUMN events.groom_first_name IS
    'Prénom du marié, extrait de concerned_names pour les mariages';
COMMENT ON COLUMN events.wedding_page_slug IS
    'Slug URL futur de la page publique du mariage';
