package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.CheckinAgent;

import java.util.Optional;

public interface CheckinAgentRepository extends JpaRepository<CheckinAgent, Long> {

    Optional<CheckinAgent> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
