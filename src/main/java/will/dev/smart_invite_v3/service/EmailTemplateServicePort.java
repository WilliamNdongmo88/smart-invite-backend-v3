package will.dev.smart_invite_v3.service;

import java.util.Map;

public interface EmailTemplateServicePort {
    String render(Map<String, Object> variables);
}
