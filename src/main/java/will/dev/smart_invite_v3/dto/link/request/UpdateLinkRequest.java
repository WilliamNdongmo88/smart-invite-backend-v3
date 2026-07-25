package will.dev.smart_invite_v3.dto.link.request;

import java.time.LocalDateTime;

public record UpdateLinkRequest(
        Integer limitCount,
        LocalDateTime dateLimitLink
) {}
