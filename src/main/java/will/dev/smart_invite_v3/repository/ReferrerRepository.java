package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import will.dev.smart_invite_v3.entity.Referrer;
import will.dev.smart_invite_v3.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

public interface ReferrerRepository extends JpaRepository<Referrer, Long> {

    Optional<Referrer> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(u) FROM User u WHERE u.referralCode = :code")
    long countRegistrationsByCode(@Param("code") String code);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.referralCode = :code")
    long countEventsByCode(@Param("code") String code);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.referralCode = :code AND p.status = :status")
    BigDecimal sumApprovedAmountByCode(@Param("code") String code, @Param("status") PaymentStatus status);
}