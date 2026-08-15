package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.CheckinParameters;

import java.util.List;
import java.util.Optional;

public interface CheckinParametersRepository extends JpaRepository<CheckinParameters, Long> {
    Optional<CheckinParameters> findByEventId(Long eventId);
    List<CheckinParameters> findAllByEventOrganizerId(Long organizerId);
}
