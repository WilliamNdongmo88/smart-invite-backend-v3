package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.checkin.request.CreateAgentRequest;
import will.dev.smart_invite_v3.dto.checkin.request.UpdateSoundRequest;
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.CheckinParametersResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;

import java.util.List;

public interface CheckinService {

    AgentResponse createAgent(CreateAgentRequest request, Long organizerId);

    List<AgentResponse> getAgents(Long organizerId);

    void deleteAgent(Long agentId, Long organizerId);

    ScanResponse scan(String token, Long agentUserId);

    CheckinParametersResponse getParameters(Long eventId);

    CheckinParametersResponse updateSound(Long eventId, UpdateSoundRequest request);

    CheckinParametersResponse getStats(Long agentUserId);
}
