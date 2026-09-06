package will.dev.smart_invite_v3.dto.guest.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkDeleteRequest(
        @NotEmpty List<Long> guestIds
) {}
