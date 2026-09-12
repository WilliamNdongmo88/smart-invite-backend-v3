package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Visitor;

import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    /** Retrouve un visiteur existant par IP + device (empreinte légère) */
    Optional<Visitor> findByIpAddressAndDevice(String ipAddress, String device);
}
