package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.guest.request.AddGuestRequest;
import will.dev.smart_invite_v3.dto.guest.request.BulkDeleteRequest;
import will.dev.smart_invite_v3.dto.guest.request.UpdateGuestRequest;
import will.dev.smart_invite_v3.dto.guest.response.GuestResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.GuestService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {

    private final GuestRepository      guestRepository;
    private final EventRepository      eventRepository;
    private final InvitationRepository invitationRepository;
    private final PaymentRepository    paymentRepository;
    private final EmailService         emailService;

    // ---- US-015 ----

    @Override
    @Transactional
    public GuestResponse add(Long eventId, AddGuestRequest request, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);

        // Vérification quota payé
        int approvedQuota = paymentRepository.sumApprovedQuotaByEventId(eventId);
        int currentCount  = guestRepository.countByEventId(eventId);
        if (approvedQuota > 0 && currentCount >= approvedQuota) {
            throw new RuntimeException("Quota d'invités atteint (" + approvedQuota + "). Souscrivez un nouveau quota.");
        }

        // Vérification doublons
        if (request.email() != null && !request.email().isBlank()
                && guestRepository.existsByEventIdAndEmail(eventId, request.email())) {
            throw new RuntimeException("Un invité avec cet email existe déjà pour cet événement");
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()
                && guestRepository.existsByEventIdAndPhoneNumber(eventId, request.phoneNumber())) {
            throw new RuntimeException("Un invité avec ce numéro existe déjà pour cet événement");
        }

        Guest guest = Guest.builder()
                .event(event)
                .fullName(request.fullName())
                .notificationMode(request.notificationMode())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .dietaryRestrictions(request.dietaryRestrictions())
                .tableNumber(request.tableNumber())
                .companionName(request.companionName())
                .build();

        return GuestResponse.from(guestRepository.save(guest));
    }

    // ---- Liste paginée ----

    @Override
    public Page<GuestResponse> list(Long eventId, String search, RsvpStatus rsvp,
                                    Pageable pageable, Long organizerId) {
        resolveOwned(eventId, organizerId);
        return guestRepository.findByEventIdFiltered(eventId, search, rsvp, pageable)
                .map(GuestResponse::from);
    }

    // ---- US-017 ----

    @Override
    @Transactional
    public GuestResponse update(Long guestId, UpdateGuestRequest request, Long organizerId) {
        Guest guest = resolveOwnedGuest(guestId, organizerId);

        if (request.fullName()            != null) guest.setFullName(request.fullName());
        if (request.email()               != null) guest.setEmail(request.email());
        if (request.phoneNumber()         != null) guest.setPhoneNumber(request.phoneNumber());
        if (request.dietaryRestrictions() != null) guest.setDietaryRestrictions(request.dietaryRestrictions());
        if (request.tableNumber()         != null) guest.setTableNumber(request.tableNumber());
        if (request.companionName()       != null) guest.setCompanionName(request.companionName());
        if (request.notificationMode()    != null) guest.setNotificationMode(request.notificationMode());

        return GuestResponse.from(guestRepository.save(guest));
    }

    // ---- US-018 ----

    @Override
    @Transactional
    public void delete(Long guestId, Long organizerId) {
        Guest guest = resolveOwnedGuest(guestId, organizerId);
        deleteGuestWithCleanup(guest);
    }

    @Override
    @Transactional
    public void bulkDelete(BulkDeleteRequest request, Long organizerId) {
        for (Long guestId : request.guestIds()) {
            Guest guest = guestRepository.findById(guestId).orElse(null);
            if (guest == null) continue;
            if (!guest.getEvent().getOrganizer().getId().equals(organizerId)) continue;
            deleteGuestWithCleanup(guest);
        }
    }

    // ---- US-019 ----

    @Override
    public void sendReminder(Long guestId, Long organizerId) {
        Guest guest = resolveOwnedGuest(guestId, organizerId);

        Invitation inv = invitationRepository.findByGuestId(guestId).orElse(null);
        String qrUrl  = inv != null ? inv.getQrCodeUrl() : null;
        String pdfUrl = inv != null ? inv.getPdfUrl()    : null;

        if (guest.getEmail() != null && !guest.getEmail().isBlank()) {
            try {
                emailService.sendReminderEmail(
                        guest.getEmail(), guest.getFullName(),
                        guest.getEvent().getTitle(), qrUrl, pdfUrl);
            } catch (Exception e) {
                log.warn("Rappel email échoué pour guest {} : {}", guestId, e.getMessage());
                throw new RuntimeException("Échec de l'envoi du rappel : " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Cet invité n'a pas d'email renseigné");
        }
    }

    // ---- Helpers ----

    private void deleteGuestWithCleanup(Guest guest) {
        // La suppression en cascade (ON DELETE CASCADE) supprime l'invitation en DB
        // Les fichiers Firebase (QR + PDF) sont orphelins — nettoyage best-effort
        invitationRepository.findByGuestId(guest.getId()).ifPresent(inv -> {
            log.info("Suppression invitation {} (guest {})", inv.getId(), guest.getId());
            invitationRepository.delete(inv);
        });
        guestRepository.delete(guest);
    }

    private Event resolveOwned(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getOrganizer().getId().equals(organizerId)) throw new EventAccessDeniedException();
        return event;
    }

    private Guest resolveOwnedGuest(Long guestId, Long organizerId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("Invité introuvable : " + guestId));
        if (!guest.getEvent().getOrganizer().getId().equals(organizerId)) throw new EventAccessDeniedException();
        return guest;
    }
}
