package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.admin.ContactReplyRequest;
import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;
import will.dev.smart_invite_v3.dto.admin.UserNewsResponse;

import java.util.List;

public interface AdminService {

    List<OrganizerSummaryResponse> getAllOrganizers();

    void blockUser(Long userId);

    void unblockUser(Long userId);

    void activateUser(Long userId);

    void deleteUser(Long userId);

    /** Retourne tous les messages de contact reçus, du plus récent au plus ancien */
    List<UserNewsResponse> getAllContacts();

    /** Envoie une réponse à un message de contact via le canal choisi par l'utilisateur */
    void replyToContact(Long contactId, ContactReplyRequest request);
}
