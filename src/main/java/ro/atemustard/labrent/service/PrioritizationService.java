package ro.atemustard.labrent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;
import ro.atemustard.labrent.model.UserType;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.prioritization.PrioritizationContext;
import ro.atemustard.labrent.service.prioritization.PrioritizationStrategy;

import java.util.List;
import java.util.Map;

/**
 * Service care coordonează prioritizarea cererilor.
 *
 * Folosește Strategy pattern: deține o referință la strategia activă și o poate
 * schimba la runtime (admin poate comuta între "weightedScoring" și "fifo").
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

    public List<RentalRequest> getPrioritizedQueue(Long equipmentId) {
        return rentalRequestRepository.findByEquipmentIdAndStatusOrderByPriorityScoreDesc(
                equipmentId, RequestStatus.PENDING);
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
