package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    Optional<Event> findByIdAndOrganizerId(Long id, Long organizerId);

    long countByOrganizerId(Long organizerId);
}
