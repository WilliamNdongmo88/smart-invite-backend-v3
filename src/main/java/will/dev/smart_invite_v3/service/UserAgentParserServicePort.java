package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.service.impl.UserAgentParserService.ParsedUA;

public interface UserAgentParserServicePort {
    ParsedUA parse(String userAgent);
}
