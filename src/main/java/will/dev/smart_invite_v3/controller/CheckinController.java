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
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.service.CheckinService;

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
}
