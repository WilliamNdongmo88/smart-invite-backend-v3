package will.dev.smart_invite_v3.dto.event.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

public record UpdateEventRequest(

        @NotBlank(message = "Le titre est obligatoire")
        String title,

        String description,

        @NotNull(message = "Le type d'événement est obligatoire")
        EventType type,

        String budget,

        @NotNull(message = "Le nombre maximum d'invités est obligatoire")
        @Min(value = 1, message = "Le nombre d'invités doit être au moins 1")
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

        Boolean importMyModelCard
) {}
