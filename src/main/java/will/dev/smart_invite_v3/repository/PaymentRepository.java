package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    Optional<Payment> findTopByEventIdAndOrganizerIdOrderByCreatedAtDesc(Long eventId, Long organizerId);

    boolean existsByEventIdAndOrganizerIdAndStatus(Long eventId, Long organizerId, PaymentStatus status);
}
