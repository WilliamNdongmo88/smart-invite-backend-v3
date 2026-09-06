package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.EventSchedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    Optional<EventSchedule> findByEventId(Long eventId);

    List<EventSchedule> findAllByExecutedFalseAndScheduledForBefore(LocalDateTime now);
}
