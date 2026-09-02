package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.checkin.request.CreateAgentRequest;
import will.dev.smart_invite_v3.dto.checkin.request.UpdateSoundRequest;
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.CheckinParametersResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;
import will.dev.smart_invite_v3.entity.Checkin;
import will.dev.smart_invite_v3.entity.CheckinAgent;
import will.dev.smart_invite_v3.entity.CheckinParameters;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.InvitationStatus;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.enums.ScanResult;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.CheckinAgentRepository;
import will.dev.smart_invite_v3.repository.CheckinParametersRepository;
import will.dev.smart_invite_v3.repository.CheckinRepository;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.CheckinService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckinServiceImpl implements CheckinService {

    private final UserRepository              userRepository;
    private final CheckinAgentRepository      agentRepository;
    private final InvitationRepository        invitationRepository;
    private final GuestRepository             guestRepository;
    private final CheckinRepository           checkinRepository;
    private final CheckinParametersRepository parametersRepository;
    private final EventRepository             eventRepository;
    private final PasswordEncoder             passwordEncoder;
    private final WhatsAppService             whatsAppService;

    @Override
    public List<AgentResponse> getAgents(Long organizerId) {
        return agentRepository.findAllByOrganizerId(organizerId).stream()
                .map(a -> new AgentResponse(a.getUser().getId(), a.getUser().getName(), a.getUser().getPhone()))
                .toList();
    }

    @Override
    @Transactional
    public void deleteAgent(Long agentId, Long organizerId) {
        CheckinAgent agent = agentRepository.findByUserId(agentId)
                .orElseThrow(() -> new UserNotFoundException("Agent introuvable"));
        if (!agent.getOrganizer().getId().equals(organizerId)) {
            throw new RuntimeException("Cet agent ne vous appartient pas");
        }
        agentRepository.delete(agent);
        userRepository.deleteById(agentId);
    }

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Organisateur introuvable"));

        String rawPassword = request.whatsapp().replaceAll("\\+", "");
        String email = request.userName().toLowerCase() + "@agent.smartinvite.local";

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un agent avec ce nom d'utilisateur existe déjà");
        }

        User agentUser = User.builder()
                .name(request.userName())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .phone(request.whatsapp())
                .role(UserRole.AGENT)
                .notificationMode(NotificationMode.WHATSAPP)
                .isActive(true)
                .isBlocked(false)
                .build();

        User savedUser = userRepository.save(agentUser);
        agentRepository.save(CheckinAgent.builder().user(savedUser).organizer(organizer).build());

        try {
            whatsAppService.sendAgentCredentialsMessage(request.whatsapp(), email, rawPassword);
        } catch (Exception e) {
            log.warn("[Checkin] Échec envoi identifiants WhatsApp à {} : {}", request.whatsapp(), e.getMessage());
        }

        return new AgentResponse(savedUser.getId(), request.userName(), request.whatsapp());
    }

    @Override
    @Transactional
    public ScanResponse scan(String token, Long agentUserId) {
        CheckinAgent agent = agentRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new UserNotFoundException("Agent introuvable"));

        Invitation invitation = invitationRepository.findByToken(token).orElse(null);

        if (invitation == null) {
            return new ScanResponse(ScanResult.INVALID, null, null, null, "QR Code invalide", null);
        }

        Long eventOrganizerId = invitation.getEvent().getOrganizer().getId();
        if (!eventOrganizerId.equals(agent.getOrganizer().getId())) {
            return new ScanResponse(ScanResult.INVALID, null, null, null, "QR Code invalide pour cet événement", null);
        }

        String  guestName   = invitation.getGuest().getFullName();
        String  eventTitle  = invitation.getEvent().getTitle();
        Integer tableNumber = invitation.getGuest().getTableNumber();
        Long    eventId     = invitation.getEvent().getId();

        boolean alreadyCheckedIn = checkinRepository
                .existsByInvitationIdAndScanStatus(invitation.getId(), ScanResult.VALID);
        if (alreadyCheckedIn) {
            updateCounters(eventId, ScanResult.DUPLICATE);
            return new ScanResponse(ScanResult.DUPLICATE, guestName, eventTitle, tableNumber, "Invité déjà enregistré", eventId);
        }

        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            updateCounters(eventId, ScanResult.EXPIRED);
            return new ScanResponse(ScanResult.EXPIRED, guestName, eventTitle, tableNumber, "Invitation expirée ou révoquée", eventId);
        }

        invitation.getGuest().setRsvpStatus(RsvpStatus.PRESENT);
        invitation.setStatus(InvitationStatus.USED);
        guestRepository.save(invitation.getGuest());
        invitationRepository.save(invitation);

        checkinRepository.save(Checkin.builder()
                .event(invitation.getEvent())
                .guest(invitation.getGuest())
                .invitation(invitation)
                .scannedBy(agent.getUser())
                .scanStatus(ScanResult.VALID)
                .checkinTime(java.time.LocalDateTime.now())
                .build());

        updateCounters(eventId, ScanResult.VALID);
        log.info("[Checkin] Invité {} validé pour l'événement {}", guestName, eventTitle);

        return new ScanResponse(ScanResult.VALID, guestName, eventTitle, tableNumber, "Entrée validée", eventId);
    }

    private void updateCounters(Long eventId, ScanResult result) {
        CheckinParameters params = parametersRepository.findByEventId(eventId)
                .orElseGet(() -> CheckinParameters.builder()
                        .event(eventRepository.getReferenceById(eventId))
                        .build());
        params.setTotalScans(params.getTotalScans() + 1);
        if (result == ScanResult.VALID) {
            params.setValidScans(params.getValidScans() + 1);
        } else if (result == ScanResult.DUPLICATE) {
            params.setDuplicateScans(params.getDuplicateScans() + 1);
        } else {
            params.setInvalidScans(params.getInvalidScans() + 1);
        }
        parametersRepository.save(params);
    }

    @Override
    public CheckinParametersResponse getParameters(Long eventId) {
        return CheckinParametersResponse.from(
                parametersRepository.findByEventId(eventId)
                        .orElseGet(() -> CheckinParameters.builder()
                                .event(eventRepository.findById(eventId)
                                        .orElseThrow(() -> new RuntimeException("Événement introuvable")))
                                .build())
        );
    }

    @Override
    @Transactional
    public CheckinParametersResponse updateSound(Long eventId, UpdateSoundRequest request) {
        CheckinParameters params = parametersRepository.findByEventId(eventId)
                .orElseGet(() -> CheckinParameters.builder()
                        .event(eventRepository.findById(eventId)
                                .orElseThrow(() -> new RuntimeException("Événement introuvable")))
                        .build());
        params.setConfirmationSound(request.confirmationSound());
        return CheckinParametersResponse.from(parametersRepository.save(params));
    }

    @Override
    public CheckinParametersResponse getStats(Long agentUserId, Long eventId) {
        CheckinAgent agent = agentRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new UserNotFoundException("Agent introuvable"));

        // Si un eventId précis est fourni, retourner uniquement les stats de cet événement
        if (eventId != null && eventId > 0) {
            return getParameters(eventId);
        }

        // Sinon agréger tous les événements de l'organisateur
        Long organizerId = agent.getOrganizer().getId();
        java.util.List<CheckinParameters> all = parametersRepository.findAllByEventOrganizerId(organizerId);
        int total = 0, valid = 0, duplicate = 0, invalid = 0;
        boolean sound = true;
        for (CheckinParameters p : all) {
            total     += p.getTotalScans()     != null ? p.getTotalScans()     : 0;
            valid     += p.getValidScans()     != null ? p.getValidScans()     : 0;
            duplicate += p.getDuplicateScans() != null ? p.getDuplicateScans() : 0;
            invalid   += p.getInvalidScans()   != null ? p.getInvalidScans()   : 0;
            sound = Boolean.TRUE.equals(p.getConfirmationSound());
        }
        return new CheckinParametersResponse(0L, sound, total, valid, duplicate, invalid);
    }

    @Override
    public java.util.List<will.dev.smart_invite_v3.dto.checkin.response.EventSummaryResponse> getEventsByAgent(Long agentUserId) {
        CheckinAgent agent = agentRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new UserNotFoundException("Agent introuvable"));
        return eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(agent.getOrganizer().getId())
                .stream()
                .map(will.dev.smart_invite_v3.dto.checkin.response.EventSummaryResponse::from)
                .toList();
    }
}
