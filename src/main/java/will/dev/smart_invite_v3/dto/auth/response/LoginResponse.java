package will.dev.smart_invite_v3.dto.auth.response;

public record LoginResponse(

        String accessToken,

        String refreshToken,

        long expiresIn

) {
}