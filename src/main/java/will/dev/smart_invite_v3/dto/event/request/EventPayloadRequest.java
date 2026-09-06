package will.dev.smart_invite_v3.dto.event.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WeddingEventPayloadRequest.class, name = "MARIAGE"),
        @JsonSubTypes.Type(value = ConferenceEventPayloadRequest.class, name = "CONFERENCE"),
        @JsonSubTypes.Type(value = GalaEventPayloadRequest.class, name = "GALA"),
        @JsonSubTypes.Type(value = CeremonieEventPayloadRequest.class, name = "CEREMONIE")
})
public sealed interface EventPayloadRequest permits
        WeddingEventPayloadRequest,
        ConferenceEventPayloadRequest,
        GalaEventPayloadRequest,
        CeremonieEventPayloadRequest {

    EventType eventType();

    String extractTitle();
    String extractDescription();
    String extractBudget();
    Integer extractMaxGuests();
    String extractConcernedNames();
    LocalDateTime extractEventDate();
    String extractDateLabel();
    String extractVenueName();
    String extractVenueCity();
    String extractCoverPhotoUrl();

    static LocalDateTime parseTargetDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {}
        return null;
    }
}
