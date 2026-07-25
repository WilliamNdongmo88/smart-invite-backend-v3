package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Invitation;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    Optional<Invitation> findByGuestId(Long guestId);
    List<Invitation> findAllByGuestEventId(Long eventId);
    boolean existsByGuestId(Long guestId);
}
