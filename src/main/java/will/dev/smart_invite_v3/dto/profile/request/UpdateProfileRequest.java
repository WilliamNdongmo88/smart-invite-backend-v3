package will.dev.smart_invite_v3.dto.profile.request;

import will.dev.smart_invite_v3.enums.NotificationMode;

public record UpdateProfileRequest(
        String name,
        String phone,
        NotificationMode notificationMode,
        Boolean attendanceNotifications,
        Boolean thankNotifications,
        Boolean eventReminders,
        Boolean marketingEmails,
        Boolean notifyMe
) {}
