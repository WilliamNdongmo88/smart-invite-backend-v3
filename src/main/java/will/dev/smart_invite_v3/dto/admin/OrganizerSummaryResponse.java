package will.dev.smart_invite_v3.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record OrganizerSummaryResponse(
        Long id,
        String name,
        String email,
        String phone,
        Boolean isActive,
        Boolean isBlocked,
        LocalDateTime createdAt,
        List<EventSummaryResponse> events
) {}
