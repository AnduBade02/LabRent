package ro.atemustard.labrent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;
import ro.atemustard.labrent.model.UserType;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.prioritization.PrioritizationContext;
import ro.atemustard.labrent.service.prioritization.PrioritizationStrategy;

import java.util.List;
import java.util.Map;

/**
 * Service that coordinates rental request prioritization.
 *
 * Uses the Strategy pattern: holds a reference to the active strategy and can
 * switch it at runtime (admin can toggle between "weightedScoring" and "fifo").
 * Also exposes recalculation hooks so priority scores stay fresh when the
 * strategy changes, when competition for an equipment changes, or when a
 * user's reputation changes.
 */
@Service
public class PrioritizationService {

    private final Map<String, PrioritizationStrategy> strategies;
    private final RentalRequestRepository rentalRequestRepository;
    private String activeStrategyName;

    public PrioritizationService(
            Map<String, PrioritizationStrategy> strategies,
            RentalRequestRepository rentalRequestRepository,
            @Value("${app.prioritization.strategy:weightedScoring}") String activeStrategyName) {
        this.strategies = strategies;
        this.rentalRequestRepository = rentalRequestRepository;
        this.activeStrategyName = activeStrategyName;
    }

    public double calculatePriority(RentalRequest request) {
        PrioritizationContext context = buildContext(request);
        PrioritizationStrategy strategy = strategies.get(activeStrategyName);
        if (strategy == null) {
            throw new IllegalStateException("Unknown prioritization strategy: " + activeStrategyName);
        }
        return strategy.calculatePriority(request, context);
    }

    public String getActiveStrategyName() {
        return activeStrategyName;
    }

    public void setActiveStrategyName(String strategyName) {
        if (!strategies.containsKey(strategyName)) {
            throw new IllegalArgumentException("Unknown strategy: " + strategyName
                    + ". Available: " + strategies.keySet());
        }
        this.activeStrategyName = strategyName;
    }

    /**
     * Returns the PENDING queue for an equipment, ordered per the active
     * strategy: weighted → highest priorityScore first; FIFO → oldest
     * createdAt first. The priority scores themselves remain identical under
     * both strategies — only the order differs.
     */
    public List<RentalRequest> getPrioritizedQueue(Long equipmentId) {
        if ("fifo".equals(activeStrategyName)) {
            return rentalRequestRepository.findByEquipmentIdAndStatusOrderByCreatedAtAsc(
                    equipmentId, RequestStatus.PENDING);
        }
        return rentalRequestRepository.findByEquipmentIdAndStatusOrderByPriorityScoreDescCreatedAtAsc(
                equipmentId, RequestStatus.PENDING);
    }

    @Transactional
    public void recalculateAllPending() {
        List<RentalRequest> pending = rentalRequestRepository.findByStatus(RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r));
        }
        rentalRequestRepository.saveAll(pending);
    }

    @Transactional
    public void recalculateForEquipment(Long equipmentId) {
        List<RentalRequest> pending = rentalRequestRepository
                .findByEquipmentIdAndStatus(equipmentId, RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r));
        }
        rentalRequestRepository.saveAll(pending);
    }

    @Transactional
    public void recalculateForUser(Long userId) {
        List<RentalRequest> pending = rentalRequestRepository
                .findByUserIdAndStatus(userId, RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r));
        }
        rentalRequestRepository.saveAll(pending);
    }

    private PrioritizationContext buildContext(RentalRequest request) {
        List<RequestStatus> activeStatuses = List.of(
                RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.RENTED);

        int activeRequestCount = (int) rentalRequestRepository.countByUserIdAndStatusIn(
                request.getUser().getId(), activeStatuses);

        int competingRequestCount = rentalRequestRepository
                .findByEquipmentIdAndStatus(request.getEquipment().getId(), RequestStatus.PENDING)
                .size();

        double reputationScore = request.getUser().getReputationScore();
        boolean isStudent = request.getUser().getUserType() == UserType.STUDENT;

        return new PrioritizationContext(
                activeRequestCount,
                competingRequestCount,
                reputationScore,
                isStudent,
                request.getExamDate()
        );
    }
}
