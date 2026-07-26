package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.invitation.request.BulkGenerateRequest;
import will.dev.smart_invite_v3.dto.invitation.request.CreateGuestRequest;
import will.dev.smart_invite_v3.dto.invitation.request.RsvpRequest;
import will.dev.smart_invite_v3.dto.invitation.response.BulkGenerateResponse;
import will.dev.smart_invite_v3.dto.invitation.response.GuestResponse;
import will.dev.smart_invite_v3.dto.invitation.response.InvitationResponse;
import will.dev.smart_invite_v3.dto.invitation.response.PublicInvitationResponse;

import java.util.List;

public interface InvitationService {

    /** US-020 — Créer un invité puis générer son invitation */
    InvitationResponse generate(Long eventId, CreateGuestRequest request, Long organizerId);

    /** US-021 — Génération en masse */
    BulkGenerateResponse bulkGenerate(BulkGenerateRequest request, Long organizerId);

    /** US-022 — Vue publique (sans auth) */
    PublicInvitationResponse getPublic(String token);

    /** US-024 — Suppression invitation */
    void delete(Long invitationId, Long organizerId);

    /** Liste des invitations d'un event */
    List<InvitationResponse> listByEvent(Long eventId, Long organizerId);

    /** RSVP invité */
    PublicInvitationResponse rsvp(String token, RsvpRequest request);

    /** Inscription via lien public — RSVP CONFIRMED immédiat, génère QR+PDF+email remerciement */
    InvitationResponse generateFromLink(Long eventId, CreateGuestRequest request, Long organizerId);
}
