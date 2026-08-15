package will.dev.smart_invite_v3.dto.checkin.response;

import will.dev.smart_invite_v3.entity.CheckinParameters;

public record CheckinParametersResponse(
        Long eventId,
        boolean confirmationSound,
        int totalScans,
        int validScans,
        int duplicateScans,
        int invalidScans
) {
    public static CheckinParametersResponse from(CheckinParameters p) {
        return new CheckinParametersResponse(
                p.getEvent().getId(),
                Boolean.TRUE.equals(p.getConfirmationSound()),
                p.getTotalScans()     != null ? p.getTotalScans()     : 0,
                p.getValidScans()     != null ? p.getValidScans()     : 0,
                p.getDuplicateScans() != null ? p.getDuplicateScans() : 0,
                p.getInvalidScans()   != null ? p.getInvalidScans()   : 0
        );
    }
}
