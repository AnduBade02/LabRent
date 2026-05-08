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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        return calculatePriority(request, LocalDate.now());
    }

    public double calculatePriority(RentalRequest request, LocalDate scoringDate) {
        PrioritizationContext context = buildContext(request, scoringDate);
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
        recalculateAllPending(LocalDate.now());
    }

    @Transactional
    public void recalculateAllPending(LocalDate scoringDate) {
        List<RentalRequest> pending = rentalRequestRepository.findByStatus(RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r, scoringDate));
        }
        rentalRequestRepository.saveAll(pending);
    }

    @Transactional
    public void recalculateForEquipment(Long equipmentId) {
        recalculateForEquipment(equipmentId, LocalDate.now());
    }

    @Transactional
    public void recalculateForEquipment(Long equipmentId, LocalDate scoringDate) {
        List<RentalRequest> pending = rentalRequestRepository
                .findByEquipmentIdAndStatus(equipmentId, RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r, scoringDate));
        }
        rentalRequestRepository.saveAll(pending);
    }

    @Transactional
    public void recalculateForUser(Long userId) {
        recalculateForUser(userId, LocalDate.now());
    }

    @Transactional
    public void recalculateForUser(Long userId, LocalDate scoringDate) {
        List<RentalRequest> pending = rentalRequestRepository
                .findByUserIdAndStatus(userId, RequestStatus.PENDING);
        for (RentalRequest r : pending) {
            r.setPriorityScore(calculatePriority(r, scoringDate));
        }
        rentalRequestRepository.saveAll(pending);
    }

    private PrioritizationContext buildContext(RentalRequest request, LocalDate scoringDate) {
        List<RequestStatus> activeStatuses = List.of(
                RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.RENTED);

        int activeRequestCount = (int) rentalRequestRepository.countByUserIdAndStatusIn(
                request.getUser().getId(), activeStatuses);

        int competingRequestCount = rentalRequestRepository
                .findByEquipmentIdAndStatus(request.getEquipment().getId(), RequestStatus.PENDING)
                .size();

        int waitingDays = 0;
        if (request.getStatus() == RequestStatus.PENDING && request.getCreatedAt() != null) {
            waitingDays = (int) Math.max(0, ChronoUnit.DAYS.between(
                    request.getCreatedAt().toLocalDate(), scoringDate));
        }

        int previousRejectedSimilarCount = 0;
        String projectDescription = request.getProjectDescription();
        if (projectDescription != null && !projectDescription.isBlank()) {
            previousRejectedSimilarCount = (int) rentalRequestRepository
                    .countByUserIdAndEquipmentIdAndStatusAndProjectDescriptionIgnoreCase(
                            request.getUser().getId(),
                            request.getEquipment().getId(),
                            RequestStatus.REJECTED,
                            projectDescription.trim()
                    );
        }

        double reputationScore = request.getUser().getReputationScore();
        boolean isStudent = request.getUser().getUserType() == UserType.STUDENT;

        // Exam date only exists on the academic subclass; standard requests have none.
        java.time.LocalDate examDate = (request instanceof ro.atemustard.labrent.model.AcademicRentalRequest a)
                ? a.getExamDate() : null;

        return new PrioritizationContext(
                activeRequestCount,
                competingRequestCount,
                waitingDays,
                previousRejectedSimilarCount,
                reputationScore,
                isStudent,
                examDate,
                scoringDate
        );
    }
}
