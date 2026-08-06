package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.checkin.request.CreateAgentRequest;
import will.dev.smart_invite_v3.dto.checkin.response.AgentResponse;
import will.dev.smart_invite_v3.dto.checkin.response.ScanResponse;

public interface CheckinService {

    AgentResponse createAgent(CreateAgentRequest request, Long organizerId);

    ScanResponse scan(String token, Long agentUserId);
}
