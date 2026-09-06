package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Checkin;
import will.dev.smart_invite_v3.enums.ScanResult;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    boolean existsByInvitationIdAndScanStatus(Long invitationId, ScanResult scanStatus);
}
