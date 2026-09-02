package will.dev.smart_invite_v3.dto.checkin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.enums.EventType;

@Schema(description = "Résumé léger d'un événement pour le sélecteur agent")
public record EventSummaryResponse(
        Long id,
        String title,
        EventType type,
        String dateLabel,
        String venueName,
        String venueCity
) {
    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getDateLabel(),
                event.getVenueName(),
                event.getVenueCity()
        );
    }
}
