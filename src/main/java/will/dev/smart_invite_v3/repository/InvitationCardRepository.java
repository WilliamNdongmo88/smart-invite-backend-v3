package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.InvitationCard;

import java.util.Optional;

public interface InvitationCardRepository extends JpaRepository<InvitationCard, Long> {
    Optional<InvitationCard> findByEventId(Long eventId);
}
