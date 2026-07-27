package will.dev.smart_invite_v3.dto.auth.response;

public record RefreshTokenResponse(

        String accessToken,

        long expiresIn

) {
}