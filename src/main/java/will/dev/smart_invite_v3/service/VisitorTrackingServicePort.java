package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.Visitor;

public interface VisitorTrackingServicePort {
    Visitor identifyVisitor(String ip, String userAgent);
    Long startSession(Visitor visitor);
    void trackPageView(Long sessionId, String pageUrl);
    void endSession(Long sessionId);
    boolean sessionExists(Long sessionId);
    void cleanupInactiveSessions();
}
