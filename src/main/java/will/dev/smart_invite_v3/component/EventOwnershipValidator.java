package will.dev.smart_invite_v3.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;

/**
 * Composant partagé pour vérifier qu'un événement appartient bien à l'organisateur donné.
 *
 * Élimine la duplication de la méthode {@code resolveOwned(eventId, organizerId)}
 * qui était copiée-collée dans EventServiceImpl, GuestServiceImpl,
 * InvitationServiceImpl et InvitationCardServiceImpl (audit BE-F2).
 */
@Component
@RequiredArgsConstructor
public class EventOwnershipValidator {

    private final EventRepository eventRepository;

    /**
     * Récupère l'événement identifié par {@code eventId} et vérifie qu'il appartient
     * à l'organisateur {@code organizerId}.
     *
     * @param eventId    identifiant de l'événement
     * @param organizerId identifiant de l'utilisateur organisateur attendu
     * @return l'entité {@link Event} si elle existe et appartient à l'organisateur
     * @throws EventNotFoundException     si l'événement n'existe pas
     * @throws EventAccessDeniedException si l'organisateur ne correspond pas
     */
    public Event resolveOwned(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new EventAccessDeniedException();
        }
        return event;
    }
}
