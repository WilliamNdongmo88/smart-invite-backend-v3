package will.dev.smart_invite_v3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import will.dev.smart_invite_v3.dto.auth.response.ApiResponse;
import will.dev.smart_invite_v3.dto.checkin.request.CreateAgentRequest;
import will.dev.smart_invite_v3.dto.checkin.request.UpdateSoundRequest;
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.CheckinParametersResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.CheckinService;

import java.util.List;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-in", description = "Gestion des agents d'accueil et scan QR Code")
@SecurityRequirement(name = "bearerAuth")
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/agents")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer un agent d'accueil")
    public ResponseEntity<ApiResponse<AgentResponse>> createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                checkinService.createAgent(request, userDetails.getUser().getId()),
                "Agent créé avec succès"));
    }

    @GetMapping("/agents")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lister les agents de l'organisateur")
    public ResponseEntity<ApiResponse<List<AgentResponse>>> getAgents(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.getAgents(userDetails.getUser().getId()), "Agents récupérés"));
    }

    @DeleteMapping("/agents/{agentId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Supprimer un agent")
    public ResponseEntity<ApiResponse<Void>> deleteAgent(
            @PathVariable Long agentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        checkinService.deleteAgent(agentId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Agent supprimé"));
    }

    @PostMapping("/scan/{token}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Scanner un QR Code à l'entrée")
    public ResponseEntity<ApiResponse<ScanResponse>> scan(
            @PathVariable String token,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.scan(token, userDetails.getUser().getId()),
                "Scan effectué"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Stats de scan pour l'agent connecté — filtrées par événement si eventId fourni")
    public ResponseEntity<ApiResponse<CheckinParametersResponse>> getStats(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.getStats(userDetails.getUser().getId(), eventId), "Stats récupérées"));
    }

    @GetMapping("/parameters/{eventId}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Récupérer les paramètres de check-in d'un événement")
    public ResponseEntity<ApiResponse<CheckinParametersResponse>> getParameters(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.getParameters(eventId), "Paramètres récupérés"));
    }

    @PatchMapping("/parameters/{eventId}/sound")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Activer ou désactiver le son de confirmation")
    public ResponseEntity<ApiResponse<CheckinParametersResponse>> updateSound(
            @PathVariable Long eventId,
            @RequestBody UpdateSoundRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.updateSound(eventId, request), "Son mis à jour"));
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Événements de l'organisateur accessibles à l'agent")
    public ResponseEntity<ApiResponse<java.util.List<will.dev.smart_invite_v3.dto.checkin.response.EventSummaryResponse>>> getEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                checkinService.getEventsByAgent(userDetails.getUser().getId()), "Événements récupérés"));
    }
}
