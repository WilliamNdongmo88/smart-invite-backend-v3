package will.dev.smart_invite_v3.dto.invitation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkGenerateRequest(
        @NotNull Long eventId,
        @NotEmpty List<Long> guestIds
) {}
