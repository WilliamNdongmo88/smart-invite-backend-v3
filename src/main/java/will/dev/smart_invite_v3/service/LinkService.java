package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.link.request.CreateLinkRequest;
import will.dev.smart_invite_v3.dto.link.request.UpdateLinkRequest;
import will.dev.smart_invite_v3.dto.link.response.LinkResponse;
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;

import java.util.List;

public interface LinkService {
    LinkResponse create(CreateLinkRequest request, Long organizerId);
    List<LinkResponse> getByEvent(Long eventId, Long organizerId);
    LinkResponse update(Long linkId, UpdateLinkRequest request, Long organizerId);
    void delete(Long linkId, Long organizerId);
    InvitationResponse join(String token, CreateGuestRequest request);
}
