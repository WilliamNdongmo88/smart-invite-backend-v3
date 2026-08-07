package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThankYouJobService {

    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;
    private final EmailService    emailService;
    private final WhatsAppService whatsAppService;

    @Transactional(readOnly = true)
    public void execute(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<Guest> presentGuests = guestRepository
                .findAllByEventIdAndRsvpStatus(eventId, RsvpStatus.PRESENT);

        if (presentGuests.isEmpty()) {
            log.info("[ThankYouJob] Aucun invité PRESENT pour l'événement {} — job ignoré", eventId);
            return;
        }

        String prefix = event.getType() != null ? event.getType().invitationPrefix() : "à ";
        log.info("[ThankYouJob] Envoi remerciements à {} invités pour l'événement {}", presentGuests.size(), eventId);

        for (Guest guest : presentGuests) {
            NotificationMode mode = guest.getNotificationMode();
            boolean sendEmail    = mode != NotificationMode.WHATSAPP;
            boolean sendWhatsApp = mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH;

            if (sendEmail && guest.getEmail() != null) {
                try {
                    emailService.sendThankYouEmail(
                            guest.getEmail(), guest.getFullName(),
                            event.getTitle(), event.getType());
                } catch (Exception e) {
                    log.warn("[ThankYouJob] Échec email pour {} : {}", guest.getFullName(), e.getMessage());
                }
            }
            if (sendWhatsApp && guest.getPhoneNumber() != null) {
                try {
                    whatsAppService.sendThankYouMessage(
                            guest.getPhoneNumber(), guest.getFullName(),
                            event.getTitle(), prefix, event.getThankYouTemplate());
                } catch (Exception e) {
                    log.warn("[ThankYouJob] Échec WhatsApp pour {} : {}", guest.getFullName(), e.getMessage());
                }
            }
        }

        log.info("[ThankYouJob] Remerciements envoyés pour l'événement {}", eventId);
    }
}
