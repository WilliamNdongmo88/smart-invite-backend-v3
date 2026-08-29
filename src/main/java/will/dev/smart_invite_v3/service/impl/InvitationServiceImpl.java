package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.invitation.request.BulkGenerateRequest;
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.request.RsvpRequest;
import will.dev.smart_invite_v3.dto.invitation.response.BulkGenerateResponse;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;
import will.dev.smart_invite_v3.dto.invitation.response.PublicInvitationResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.entity.InvitationCard;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.InvitationStatus;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationCardRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.InvitationService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    private final EventRepository          eventRepository;
    private final GuestRepository          guestRepository;
    private final InvitationRepository     invitationRepository;
    private final InvitationCardRepository cardRepository;
    private final PaymentRepository        paymentRepository;
    private final FirebaseStorageService   firebaseStorage;
    private final QrCodeService            qrCodeService;
    private final PdfCardGeneratorService  pdfCardGeneratorService;
    private final EmailService             emailService;
    private final WhatsAppService          whatsAppService;
    private final NotificationDispatcher   notificationDispatcher;

    @Value("${app.base.url}")
    private String apiUrl;

    @Value("${app.firebase.storage-bucket}")
    private String firebaseBucket;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // ---- US-020 ----

    @Override
    @Transactional
    public InvitationResponse generate(Long eventId, CreateGuestRequest request, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);
        checkPaymentApproved(eventId, organizerId);

        Guest guest = Guest.builder()
                .event(event)
                .fullName(request.fullName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .notificationMode(request.notificationMode())
                .build();
        guest = guestRepository.save(guest);

        return doGenerate(guest, event);
    }

    // ---- US-021 ----

    @Override
    @Transactional
    public BulkGenerateResponse bulkGenerate(BulkGenerateRequest request, Long organizerId) {
        Event event = resolveOwned(request.eventId(), organizerId);
        checkPaymentApproved(request.eventId(), organizerId);

        List<InvitationResponse> generated = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        for (Long guestId : request.guestIds()) {
            Guest guest = guestRepository.findById(guestId).orElse(null);
            if (guest == null) {
                skippedReasons.add("Invité introuvable : " + guestId);
                continue;
            }
            if (!guest.getEvent().getId().equals(event.getId())) {
                skippedReasons.add("Invité " + guestId + " n'appartient pas à cet événement");
                continue;
            }
            if (invitationRepository.existsByGuestId(guestId)) {
                skippedReasons.add("Invitation déjà générée pour " + guest.getFullName());
                continue;
            }
            try {
                generated.add(doGenerate(guest, event));
            } catch (Exception e) {
                skippedReasons.add("Erreur pour " + guest.getFullName() + " : " + e.getMessage());
                log.warn("Erreur génération invitation guest {} : {}", guestId, e.getMessage());
            }
        }

        return new BulkGenerateResponse(
                request.guestIds().size(),
                generated.size(),
                skippedReasons.size(),
                skippedReasons,
                generated
        );
    }

    // ---- US-022 ----

    @Override
    public PublicInvitationResponse getPublic(String token) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));
        if (inv.getStatus() == InvitationStatus.REVOKED) {
            throw new RuntimeException("Cette invitation a été révoquée");
        }
        return PublicInvitationResponse.from(inv);
    }

    // ---- US-024 ----

    @Override
    @Transactional
    public void delete(Long invitationId, Long organizerId) {
        Invitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable : " + invitationId));
        resolveOwned(inv.getGuest().getEvent().getId(), organizerId);
        deleteFirebaseFiles(inv);
        Guest guest = inv.getGuest();
        if (guest.getRsvpStatus() == RsvpStatus.CONFIRMED) {
            guest.setRsvpStatus(RsvpStatus.PENDING);
            guestRepository.save(guest);
        }
        invitationRepository.delete(inv);
    }

    // ---- Liste ----

    @Override
    public List<InvitationResponse> listByEvent(Long eventId, Long organizerId) {
        resolveOwned(eventId, organizerId);
        return invitationRepository.findAllByGuestEventId(eventId)
                .stream().map(InvitationResponse::from).toList();
    }

    // ---- RSVP ----

    @Override
    @Transactional
    public PublicInvitationResponse rsvp(String token, RsvpRequest request) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));
        if (inv.getStatus() == InvitationStatus.REVOKED) {
            throw new RuntimeException("Cette invitation a été révoquée");
        }
        Guest guest = inv.getGuest();
        guest.setRsvpStatus(request.status());
        guestRepository.save(guest);

        Event event = guest.getEvent();

        // Notification organisateur selon notifyMe + notificationMode
        notificationDispatcher.sendOrganizerNotification(
                event.getOrganizer(),
                () -> emailService.sendRsvpNotification(
                        event.getOrganizer().getEmail(),
                        guest.getFullName(),
                        event.getType(),
                        event.getTitle(),
                        request.status().name()),
                () -> whatsAppService.sendOrganizerTextMessage(
                        event.getOrganizer().getPhone(),
                        buildRsvpWhatsAppMessage(guest.getFullName(), event.getTitle(), event.getType(), request.status().name()))
        );

        // Génération QR + envoi confirmation si CONFIRMED (plus de PDF)
        if (request.status() == RsvpStatus.CONFIRMED) {
            checkQuotaAvailable(event.getId());
            try {
                String folder = activeProfile + "/invitations";
                String publicUrl = apiUrl + "/api/invitations/" + token;

                byte[] qrBytes = qrCodeService.generateWithColor(publicUrl);
                String qrUrl = firebaseStorage.uploadBytes(qrBytes, folder, token + "_qr.png", "image/png");

                // Lien vers la page de l'événement (avec mode prévisualisation)
                String eventPageUrl = buildEventPageUrl(event);

                inv.setQrCodeUrl(qrUrl);
                inv.setIsInvitationSent(true);
                invitationRepository.save(inv);

                notificationDispatcher.sendConfirmation(
                        guest.getNotificationMode(),
                        guest.getEmail(), guest.getPhoneNumber(),
                        guest.getFullName(), event.getType(), event.getTitle(),
                        qrBytes, qrUrl, eventPageUrl, false);
                incrementSentInvitations(event.getId());

            } catch (Exception e) {
                log.warn("Génération/envoi confirmation échoué pour {} : {}", guest.getFullName(), e.getMessage());
            }
        }

        return PublicInvitationResponse.from(inv);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = will.dev.smart_invite_v3.cache.CacheNames.EVENTS, allEntries = true)
    public String updateEventPhotoByToken(String token, org.springframework.web.multipart.MultipartFile file, Long userId) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));
        Event event = inv.getGuest().getEvent();
        String url = firebaseStorage.upload(file, activeProfile + "/events/photos");

        // Mettre à jour la colonne dédiée
        event.setCouplePhotoUrl(url);

        eventRepository.save(event);
        log.info("[Invitation] Photo mise à jour pour l'événement {} via token {} -> {}", event.getId(), token, url);
        return url;
    }

    // ---- Inscription via lien public ----

    @Override
    @Transactional
    public InvitationResponse generateFromLink(Long eventId, CreateGuestRequest request, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);
        checkPaymentApproved(eventId, organizerId);
        checkQuotaAvailable(eventId);

        Guest guest = Guest.builder()
                .event(event)
                .fullName(request.fullName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .notificationMode(request.notificationMode())
                .rsvpStatus(RsvpStatus.CONFIRMED)
                .build();
        guest = guestRepository.save(guest);

        String token = UUID.randomUUID().toString();
        String folder = activeProfile + "/invitations";
        String publicUrl = apiUrl + "/api/invitations/" + token;

        byte[] qrBytes = null;
        String qrUrl = null;
        try {
            qrBytes = qrCodeService.generateWithColor(publicUrl);
            qrUrl = firebaseStorage.uploadBytes(qrBytes, folder, token + "_qr.png", "image/png");
        } catch (Exception e) {
            log.warn("Erreur génération QR via lien pour {} : {}", guest.getEmail(), e.getMessage());
        }

        Invitation invitation = Invitation.builder()
                .guest(guest)
                .event(event)
                .token(token)
                .qrCodeUrl(qrUrl)
                .status(InvitationStatus.ACTIVE)
                .isInvitationSent(true)
                .build();
        invitation = invitationRepository.save(invitation);

        if (qrBytes != null) {
            String eventPageUrl = buildEventPageUrl(event);
            notificationDispatcher.sendConfirmation(
                    guest.getNotificationMode(),
                    guest.getEmail(), guest.getPhoneNumber(),
                    guest.getFullName(), event.getType(), event.getTitle(),
                    qrBytes, qrUrl, eventPageUrl, true);
            incrementSentInvitations(event.getId());
        }

        return InvitationResponse.from(invitation);
    }

    // ---- Core generation ----

    /**
     * Construit l'URL de la page de l'événement côté frontend pour les invités.
     * Utilise ?preview_details=true (distinct de ?preview=true réservé à l'organisateur).
     * Ex : http://localhost:4200/events/2/wedding?preview_details=true
     */
    private String buildEventPageUrl(Event event) {
        if (event.getType() == null) return frontendUrl;
        String path = switch (event.getType()) {
            case MARIAGE    -> "/events/" + event.getId() + "/wedding?preview_details=true";
            case CONFERENCE -> "/events/" + event.getId() + "/conference?preview_details=true";
            case GALA       -> "/events/" + event.getId() + "/gala?preview_details=true";
            case CEREMONIE  -> "/events/" + event.getId() + "/ceremonie?preview_details=true";
        };
        return frontendUrl + path;
    }

    private InvitationResponse doGenerate(Guest guest, Event event) {
        String token = UUID.randomUUID().toString();
        String rsvpLink = frontendUrl + "/invitations/" + token + "/rsvp";

        Invitation invitation = Invitation.builder()
                .guest(guest)
                .event(event)
                .token(token)
                .status(InvitationStatus.ACTIVE)
                .isInvitationSent(false)
                .build();
        invitation = invitationRepository.save(invitation);

        notificationDispatcher.sendRsvpInvite(
                guest.getNotificationMode(),
                guest.getEmail(), guest.getPhoneNumber(),
                guest.getFullName(), event.getTitle(),
                event.getType(), rsvpLink, token);

        return InvitationResponse.from(invitation);
    }

    private byte[] buildPdfBytes(Event event, InvitationCard card, byte[] qrBytes, String guestName) {
        if (card != null && Boolean.TRUE.equals(card.getHasInvitationModelCard()) && card.getPdfUrl() != null) {
            byte[] imported = firebaseStorage.downloadBytes(
                    card.getPdfUrl().replace("https://storage.googleapis.com/" + firebaseBucket + "/", ""));
            if (imported != null) return imported;
        }
        try {
            return pdfCardGeneratorService.generate(event, card, qrBytes, guestName);
        } catch (Exception e) {
            log.warn("Erreur génération PDF pour {} : {}", guestName, e.getMessage());
            return null;
        }
    }

    private void deleteFirebaseFiles(Invitation inv) {
        String base = "https://storage.googleapis.com/" + firebaseBucket + "/";
        if (inv.getQrCodeUrl() != null) firebaseStorage.delete(inv.getQrCodeUrl().replace(base, ""));
        if (inv.getPdfUrl()    != null) firebaseStorage.delete(inv.getPdfUrl().replace(base, ""));
    }

    private void checkQuotaAvailable(Long eventId) {
        paymentRepository.findTopByEventIdAndStatusOrderByCreatedAtDesc(
                eventId, will.dev.smart_invite_v3.enums.PaymentStatus.APPROVED)
            .ifPresent(payment -> {
                if (payment.getSentInvitations() >= payment.getPaidQuota()) {
                    throw new RuntimeException(
                        "Quota d'invitations épuisé : vous avez atteint la limite de " +
                        payment.getPaidQuota() + " invitations. Veuillez soumettre une nouvelle preuve de paiement.");
                }
            });
    }

    private void checkPaymentApproved(Long eventId, Long organizerId) {
        if (!paymentRepository.existsByEventIdAndOrganizerIdAndStatus(
                eventId, organizerId, will.dev.smart_invite_v3.enums.PaymentStatus.APPROVED)) {
            throw new RuntimeException(
                "Paiement requis : veuillez effectuer et faire approuver votre paiement avant de générer des invitations.");
        }
    }

    private void incrementSentInvitations(Long eventId) {
        paymentRepository.findTopByEventIdAndStatusOrderByCreatedAtDesc(
                eventId, will.dev.smart_invite_v3.enums.PaymentStatus.APPROVED)
            .ifPresent(payment -> {
                payment.setSentInvitations(payment.getSentInvitations() + 1);
                paymentRepository.save(payment);
                if (payment.getSentInvitations().equals(payment.getPaidQuota())) {
                    notificationDispatcher.sendOrganizerNotification(
                            payment.getOrganizer(),
                            () -> emailService.sendQuotaReachedNotification(
                                    payment.getOrganizer().getEmail(),
                                    payment.getOrganizer().getName(),
                                    payment.getEvent().getTitle(),
                                    payment.getPaidQuota()),
                            () -> whatsAppService.sendOrganizerTextMessage(
                                    payment.getOrganizer().getPhone(),
                                    "⚠️ *Quota atteint !*\n\nVous avez atteint votre quota de *" +
                                    payment.getPaidQuota() + " invitations* pour *" +
                                    payment.getEvent().getTitle() + "*.\n" +
                                    "Soumettez une nouvelle preuve de paiement pour continuer.")
                    );
                }
            });
    }

    private String buildRsvpWhatsAppMessage(String guestName, String eventTitle, EventType eventType, String status) {
        String label = "CONFIRMED".equals(status) ? "confirmé ✅" : "décliné ❌";
        return String.join("\n",
            "╔═════════════════════╗",
            "              ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "📩 *Réponse RSVP reçue*",
            "",
            "*" + guestName + "* a *" + label + "* sa participation "+ eventType.invitationPrefix() +" *" + eventTitle + "*.",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "              🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );
    }

    private Event resolveOwned(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new EventAccessDeniedException();
        }
        return event;
    }
}
