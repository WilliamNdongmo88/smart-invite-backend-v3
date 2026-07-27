package will.dev.smart_invite_v3.dto.event.response;

public record EventStatsResponse(
        Long eventId,
        String eventTitle,
        Integer maxGuests,
        long totalGuests,
        long confirmedGuests,
        long pendingGuests,
        long declinedGuests,
        double occupancyRate
) {}
