package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.admin.AdminEventDetailResponse;
import will.dev.smart_invite_v3.dto.admin.ContactReplyRequest;
import will.dev.smart_invite_v3.dto.admin.EventSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.UserNewsResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.entity.UserNews;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.entity.Referrer;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.repository.UserNewsRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.AdminService;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.ReferrerService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository     userRepository;
    private final EventRepository    eventRepository;
    private final PaymentRepository  paymentRepository;
    private final GuestRepository    guestRepository;
    private final UserNewsRepository userNewsRepository;
    private final WhatsAppService    whatsAppService;
    private final EmailService       emailService;
    private final ReferrerService    referrerService;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ──────────────────────────────────────────────────────────────────
    // Organisateurs
    // ──────────────────────────────────────────────────────────────────

    @Override
    public List<OrganizerSummaryResponse> getAllOrganizers() {
        List<User> organizers = userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.USER);
        return mapOrganizers(organizers);
    }

    @Override
    public org.springframework.data.domain.Page<OrganizerSummaryResponse> getAllOrganizers(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<User> page = userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.USER, pageable);
        List<OrganizerSummaryResponse> content = mapOrganizers(page.getContent());
        return new org.springframework.data.domain.PageImpl<>(content, pageable, page.getTotalElements());
    }

    private List<OrganizerSummaryResponse> mapOrganizers(List<User> organizers) {
        List<Long> organizerIds = organizers.stream().map(User::getId).toList();

        List<Event> allEvents = organizerIds.stream()
                .flatMap(id -> eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(id).stream())
                .toList();

        List<Long> eventIds = allEvents.stream().map(Event::getId).toList();

        Map<Long, PaymentStatus> paymentByEvent = paymentRepository.findAllByEventIdIn(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getEvent().getId(),
                        Payment::getStatus,
                        (a, b) -> a
                ));

        Map<Long, List<Event>> eventsByOrganizer = allEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getOrganizer().getId()));

        return organizers.stream().map(user -> {
            List<EventSummaryResponse> events = eventsByOrganizer
                    .getOrDefault(user.getId(), List.of())
                    .stream()
                    .map(e -> new EventSummaryResponse(
                            e.getId(),
                            e.getTitle(),
                            e.getEventDate(),
                            paymentByEvent.get(e.getId())
                    ))
                    .toList();

            return new OrganizerSummaryResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getIsActive(),
                    user.getIsBlocked(),
                    user.getReferralCode(),
                    user.getCreatedAt(),
                    events
            );
        }).toList();
    }

    // ──────────────────────────────────────────────────────────────────
    // Détail d'un événement (vue admin)
    // ──────────────────────────────────────────────────────────────────

    @Override
    public AdminEventDetailResponse getEventDetail(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable : " + eventId));

        User organizer = event.getOrganizer();

        AdminEventDetailResponse.PaymentDetail payment = paymentRepository
                .findTopByEventIdAndOrganizerIdOrderByCreatedAtDesc(eventId, organizer.getId())
                .map(p -> new AdminEventDetailResponse.PaymentDetail(
                        p.getStatus(),
                        p.getQuota(),
                        p.getPaidQuota(),
                        p.getAmount(),
                        p.getRejectionReason(),
                        p.getProofUrl(),
                        p.getReferralCode(),
                        p.getCreatedAt()
                ))
                .orElse(null);

        long total     = guestRepository.countByEventId(eventId);
        long confirmed = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.CONFIRMED);
        long pending   = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING);
        long declined  = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED);
        int maxGuests  = event.getMaxGuests() != null ? event.getMaxGuests() : 0;
        double occupancy = maxGuests > 0
                ? Math.round((confirmed / (double) maxGuests) * 1000.0) / 10.0
                : 0.0;

        AdminEventDetailResponse.StatsDetail stats = new AdminEventDetailResponse.StatsDetail(
                total, confirmed, pending, declined, occupancy);

        return new AdminEventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getType(),
                event.getStatus(),
                event.getBudget(),
                event.getMaxGuests(),
                event.getConcernedNames(),
                event.getEventDate(),
                event.getDateLabel(),
                event.getVenueName(),
                event.getVenueCity(),
                event.getCouplePhotoUrl(),
                event.getReferralCode(),
                event.getCreatedAt(),
                organizer.getId(),
                organizer.getName(),
                organizer.getEmail(),
                organizer.getPhone(),
                payment,
                stats
        );
    }

    // ──────────────────────────────────────────────────────────────────
    // Gestion des utilisateurs
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void blockUser(Long userId) {
        User user = resolve(userId);
        user.setIsBlocked(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unblockUser(Long userId) {
        User user = resolve(userId);
        user.setIsBlocked(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = resolve(userId);
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = resolve(userId);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void assignReferralCode(Long userId, String referralCode) {
        User user = resolve(userId);

        if (referralCode == null || referralCode.isBlank()) {
            user.setReferralCode(null);
            userRepository.save(user);
            return;
        }

        String cleanCode = referralCode.trim().toUpperCase();
        Referrer referrer = referrerService.getActiveByCode(cleanCode);

        user.setReferralCode(referrer.getCode());
        userRepository.save(user);

        // Rétro-affecter aux événements de l'organisateur qui n'ont pas encore de code
        List<Event> events = eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(userId);
        boolean eventUpdated = false;
        for (Event event : events) {
            if (event.getReferralCode() == null || event.getReferralCode().isBlank()) {
                event.setReferralCode(referrer.getCode());
                eventUpdated = true;
            }
        }
        if (eventUpdated) {
            eventRepository.saveAll(events);
        }

        // Rétro-affecter aux paiements de l'organisateur qui n'ont pas encore de code
        List<Payment> payments = paymentRepository.findAllByOrganizerIdOrderByCreatedAtDesc(userId);
        boolean paymentUpdated = false;
        for (Payment payment : payments) {
            if (payment.getReferralCode() == null || payment.getReferralCode().isBlank()) {
                payment.setReferralCode(referrer.getCode());
                paymentUpdated = true;
            }
        }
        if (paymentUpdated) {
            paymentRepository.saveAll(payments);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Messages de contact (usernews)
    // ──────────────────────────────────────────────────────────────────

    @Override
    public List<UserNewsResponse> getAllContacts() {
        return userNewsRepository.findAllByMessageIsNotNullOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void replyToContact(Long contactId, ContactReplyRequest request) {
        UserNews contact = userNewsRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));

        String channel = contact.getReplyChannel();
        String target  = contact.getReplyContact();
        String senderName = contact.getName() != null ? contact.getName() : "Visiteur";

        if ("WHATSAPP".equalsIgnoreCase(channel)) {
            String msg = buildWhatsAppReply(senderName, request.getReplyMessage());
            whatsAppService.sendOrganizerTextMessage(target, msg);
        } else {
            emailService.sendAdminReply(target, senderName, request.getReplyMessage());
        }
    }

    @Override
    @Transactional
    public UserNewsResponse markContactAsRead(Long contactId) {
        UserNews contact = userNewsRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));
        contact.setIsRead(true);
        return toDto(userNewsRepository.save(contact));
    }

    @Override
    @Transactional
    public void deleteContact(Long contactId) {
        UserNews contact = userNewsRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));
        userNewsRepository.delete(contact);
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private UserNewsResponse toDto(UserNews un) {
        return UserNewsResponse.builder()
                .id(un.getId())
                .name(un.getName())
                .email(un.getEmail())
                .phone(un.getPhone())
                .message(un.getMessage())
                .replyChannel(un.getReplyChannel())
                .replyContact(un.getReplyContact())
                .userId(un.getUser() != null ? un.getUser().getId() : null)
                .isRead(Boolean.TRUE.equals(un.getIsRead()))
                .createdAt(un.getCreatedAt())
                .build();
    }

    private String buildWhatsAppReply(String senderName, String replyMessage) {
        return String.join("\n",
            "╔═════════════════════╗",
            "      ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "Bonjour *" + senderName + "* 👋 \n\n",
            "L'équipe smart-invite vous a répondu:\n\n",
            "",
            replyMessage,
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "🌐 smart-invite.com"
        );
    }

    private User resolve(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
