package will.dev.smart_invite_v3.dto.invitation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import will.dev.smart_invite_v3.enums.NotificationMode;

public record CreateGuestRequest(
        @NotBlank String fullName,
        String email,
        String phoneNumber,
        NotificationMode notificationMode
) {}
