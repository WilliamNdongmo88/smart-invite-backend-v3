package will.dev.smart_invite_v3.dto.guest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import will.dev.smart_invite_v3.enums.NotificationMode;

public record AddGuestRequest(
        @NotBlank String fullName,
        @NotNull NotificationMode notificationMode,
        String email,
        String phoneNumber,
        Integer tableNumber
) {}
