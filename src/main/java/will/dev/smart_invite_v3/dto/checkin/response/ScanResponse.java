package will.dev.smart_invite_v3.dto.checkin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import will.dev.smart_invite_v3.enums.ScanResult;

@Schema(description = "Résultat du scan QR Code")
public record ScanResponse(

        ScanResult result,

        @Schema(example = "Jean Dupont")
        String guestName,

        @Schema(example = "Mariage de Paul & Marie")
        String eventTitle,

        Integer tableNumber,

        String message,

        @Schema(example = "1", description = "ID de l'événement scanné")
        Long eventId
) {}
