package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.event.request.*;
import will.dev.smart_invite_v3.dto.event.response.*;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.EventService;
import will.dev.smart_invite_v3.service.InvitationCardService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Gestion des événements")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService          eventService;
    private final InvitationCardService cardService;

    // ---- CRUD Événements ----

    @PostMapping
    @Operation(summary = "Créer un événement")
    public ResponseEntity<ApiResponse<EventResponse>> create(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                eventService.create(request, userDetails.getUser().getId()),
                "Événement créé avec succès"));
    }

    @PostMapping("/with-card")
    @Operation(summary = "Créer un événement avec sa carte d'invitation simultanément")
    public ResponseEntity<ApiResponse<EventWithCardResponse>> createWithCard(
            @Valid @RequestBody CreateEventWithCardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                cardService.createWithCard(request, userDetails.getUser().getId()),
                "Événement et carte créés avec succès"));
    }

    @GetMapping
    @Operation(summary = "Lister mes événements")
    public ResponseEntity<ApiResponse<List<EventResponse>>> findAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.findAllByOrganizer(userDetails.getUser().getId()),
                "Événements récupérés"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un événement")
    public ResponseEntity<ApiResponse<EventResponse>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.findById(id, userDetails.getUser().getId()),
                "Événement récupéré"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un événement")
    public ResponseEntity<ApiResponse<EventResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.update(id, request, userDetails.getUser().getId()),
                "Événement mis à jour"));
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
        return ResponseEntity.ok(ApiResponse.success(
                eventService.getStats(id, userDetails.getUser().getId()),
                "Statistiques récupérées"));
    }

    // ---- Carte d'invitation ----

    @GetMapping("/{id}/card")
    @Operation(summary = "Récupérer l'événement et sa carte d'invitation")
    public ResponseEntity<ApiResponse<EventWithCardResponse>> getCard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getCard(id, userDetails.getUser().getId()),
                "Événement et carte récupérés"));
    }

    @PutMapping(value = "/{id}/card", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre à jour l'événement et sa carte d'invitation")
    public ResponseEntity<ApiResponse<EventWithCardResponse>> updateWithCard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventWithCardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.updateWithCard(id, request, userDetails.getUser().getId()),
                "Événement et carte mis à jour"));
    }

    @GetMapping("/{id}/thank-you-message")
    @Operation(summary = "Récupérer le template du message de remerciement",
               description = "Retourne les parties personnalisables du message. La salutation (Cher(e) *NomInvité*) est fixe et gérée par le système.")
    public ResponseEntity<ApiResponse<ThankYouTemplateResponse>> getThankYouTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.getThankYouTemplate(id, userDetails.getUser().getId()),
                "Template récupéré"));
    }

    @PatchMapping("/{id}/thank-you-message")
    @Operation(summary = "Personnaliser le message de remerciement",
               description = "Modifie les parties personnalisables du message envoyé à tous les invités présents après l'événement. Envoyer tous les champs à null pour revenir au message par défaut.")
    public ResponseEntity<ApiResponse<EventResponse>> updateThankYouMessage(
            @PathVariable Long id,
            @Valid @RequestBody ThankYouMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.updateThankYouMessage(id, request, userDetails.getUser().getId()),
                "Message de remerciement mis à jour"));
    }

    @GetMapping(value = "/{id}/card/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Télécharger la carte d'invitation en PDF")
    public ResponseEntity<byte[]> downloadCard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] pdf = cardService.generatePdf(id, userDetails.getUser().getId());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=invitation-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping(value = "/{id}/card/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer un modèle de carte PDF personnalisé")
    public ResponseEntity<ApiResponse<CardResponse>> uploadCustomCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.uploadCustomModel(id, file, userDetails.getUser().getId()),
                "Modèle importé avec succès"));
    }
}
