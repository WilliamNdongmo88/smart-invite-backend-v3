package will.dev.smart_invite_v3.dto.analytics;

import java.time.LocalDateTime;

/**
 * Ligne du tableau "Visiteurs du Site" dans l'admin.
 *
 * Chaque ligne représente un visiteur unique avec ses statistiques agrégées.
 */
public record VisitorRow(

        /** Identifiant interne */
        Long   id,

        /** Adresse IP (peut être masquée en RGPD) */
        String ipAddress,

        /** Pays détecté */
        String country,

        /** Ville détectée */
        String city,

        /** Type d'appareil : Desktop / Mobile / Tablet */
        String device,

        /** Système d'exploitation */
        String os,

        /** Navigateur */
        String browser,

        /** Date et heure de la première visite */
        LocalDateTime firstVisit,

        /** Nombre total de pages vues sur toutes les sessions */
        long totalPageViews,

        /** Nombre total de sessions */
        long totalSessions,

        /** Durée de la dernière session (en secondes, null si session ouverte) */
        Integer lastDurationSeconds,

        /**
         * Type de visiteur :
         * "new"       = 1 seule session
         * "returning" = plusieurs sessions
         */
        String visitorType
) {}
