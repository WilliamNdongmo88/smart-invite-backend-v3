package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;

/**
 * Endpoints publics (sans authentification) pour la consultation
 * d'événements en mode preview — utilisés par les invités ayant confirmé
 * leur présence via le lien envoyé dans la notification de confirmation.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public Events", description = "Consultation publique des événements (mode preview)")
public class PublicEventController {

    private final EventRepository eventRepository;

    /**
     * Retourne les données publiques d'un événement pour l'affichage
     * en mode preview (page mariage, conférence, gala, cérémonie).
     *
     * Accessible sans authentification — utilisé par les invités.
     * GET /api/events/{id}/public
     */
    @GetMapping("/api/events/{id}/public")
    @Operation(summary = "Vue publique d'un événement (sans auth) — mode preview invité")
    public ResponseEntity<ApiResponse<EventResponse>> getPublic(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return ResponseEntity.ok(ApiResponse.success(
                EventResponse.from(event),
                "Événement récupéré"));
    }
}
