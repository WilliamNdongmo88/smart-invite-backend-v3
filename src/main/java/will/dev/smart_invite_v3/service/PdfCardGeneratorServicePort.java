package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.InvitationCard;
import java.io.IOException;

public interface PdfCardGeneratorServicePort {
    byte[] generate(Event event, InvitationCard card, byte[] qrCodeBytes, String guestName) throws IOException;
}
