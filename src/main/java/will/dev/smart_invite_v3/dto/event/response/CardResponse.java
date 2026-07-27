package will.dev.smart_invite_v3.dto.event.response;

import will.dev.smart_invite_v3.entity.InvitationCard;

public record CardResponse(
        Long id,
        Long eventId,
        String title,
        String mainMessage,
        String mainMessagePart1,
        String mainMessagePart2,
        String sousMainMessage,
        String eventTheme,
        String qrInstructions,
        String dressCodeMessage,
        String thanksMessage1,
        String closingMessage,
        String civilNote,
        String titleColor,
        String topBandColor,
        String bottomBandColor,
        String textColor,
        String logoUrl,
        String heartIconUrl,
        String pdfUrl,
        Boolean hasInvitationModelCard,
        String code
) {
    public static CardResponse from(InvitationCard card, Long eventId) {
        return new CardResponse(
                card.getId(),
                eventId,
                card.getTitle(),
                card.getMainMessage(),
                card.getMainMessagePart1(),
                card.getMainMessagePart2(),
                card.getSousMainMessage(),
                card.getEventTheme(),
                card.getQrInstructions(),
                card.getDressCodeMessage(),
                card.getThanksMessage1(),
                card.getClosingMessage(),
                card.getCivilNote(),
                card.getTitleColor(),
                card.getTopBandColor(),
                card.getBottomBandColor(),
                card.getTextColor(),
                card.getLogoUrl(),
                card.getHeartIconUrl(),
                card.getPdfUrl(),
                Boolean.TRUE.equals(card.getHasInvitationModelCard()),
                card.getCode()
        );
    }
}
