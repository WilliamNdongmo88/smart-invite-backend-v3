package will.dev.smart_invite_v3.service;

import java.time.LocalDateTime;

public interface EventScheduleServicePort {
    void schedule(Long eventId, LocalDateTime eventDate, int numberOfDays);
    void processPendingJobs();
}
