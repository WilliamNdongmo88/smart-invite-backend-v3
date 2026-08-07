package will.dev.smart_invite_v3.dto.event.response;

import will.dev.smart_invite_v3.entity.ThankYouTemplate;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.enums.EventStatus;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventType type,
        EventStatus status,
        String budget,
        Integer maxGuests,
        String concernedNames,
        LocalDateTime eventDate,
        String religiousLocation,
        LocalDateTime religiousDateTime,
        String civilLocation,
        LocalDateTime civilDateTime,
        String banquetLocation,
        LocalDateTime banquetDateTime,
        Boolean showWeddingReligiousLocation,
        Boolean importMyModelCard,
        ThankYouTemplate thankYouTemplate,
        Long organizerId,
        String organizerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getType(),
                event.getStatus(),
                event.getBudget(),
                event.getMaxGuests(),
                event.getConcernedNames(),
                event.getEventDate(),
                event.getReligiousLocation(),
                event.getReligiousDateTime(),
                event.getCivilLocation(),
                event.getCivilDateTime(),
                event.getBanquetLocation(),
                event.getBanquetDateTime(),
                event.getShowWeddingReligiousLocation(),
                event.getImportMyModelCard(),
                event.getThankYouTemplate(),
                event.getOrganizer().getId(),
                event.getOrganizer().getName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
