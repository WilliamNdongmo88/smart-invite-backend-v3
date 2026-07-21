package will.dev.smart_invite_v3.dto.event.response;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.enums.EventStatus;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        EventType type,
        EventStatus status,
        Integer maxGuests,
        String concernedNames,
        String religiousLocation,
        LocalDateTime religiousDateTime,
        String civilLocation,
        LocalDateTime civilDateTime,
        String banquetLocation,
        LocalDateTime banquetDateTime,
        Long organizerId,
        String organizerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getStatus(),
                event.getMaxGuests(),
                event.getConcernedNames(),
                event.getReligiousLocation(),
                event.getReligiousDateTime(),
                event.getCivilLocation(),
                event.getCivilDateTime(),
                event.getBanquetLocation(),
                event.getBanquetDateTime(),
                event.getOrganizer().getId(),
                event.getOrganizer().getName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
