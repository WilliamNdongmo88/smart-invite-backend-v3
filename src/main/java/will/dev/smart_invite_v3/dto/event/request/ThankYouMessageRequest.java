package will.dev.smart_invite_v3.dto.event.request;

import jakarta.validation.constraints.Size;

public record ThankYouMessageRequest(
        @Size(max = 200) String accroche,
        @Size(max = 200) String corpsLigne1,
        @Size(max = 200) String corpsLigne2,
        @Size(max = 200) String conclusion
) {}
