package will.dev.smart_invite_v3.dto.analytics;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête POST /api/track/pageview
 */
public record TrackPageViewRequest(

        /** URL de la page visitée */
        @NotBlank String pageUrl,

        /**
         * Session ID existante (null si première page vue).
         * Si null, une nouvelle session est créée automatiquement.
         */
        Long sessionId
) {}
