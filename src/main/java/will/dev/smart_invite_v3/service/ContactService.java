package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.contact.ContactRequest;
import will.dev.smart_invite_v3.dto.contact.ContactResponse;

public interface ContactService {

    /**
     * Persiste le message de contact en base (table usernews)
     * et notifie l'admin via le canal adéquat (WhatsApp ou Email).
     *
     * @param request corps du formulaire de contact
     * @param userId  identifiant de l'utilisateur connecté (null si anonyme)
     * @return résumé de l'enregistrement
     */
    ContactResponse submitContact(ContactRequest request, Long userId);
}
