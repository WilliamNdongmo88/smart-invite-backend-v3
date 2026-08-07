package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.admin.EventSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.AdminService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository    userRepository;
    private final EventRepository   eventRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<OrganizerSummaryResponse> getAllOrganizers() {
        List<User> organizers = userRepository.findAllByRoleOrderByCreatedAtDesc(UserRole.USER);

        List<Long> organizerIds = organizers.stream().map(User::getId).toList();

        // Récupérer tous les events des organisateurs
        List<Event> allEvents = organizerIds.stream()
                .flatMap(id -> eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(id).stream())
                .toList();

        List<Long> eventIds = allEvents.stream().map(Event::getId).toList();

        // Récupérer tous les paiements liés à ces events
        Map<Long, PaymentStatus> paymentByEvent = paymentRepository.findAllByEventIdIn(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getEvent().getId(),
                        Payment::getStatus,
                        (a, b) -> a // garder le premier si plusieurs
                ));

        // Grouper les events par organisateur
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

    private User resolve(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}
