-- V28 : Ajout du champ is_read sur la table usernews
-- Permet à l'admin de marquer un message de contact comme lu
-- et de décrémenter le compteur de non-lus dans l'interface.

ALTER TABLE usernews
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;
