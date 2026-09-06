package will.dev.smart_invite_v3.dto.invitation.request;

import jakarta.validation.constraints.NotNull;
import will.dev.smart_invite_v3.enums.RsvpStatus;

public record RsvpRequest(
        @NotNull RsvpStatus status
) {}
