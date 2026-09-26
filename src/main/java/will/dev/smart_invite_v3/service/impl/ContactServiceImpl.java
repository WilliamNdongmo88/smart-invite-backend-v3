package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.contact.ContactRequest;
import will.dev.smart_invite_v3.dto.contact.ContactResponse;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.entity.UserNews;
import will.dev.smart_invite_v3.repository.UserNewsRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.ContactService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final UserNewsRepository       userNewsRepository;
    private final UserRepository           userRepository;
    private final ContactNotificationSender notificationSender;

    @Override
    @Transactional
    public ContactResponse submitContact(ContactRequest request, Long userId) {

        // 1 ── Résoudre l'utilisateur connecté (peut être null)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // 2 ── Construire et persister l'enregistrement dans usernews
        UserNews entry = UserNews.builder()
                .name(request.getName())
                .message(request.getMessage())
                .replyChannel(request.getReplyChannel())
                .replyContact(request.getReplyContact())
                .newsletter(false)
                .user(user)
                .build();

        if ("EMAIL".equalsIgnoreCase(request.getReplyChannel())) {
            entry.setEmail(request.getReplyContact());
        } else {
            entry.setPhone(request.getReplyContact());
        }

        userNewsRepository.save(entry);
        log.info("[Contact] Message enregistré (id={}, canal={}, de={})",
                entry.getId(), entry.getReplyChannel(), entry.getName());

        // 3 ── Notifier l'admin en arrière-plan (non bloquant)
        //      L'utilisateur reçoit sa réponse immédiatement sans attendre WhatsApp/Email
        notificationSender.notifyAdmin(
                request.getName(),
                request.getReplyChannel(),
                request.getReplyContact(),
                request.getMessage()
        );

        // 4 ── Réponse
        return ContactResponse.builder()
                .id(entry.getId())
                .name(entry.getName())
                .replyChannel(entry.getReplyChannel())
                .replyContact(entry.getReplyContact())
                .createdAt(entry.getCreatedAt())
                .message(entry.getMessage())
                .build();
    }
}
