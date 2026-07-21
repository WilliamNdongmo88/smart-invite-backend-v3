package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventRequest;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Gestion des événements")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Créer un événement")
    public ResponseEntity<ApiResponse<EventResponse>> create(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EventResponse response = eventService.create(request, userDetails.getUser().getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Événement créé avec succès"));
    }

    @GetMapping
    @Operation(summary = "Lister mes événements")
    public ResponseEntity<ApiResponse<List<EventResponse>>> findAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<EventResponse> events = eventService.findAllByOrganizer(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(events, "Événements récupérés"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un événement")
    public ResponseEntity<ApiResponse<EventResponse>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EventResponse response = eventService.findById(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Événement récupéré"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un événement")
    public ResponseEntity<ApiResponse<EventResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EventResponse response = eventService.update(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Événement mis à jour"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un événement")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        eventService.delete(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Événement supprimé"));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Statistiques d'un événement")
    public ResponseEntity<ApiResponse<EventStatsResponse>> getStats(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EventStatsResponse stats = eventService.getStats(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));
    }
}
