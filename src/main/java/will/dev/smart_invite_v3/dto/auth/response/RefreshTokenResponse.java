package will.dev.smart_invite_v3.dto.auth.response;

public record RefreshTokenResponse(

        String accessToken,

        /**
         * Nouveau refresh token (rotation à chaque appel).
         * Le client doit remplacer son ancien refresh token par celui-ci.
         */
        String refreshToken,

        long expiresIn

) {
}
