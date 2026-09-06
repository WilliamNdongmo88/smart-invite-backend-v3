package will.dev.smart_invite_v3.dto.guest.response;

import java.util.List;

public record ImportGuestResult(
        int imported,
        int skipped,
        List<String> errors
) {}
