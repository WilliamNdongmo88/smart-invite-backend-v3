package will.dev.smart_invite_v3.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.enums.RsvpStatus;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    List<Guest> findAllByEventId(Long eventId);

    @Query("SELECT g FROM Guest g WHERE g.event.id = :eventId " +
           "AND (:search IS NULL OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(g.email,'')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "AND (:rsvp IS NULL OR g.rsvpStatus = :rsvp)")
    Page<Guest> findByEventIdFiltered(@Param("eventId") Long eventId,
                                      @Param("search") String search,
                                      @Param("rsvp") RsvpStatus rsvp,
                                      Pageable pageable);

    boolean existsByEventIdAndEmail(Long eventId, String email);
    boolean existsByEventIdAndPhoneNumber(Long eventId, String phoneNumber);

    int countByEventId(Long eventId);

    List<Guest> findAllByEventIdAndRsvpStatus(Long eventId, RsvpStatus rsvpStatus);
}
