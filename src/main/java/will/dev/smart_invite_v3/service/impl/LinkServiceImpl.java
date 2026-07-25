package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;
import will.dev.smart_invite_v3.dto.link.request.CreateLinkRequest;
import will.dev.smart_invite_v3.dto.link.request.UpdateLinkRequest;
import will.dev.smart_invite_v3.dto.link.response.LinkResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Link;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.LinkRepository;
import will.dev.smart_invite_v3.service.InvitationService;
import will.dev.smart_invite_v3.service.LinkService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkServiceImpl implements LinkService {

    private final LinkRepository    linkRepository;
    private final EventRepository   eventRepository;
    private final InvitationService invitationService;

    @Value("${app.base.url}")
    private String baseUrl;

    // ---- US-035 ----

    @Override
    @Transactional
    public LinkResponse create(CreateLinkRequest request, Long organizerId) {
        Event event = resolveOwned(request.eventId(), organizerId);
        Link link = Link.builder()
                .event(event)
                .token(UUID.randomUUID().toString())
                .limitCount(request.limitCount())
                .dateLimitLink(request.dateLimitLink())
                .build();
        return LinkResponse.from(linkRepository.save(link), baseUrl);
    }

    // ---- US-036 ----

    @Override
    public List<LinkResponse> getByEvent(Long eventId, Long organizerId) {
        resolveOwned(eventId, organizerId);
        return linkRepository.findAllByEventId(eventId)
                .stream().map(l -> LinkResponse.from(l, baseUrl)).toList();
    }

    @Override
    @Transactional
    public LinkResponse update(Long linkId, UpdateLinkRequest request, Long organizerId) {
        Link link = resolveOwnedLink(linkId, organizerId);
        if (request.limitCount() != null)    link.setLimitCount(request.limitCount());
        if (request.dateLimitLink() != null) link.setDateLimitLink(request.dateLimitLink());
        return LinkResponse.from(linkRepository.save(link), baseUrl);
    }

    @Override
    @Transactional
    public void delete(Long linkId, Long organizerId) {
        linkRepository.delete(resolveOwnedLink(linkId, organizerId));
    }

    // ---- Inscription publique ----

    @Override
    @Transactional
    public InvitationResponse join(String token, CreateGuestRequest request) {
        Link link = linkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lien invalide"));

        if (link.getDateLimitLink() != null && link.getDateLimitLink().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ce lien a expiré");
        }
        if (link.getLimitCount() != null && link.getUsedCount() >= link.getLimitCount()) {
            throw new RuntimeException("Ce lien a atteint sa limite d'utilisations");
        }

        link.setUsedCount(link.getUsedCount() + 1);
        linkRepository.save(link);

        return invitationService.generate(link.getEvent().getId(), request,
                link.getEvent().getOrganizer().getId());
    }

    // ---- Helpers ----

    private Event resolveOwned(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getOrganizer().getId().equals(organizerId)) throw new EventAccessDeniedException();
        return event;
    }

    private Link resolveOwnedLink(Long linkId, Long organizerId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Lien introuvable : " + linkId));
        if (!link.getEvent().getOrganizer().getId().equals(organizerId)) throw new EventAccessDeniedException();
        return link;
    }
}
