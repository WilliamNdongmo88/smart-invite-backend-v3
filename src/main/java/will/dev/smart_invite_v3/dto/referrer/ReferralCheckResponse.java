package will.dev.smart_invite_v3.dto.referrer;

public record ReferralCheckResponse(
        boolean valid,
        String referrerName
) {}