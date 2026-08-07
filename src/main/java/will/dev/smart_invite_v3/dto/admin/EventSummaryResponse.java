package will.dev.smart_invite_v3.dto.admin;

import will.dev.smart_invite_v3.enums.PaymentStatus;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        LocalDateTime eventDate,
        PaymentStatus paymentStatus
) {}
