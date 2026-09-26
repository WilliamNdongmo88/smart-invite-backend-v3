-- V29 : Correction des valeurs invalides dans la colonne status de la table invitations.
--
-- La table invitations n'accepte que : ACTIVE | REVOKED | USED
-- Des données de test (mock 1000 invités) ont pu insérer des valeurs
-- issues de RsvpStatus (PENDING, CONFIRMED, DECLINED) dans cette colonne.
-- Ce script les normalise vers ACTIVE (statut par défaut).

UPDATE invitations
SET status = 'ACTIVE'
WHERE status NOT IN ('ACTIVE', 'REVOKED', 'USED');
