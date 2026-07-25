package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Guest;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findAllByEventId(Long eventId);
    boolean existsByEventIdAndEmail(Long eventId, String email);
}
