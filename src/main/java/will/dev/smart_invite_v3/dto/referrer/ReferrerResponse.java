package will.dev.smart_invite_v3.dto.referrer;

import will.dev.smart_invite_v3.enums.NotificationMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReferrerResponse(
        Long id,
        String name,
        String phone,
        String email,
        String code,
        NotificationMode notificationMode,
        Boolean active,
        LocalDateTime createdAt,
        long registrations,
        long events,
        BigDecimal approvedAmount
) {}