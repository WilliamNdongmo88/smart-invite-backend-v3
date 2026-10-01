package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.InvitationCard;
import java.io.IOException;

public interface InvitationPdfServicePort {
    byte[] generate(Guest guest, Event event, InvitationCard card, byte[] qrCodeBytes) throws IOException;
}
