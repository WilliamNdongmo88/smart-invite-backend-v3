package will.dev.smart_invite_v3.dto.guest.request;

import will.dev.smart_invite_v3.enums.NotificationMode;

public record UpdateGuestRequest(
        String fullName,
        String email,
        String phoneNumber,
        String dietaryRestrictions,
        Integer tableNumber,
        String companionName,
        NotificationMode notificationMode
) {}
