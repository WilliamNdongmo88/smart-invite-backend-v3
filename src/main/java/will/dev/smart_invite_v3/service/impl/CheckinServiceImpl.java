package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.checkin.request.CreateAgentRequest;
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;
import will.dev.smart_invite_v3.entity.Checkin;
import will.dev.smart_invite_v3.entity.CheckinAgent;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.InvitationStatus;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.enums.ScanResult;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.CheckinAgentRepository;
import will.dev.smart_invite_v3.repository.CheckinRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.CheckinService;
import will.dev.smart_invite_v3.service.WhatsAppService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final UserRepository         userRepository;
    private final CheckinAgentRepository  agentRepository;
    private final InvitationRepository    invitationRepository;
    private final GuestRepository         guestRepository;
    private final CheckinRepository       checkinRepository;
    private final PasswordEncoder         passwordEncoder;
    private final WhatsAppService         whatsAppService;

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Organisateur introuvable"));

        // Le mot de passe = numéro sans le "+"
        String rawPassword = request.whatsapp().replaceAll("\\+", "");

        // Email fictif unique basé sur le userName
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

        CheckinAgent agent = CheckinAgent.builder()
                .user(savedUser)
                .organizer(organizer)
                .build();

        agentRepository.save(agent);

        // Envoi des identifiants par WhatsApp
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
        // Vérifier que l'agent existe
        CheckinAgent agent = agentRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new UserNotFoundException("Agent introuvable"));

        // Chercher l'invitation par token
        Invitation invitation = invitationRepository.findByToken(token).orElse(null);

        if (invitation == null) {
            return new ScanResponse(ScanResult.INVALID, null, null, null, "QR Code invalide");
        }

        // Vérifier que l'invitation appartient à un événement de l'organisateur de l'agent
        Long eventOrganizerId = invitation.getEvent().getOrganizer().getId();
        if (!eventOrganizerId.equals(agent.getOrganizer().getId())) {
            return new ScanResponse(ScanResult.INVALID, null, null, null, "QR Code invalide pour cet événement");
        }

        String guestName    = invitation.getGuest().getFullName();
        String eventTitle   = invitation.getEvent().getTitle();
        Integer tableNumber = invitation.getGuest().getTableNumber();

        // Déjà un checkin VALID → DUPLICATE
        boolean alreadyCheckedIn = checkinRepository
                .existsByInvitationIdAndScanStatus(invitation.getId(), ScanResult.VALID);
        if (alreadyCheckedIn) {
            return new ScanResponse(ScanResult.DUPLICATE, guestName, eventTitle, tableNumber, "Invité déjà enregistré");
        }

        // REVOKED ou USED → EXPIRED
        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            return new ScanResponse(ScanResult.EXPIRED, guestName, eventTitle, tableNumber, "Invitation expirée ou révoquée");
        }

        // VALID — mise à jour statut
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

        log.info("[Checkin] Invité {} validé pour l'événement {}", guestName, eventTitle);

        return new ScanResponse(ScanResult.VALID, guestName, eventTitle, tableNumber, "Entrée validée");
    }
}
