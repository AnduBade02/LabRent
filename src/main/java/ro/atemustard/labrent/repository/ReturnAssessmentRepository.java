package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.atemustard.labrent.model.ReturnAssessment;

import java.util.List;
import java.util.Optional;

public interface ReturnAssessmentRepository extends JpaRepository<ReturnAssessment, Long> {

    Optional<ReturnAssessment> findByRentalRequestId(Long rentalRequestId);

    List<ReturnAssessment> findByOperatorId(Long operatorId);

    List<ReturnAssessment> findByRentalRequestUserId(Long userId);
}
