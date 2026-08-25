package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.event.request.InvitationNoteRequest;
import will.dev.smart_invite_v3.dto.event.response.CardResponse;
import will.dev.smart_invite_v3.dto.event.response.EventWithCardResponse;

public interface InvitationCardService {

    CardResponse saveOrUpdate(Long eventId, InvitationNoteRequest request, Long organizerId);

    EventWithCardResponse getCard(Long eventId, Long organizerId);

    byte[] generatePdf(Long eventId, Long organizerId);

    CardResponse uploadCustomModel(Long eventId, MultipartFile file, Long organizerId);
}
