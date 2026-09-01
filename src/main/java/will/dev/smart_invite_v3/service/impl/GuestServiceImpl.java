package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.guest.request.AddGuestRequest;
import will.dev.smart_invite_v3.dto.guest.request.BulkDeleteRequest;
import will.dev.smart_invite_v3.dto.guest.request.UpdateGuestRequest;
import will.dev.smart_invite_v3.dto.guest.response.GuestResponse;
import will.dev.smart_invite_v3.dto.guest.response.ImportGuestResult;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.GuestService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final WhatsAppService      whatsAppService;

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
                .tableNumber(request.tableNumber())
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
        if (request.tableNumber()         != null) guest.setTableNumber(request.tableNumber());
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

        NotificationMode mode = guest.getNotificationMode();
        String email       = guest.getEmail();
        String phone       = guest.getPhoneNumber();
        String guestName   = guest.getFullName();
        String eventTitle  = guest.getEvent().getTitle();
        String token       = inv != null ? inv.getToken() : null;
        String eventPrefix = guest.getEvent().getType() != null
                ? guest.getEvent().getType().invitationPrefix() : "à ";

        boolean sentAny = false;

        // EMAIL ou BOTH
        if ((mode == NotificationMode.EMAIL || mode == NotificationMode.BOTH || mode == null)
                && email != null && !email.isBlank()) {
            try {
                emailService.sendReminderEmail(email, guestName, eventTitle, qrUrl, pdfUrl);
                sentAny = true;
            } catch (Exception e) {
                log.warn("Rappel email échoué pour guest {} : {}", guestId, e.getMessage());
            }
        }

        // WHATSAPP ou BOTH
        if ((mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH)
                && phone != null && !phone.isBlank()) {
            if (token == null) {
                log.warn("Rappel WhatsApp ignoré pour guest {} : pas d'invitation/token", guestId);
            } else {
                try {
                    whatsAppService.sendReminderMessage(phone, guestName, eventTitle, token, eventPrefix);
                    sentAny = true;
                } catch (Exception e) {
                    log.warn("Rappel WhatsApp échoué pour guest {} : {}", guestId, e.getMessage());
                }
            }
        }

        if (!sentAny) {
            throw new RuntimeException(
                "Impossible d'envoyer le rappel : aucun contact valide pour le mode " + mode);
        }
    }

    // ---- Import Excel ----

    @Override
    @Transactional
    public ImportGuestResult importFromExcel(Long eventId, MultipartFile file, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);
        int imported = 0;
        int skipped  = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // Ligne 0 = en-têtes, on commence à 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String fullName = cellStr(row, 0);
                String email    = cellStr(row, 1);
                String phone    = cellStr(row, 2);
                String notifRaw = cellStr(row, 3);
                String tableRaw = cellStr(row, 4);

                if (fullName == null || fullName.isBlank()) {
                    errors.add("Ligne " + (i + 1) + " ignorée : nom vide");
                    skipped++;
                    continue;
                }

                // Vérification doublons
                if (email != null && !email.isBlank()
                        && guestRepository.existsByEventIdAndEmail(eventId, email)) {
                    errors.add("Ligne " + (i + 1) + " ignorée : email '" + email + "' déjà existant");
                    skipped++;
                    continue;
                }
                if (phone != null && !phone.isBlank()
                        && guestRepository.existsByEventIdAndPhoneNumber(eventId, phone)) {
                    errors.add("Ligne " + (i + 1) + " ignorée : téléphone '" + phone + "' déjà existant");
                    skipped++;
                    continue;
                }

                // Quota
                int approvedQuota = paymentRepository.sumApprovedQuotaByEventId(eventId);
                int currentCount  = guestRepository.countByEventId(eventId);
                if (approvedQuota > 0 && currentCount >= approvedQuota) {
                    errors.add("Ligne " + (i + 1) + " et suivantes ignorées : quota atteint (" + approvedQuota + ")");
                    break;
                }

                NotificationMode mode = NotificationMode.EMAIL; // défaut
                if (notifRaw != null) {
                    try { mode = NotificationMode.valueOf(notifRaw.toUpperCase().trim()); }
                    catch (IllegalArgumentException ignored) {}
                }

                Integer tableNumber = null;
                if (tableRaw != null && !tableRaw.isBlank()) {
                    try { tableNumber = Integer.parseInt(tableRaw.trim()); }
                    catch (NumberFormatException ignored) {}
                }

                Guest guest = Guest.builder()
                        .event(event)
                        .fullName(fullName)
                        .email(email != null && !email.isBlank() ? email : null)
                        .phoneNumber(phone != null && !phone.isBlank() ? phone : null)
                        .notificationMode(mode)
                        .tableNumber(tableNumber)
                        .build();
                guestRepository.save(guest);
                imported++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier Excel : " + e.getMessage());
        }

        return new ImportGuestResult(imported, skipped, errors);
    }

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
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
