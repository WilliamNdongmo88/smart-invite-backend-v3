package will.dev.smart_invite_v3.dto.event.response;

public record ThankYouTemplateResponse(
        String accroche,
        String salutation,       // fixe, non modifiable — ex: "Cher(e) *Jean Dupont*,"
        String corpsLigne1,
        String corpsLigne2,
        String conclusion,
        boolean isCustom
) {}
