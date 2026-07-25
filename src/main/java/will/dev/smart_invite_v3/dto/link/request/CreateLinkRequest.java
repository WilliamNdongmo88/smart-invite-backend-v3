package will.dev.smart_invite_v3.dto.link.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateLinkRequest(
        @NotNull Long eventId,
        Integer limitCount,
        LocalDateTime dateLimitLink
) {}
