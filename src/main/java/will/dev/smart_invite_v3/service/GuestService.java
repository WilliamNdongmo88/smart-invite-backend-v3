package will.dev.smart_invite_v3.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import will.dev.smart_invite_v3.dto.guest.request.AddGuestRequest;
import will.dev.smart_invite_v3.dto.guest.request.BulkDeleteRequest;
import will.dev.smart_invite_v3.dto.guest.request.UpdateGuestRequest;
import will.dev.smart_invite_v3.dto.guest.response.GuestResponse;
import will.dev.smart_invite_v3.enums.RsvpStatus;

public interface GuestService {

    /** US-015 — Ajout d'un invité */
    GuestResponse add(Long eventId, AddGuestRequest request, Long organizerId);

    /** Liste paginée avec filtres */
    Page<GuestResponse> list(Long eventId, String search, RsvpStatus rsvp,
                             Pageable pageable, Long organizerId);

    /** US-017 — Modification */
    GuestResponse update(Long guestId, UpdateGuestRequest request, Long organizerId);

    /** US-018 — Suppression individuelle */
    void delete(Long guestId, Long organizerId);

    /** US-018 — Suppression multiple */
    void bulkDelete(BulkDeleteRequest request, Long organizerId);

    /** US-019 — Envoi rappel */
    void sendReminder(Long guestId, Long organizerId);
}
