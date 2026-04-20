package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;

import java.util.List;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, Long> {

    List<RentalRequest> findByUserId(Long userId);

    List<RentalRequest> findByEquipmentId(Long equipmentId);

    List<RentalRequest> findByStatus(RequestStatus status);

    List<RentalRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

    List<RentalRequest> findByEquipmentIdAndStatus(Long equipmentId, RequestStatus status);

    long countByUserIdAndStatusIn(Long userId, List<RequestStatus> statuses);

    List<RentalRequest> findByStatusOrderByPriorityScoreDesc(RequestStatus status);

    List<RentalRequest> findByEquipmentIdAndStatusOrderByPriorityScoreDesc(Long equipmentId, RequestStatus status);
}
