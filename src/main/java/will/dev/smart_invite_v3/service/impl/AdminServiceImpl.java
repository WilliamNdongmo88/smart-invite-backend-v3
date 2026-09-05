package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.admin.ContactReplyRequest;
import will.dev.smart_invite_v3.dto.admin.EventSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.UserNewsResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.entity.UserNews;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.repository.UserNewsRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.AdminService;
import will.dev.smart_invite_v3.service.EmailService;
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
    private final UserNewsRepository userNewsRepository;
    private final WhatsAppService    whatsAppService;
    private final EmailService       emailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ──────────────────────────────────────────────────────────────────
    // Organisateurs
    // ──────────────────────────────────────────────────────────────────

    @Override
    public List<OrganizerSummaryResponse> getAllOrganizers() {
        List<User> organizers = userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.USER);

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
                    user.getCreatedAt(),
                    events
            );
        }).toList();
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
