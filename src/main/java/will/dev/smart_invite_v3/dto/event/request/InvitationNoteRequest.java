package will.dev.smart_invite_v3.dto.event.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données de la carte d'invitation")
public record InvitationNoteRequest(

        String title,
        String mainMessage,
        String mainMessagePart1,
        String mainMessagePart2,
        String sousMainMessage,
        String eventTheme,
        String priorityColors,
        String qrInstructions,
        String dressCodeMessage,
        String thanksMessage1,
        String closingMessage,
        String titleColor,
        String topBandColor,
        String bottomBandColor,
        String textColor,
        String logoUrl,
        String heartIconUrl,
        Boolean hasInvitationModelCard,
        String code,
        String pdfUrl
) {}
