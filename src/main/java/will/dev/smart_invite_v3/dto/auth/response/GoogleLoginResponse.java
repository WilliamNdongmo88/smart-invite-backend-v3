package will.dev.smart_invite_v3.dto.auth.response;

public record GoogleLoginResponse(
        boolean needsRegistration,
        // Présents uniquement si needsRegistration = false
        String accessToken,
        String refreshToken,
        Long expiresIn,
        // Présents uniquement si needsRegistration = true
        String email,
        String name,
        String avatarUrl
) {}
