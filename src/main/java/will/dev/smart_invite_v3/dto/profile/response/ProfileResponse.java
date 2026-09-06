package will.dev.smart_invite_v3.dto.profile.response;

import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.UserRole;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String avatarUrl,
        UserRole role,
        NotificationMode notificationMode,
        Boolean attendanceNotifications,
        Boolean thankNotifications,
        Boolean eventReminders,
        Boolean marketingEmails,
        Boolean notifyMe,
        LocalDateTime createdAt
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getNotificationMode(),
                user.getAttendanceNotifications(),
                user.getThankNotifications(),
                user.getEventReminders(),
                user.getMarketingEmails(),
                user.getNotifyMe(),
                user.getCreatedAt()
        );
    }
}
