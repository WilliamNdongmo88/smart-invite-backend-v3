package will.dev.smart_invite_v3.dto.invitation.response;

import java.util.List;

public record BulkGenerateResponse(
        int total,
        int generated,
        int skipped,
        List<String> skippedReasons,
        List<InvitationResponse> invitations
) {}
