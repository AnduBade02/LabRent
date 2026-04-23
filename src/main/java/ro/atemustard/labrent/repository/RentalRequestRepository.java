package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;

import java.time.LocalDateTime;
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

    /**
     * Seeder-only hook. {@code createdAt} is marked {@code updatable=false} on
     * the entity (it's set by {@code @PrePersist}); this query bypasses that
     * so DataSeeder can stamp historical timestamps for demo data.
     */
    @Modifying
    @Query("UPDATE RentalRequest r SET r.createdAt = :createdAt WHERE r.id = :id")
    void overrideCreatedAt(@Param("id") Long id, @Param("createdAt") LocalDateTime createdAt);
}
