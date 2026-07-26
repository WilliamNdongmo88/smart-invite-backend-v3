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
import will.dev.smart_invite_v3.enums.InvitationStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationCardRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.InvitationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    private final EventRepository        eventRepository;
    private final GuestRepository        guestRepository;
    private final InvitationRepository   invitationRepository;
    private final InvitationCardRepository cardRepository;
    private final FirebaseStorageService firebaseStorage;
    private final QrCodeService          qrCodeService;
    private final PdfCardGeneratorService pdfCardGeneratorService;
    private final EmailService           emailService;

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
        if (guest.getRsvpStatus() == will.dev.smart_invite_v3.enums.RsvpStatus.CONFIRMED) {
            guest.setRsvpStatus(will.dev.smart_invite_v3.enums.RsvpStatus.PENDING);
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

        // Notification organisateur
        try {
            emailService.sendRsvpNotification(
                    event.getOrganizer().getEmail(),
                    guest.getFullName(),
                    event.getTitle(),
                    request.status().name());
        } catch (Exception e) {
            log.warn("Notification RSVP organisateur échouée : {}", e.getMessage());
        }

        // Génération QR + PDF + envoi confirmation si CONFIRMED
        if (request.status() == will.dev.smart_invite_v3.enums.RsvpStatus.CONFIRMED
                && guest.getEmail() != null && !guest.getEmail().isBlank()) {
            try {
                String folder = activeProfile + "/invitations";
                String publicUrl = apiUrl + "/api/invitations/" + token;

                // QR Code
                byte[] qrBytes = qrCodeService.generateWithColor(publicUrl);
                String qrUrl = firebaseStorage.uploadBytes(qrBytes, folder, token + "_qr.png", "image/png");

                // PDF carte d'invitation (même contenu que la carte créée à l'événement)
                InvitationCard card = cardRepository.findByEventId(event.getId()).orElse(null);
                byte[] pdfBytes = pdfCardGeneratorService.generate(event, card, qrBytes, guest.getFullName());
                String pdfUrl = firebaseStorage.uploadBytes(pdfBytes, folder, token + "_invitation.pdf", "application/pdf");

                // Mise à jour invitation
                inv.setQrCodeUrl(qrUrl);
                inv.setPdfUrl(pdfUrl);
                inv.setIsInvitationSent(true);
                invitationRepository.save(inv);

                // Email de confirmation avec pièces jointes
                emailService.sendConfirmationEmail(
                        guest.getEmail(), guest.getFullName(),
                        event.getTitle(), qrBytes, pdfBytes);

            } catch (Exception e) {
                log.warn("Génération/envoi confirmation échoué pour {} : {}", guest.getEmail(), e.getMessage());
            }
        }

        return PublicInvitationResponse.from(inv);
    }

    // ---- Core generation ----

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

        // Envoi email lien RSVP si email présent
        if (guest.getEmail() != null && !guest.getEmail().isBlank()) {
            try {
                emailService.sendRsvpInviteEmail(
                        guest.getEmail(), guest.getFullName(),
                        event.getTitle(), rsvpLink);
            } catch (Exception e) {
                log.warn("Envoi email RSVP échoué pour {} : {}", guest.getEmail(), e.getMessage());
            }
        }

        return InvitationResponse.from(invitation);
    }

    private void deleteFirebaseFiles(Invitation inv) {
        String base = "https://storage.googleapis.com/" + firebaseBucket + "/";
        if (inv.getQrCodeUrl() != null) {
            firebaseStorage.delete(inv.getQrCodeUrl().replace(base, ""));
        }
        if (inv.getPdfUrl() != null) {
            firebaseStorage.delete(inv.getPdfUrl().replace(base, ""));
        }
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
