package will.dev.smart_invite_v3.dto.invitation.request;

import jakarta.validation.constraints.NotNull;

public record GenerateInvitationRequest(
        @NotNull Long guestId
) {}
