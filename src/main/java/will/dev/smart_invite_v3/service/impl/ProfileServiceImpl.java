package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.profile.request.ChangePasswordRequest;
import will.dev.smart_invite_v3.dto.profile.request.UpdateProfileRequest;
import will.dev.smart_invite_v3.dto.profile.response.ProfileResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.ProfileService;
import will.dev.smart_invite_v3.service.RefreshTokenService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository        userRepository;
    private final EventRepository       eventRepository;
    private final InvitationRepository  invitationRepository;
    private final FirebaseStorageService firebaseStorage;
    private final PasswordEncoder       passwordEncoder;
    private final RefreshTokenService   refreshTokenService;

    @Value("${app.firebase.storage-bucket}")
    private String firebaseBucket;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(findUser(userId));
    }

    @Override
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.name()                  != null) user.setName(request.name());
        if (request.phone()                 != null) user.setPhone(request.phone());
        if (request.notificationMode()      != null) user.setNotificationMode(request.notificationMode());
        if (request.attendanceNotifications() != null) user.setAttendanceNotifications(request.attendanceNotifications());
        if (request.thankNotifications()    != null) user.setThankNotifications(request.thankNotifications());
        if (request.eventReminders()        != null) user.setEventReminders(request.eventReminders());
        if (request.marketingEmails()       != null) user.setMarketingEmails(request.marketingEmails());
        if (request.notifyMe()              != null) user.setNotifyMe(request.notifyMe());
        return ProfileResponse.from(userRepository.save(user));
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = findUser(userId);

        // Supprimer l'ancien avatar Firebase si existant
        if (user.getAvatarUrl() != null) {
            String base = "https://storage.googleapis.com/" + firebaseBucket + "/";
            firebaseStorage.delete(user.getAvatarUrl().replace(base, ""));
        }

        String url = firebaseStorage.upload(file, activeProfile + "/avatars");
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteAccount(Long userId) {
        User user = findUser(userId);

        // Supprimer les fichiers Firebase liés aux invitations
        List<Event> events = eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(userId);
        String base = "https://storage.googleapis.com/" + firebaseBucket + "/";
        for (Event event : events) {
            List<Invitation> invitations = invitationRepository.findAllByGuestEventId(event.getId());
            for (Invitation inv : invitations) {
                if (inv.getQrCodeUrl() != null) firebaseStorage.delete(inv.getQrCodeUrl().replace(base, ""));
                if (inv.getPdfUrl()    != null) firebaseStorage.delete(inv.getPdfUrl().replace(base, ""));
            }
        }

        // Supprimer l'avatar
        if (user.getAvatarUrl() != null) {
            firebaseStorage.delete(user.getAvatarUrl().replace(base, ""));
        }

        // Invalider le refresh token
        refreshTokenService.delete(userId);

        // Suppression explicite des events (pas de cascade JPA sur organizer)
        eventRepository.deleteAll(events);

        // Suppression du compte
        userRepository.delete(user);
        log.info("[Profile] Compte supprimé : userId={}", userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));
    }
}
