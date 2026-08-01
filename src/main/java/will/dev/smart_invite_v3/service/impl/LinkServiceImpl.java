package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.InvitationService;
import will.dev.smart_invite_v3.service.LinkService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkServiceImpl implements LinkService {

    private final LinkRepository    linkRepository;
    private final EventRepository   eventRepository;
    private final InvitationService invitationService;
    private final EmailService      emailService;
    private final WhatsAppService   whatsAppService;
    private final NotificationDispatcher notificationDispatcher;
    private final PaymentRepository paymentRepository;

    @Value("${app.env.apiUrl}")
    private String apiUrl;

    // ---- US-035 ----

    @Override
    @Transactional
    public LinkResponse create(CreateLinkRequest request, Long organizerId) {
        Event event = resolveOwned(request.eventId(), organizerId);
        if (!paymentRepository.existsByEventIdAndOrganizerIdAndStatus(
                request.eventId(), organizerId, will.dev.smart_invite_v3.enums.PaymentStatus.APPROVED)) {
            throw new RuntimeException(
                "Paiement requis : veuillez effectuer et faire approuver votre paiement avant de créer un lien d'invitation.");
        }
        Link link = Link.builder()
                .event(event)
                .token(UUID.randomUUID().toString())
                .limitCount(request.limitCount())
                .dateLimitLink(request.dateLimitLink())
                .build();
        return LinkResponse.from(linkRepository.save(link), apiUrl);
    }

    // ---- US-036 ----

    @Override
    public List<LinkResponse> getByEvent(Long eventId, Long organizerId) {
        resolveOwned(eventId, organizerId);
        return linkRepository.findAllByEventId(eventId)
                .stream().map(l -> LinkResponse.from(l, apiUrl)).toList();
    }

    @Override
    @Transactional
    public LinkResponse update(Long linkId, UpdateLinkRequest request, Long organizerId) {
        Link link = resolveOwnedLink(linkId, organizerId);
        if (request.limitCount() != null)    link.setLimitCount(request.limitCount());
        if (request.dateLimitLink() != null) link.setDateLimitLink(request.dateLimitLink());
        return LinkResponse.from(linkRepository.save(link), apiUrl);
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

        InvitationResponse invitation = invitationService.generateFromLink(
                link.getEvent().getId(), request, link.getEvent().getOrganizer().getId());

        // Notification organisateur selon notifyMe + notificationMode
        notificationDispatcher.sendOrganizerNotification(
                link.getEvent().getOrganizer(),
                () -> emailService.sendNewGuestNotification(
                        link.getEvent().getOrganizer().getEmail(),
                        request.fullName(),
                        link.getEvent().getTitle()),
                () -> whatsAppService.sendOrganizerTextMessage(
                        link.getEvent().getOrganizer().getPhone(),
                        "🎉 *Nouvelle inscription !*\n\n*" + request.fullName() +
                        "* vient de s'inscrire à votre événement *" + link.getEvent().getTitle() + "*."
                )
        );

        return invitation;
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
