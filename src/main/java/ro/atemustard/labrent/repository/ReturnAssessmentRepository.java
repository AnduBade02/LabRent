package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.atemustard.labrent.model.ReturnAssessment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReturnAssessmentRepository extends JpaRepository<ReturnAssessment, Long> {

    Optional<ReturnAssessment> findByRentalRequestId(Long rentalRequestId);

    List<ReturnAssessment> findByOperatorId(Long operatorId);

    List<ReturnAssessment> findByRentalRequestUserId(Long userId);

    /**
     * Seeder-only hook for historical demo data. See
     * RentalRequestRepository#overrideCreatedAt for rationale.
     */
    @Modifying
    @Query("UPDATE ReturnAssessment a SET a.assessedAt = :assessedAt WHERE a.id = :id")
    void overrideAssessedAt(@Param("id") Long id, @Param("assessedAt") LocalDateTime assessedAt);
}
