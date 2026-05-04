package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.dto.ReturnAssessmentCreateDTO;
import ro.atemustard.labrent.dto.ReturnAssessmentDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnAssessmentService {

    private static final double MAX_OVERDUE_PENALTY = 10.0;

    private final ReturnAssessmentRepository returnAssessmentRepository;
    private final RentalRequestService rentalRequestService;
    private final UserService userService;
    private final EquipmentService equipmentService;
    private final NotificationService notificationService;
    private final PrioritizationService prioritizationService;

    public ReturnAssessmentService(ReturnAssessmentRepository returnAssessmentRepository,
                                    RentalRequestService rentalRequestService,
                                    UserService userService,
                                    EquipmentService equipmentService,
                                    NotificationService notificationService,
                                    PrioritizationService prioritizationService) {
        this.returnAssessmentRepository = returnAssessmentRepository;
        this.rentalRequestService = rentalRequestService;
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.notificationService = notificationService;
        this.prioritizationService = prioritizationService;
    }

    @Transactional
    public ReturnAssessmentDTO submitAssessment(ReturnAssessmentCreateDTO dto, String operatorUsername) {
        RentalRequest request = rentalRequestService.findEntityById(dto.getRentalRequestId());

        if (request.getStatus() != RequestStatus.RETURNED) {
            throw new InvalidOperationException("Can only assess requests with RETURNED status");
        }

        if (returnAssessmentRepository.findByRentalRequestId(request.getId()).isPresent()) {
            throw new InvalidOperationException("Assessment already exists for this request");
        }

        User operator = userService.findEntityByUsername(operatorUsername);
        ConditionRating rating = ConditionRating.valueOf(dto.getConditionRating().toUpperCase());

        ReturnAssessment assessment = ReturnAssessment.builder()
                .rentalRequest(request)
                .operator(operator)
                .conditionRating(rating)
                .notes(dto.getNotes())
                .build();
        double totalImpact = rating.getReputationImpact() + calculateOverduePenalty(request);
        assessment.setReputationImpact(totalImpact);

        assessment = returnAssessmentRepository.save(assessment);

        // Mark request as fully completed so it no longer shows up in Manage Requests
        request.setStatus(RequestStatus.COMPLETED);

        // Update user reputation (rating impact + overdue penalty, if any)
        userService.updateReputationScore(request.getUser().getId(), totalImpact);

        // Reputation changed — rescore this user's PENDING requests so future
        // prioritization reflects the updated trust score.
        prioritizationService.recalculateForUser(request.getUser().getId());

        // Release equipment unit back to available
        equipmentService.releaseUnit(request.getEquipment().getId());

        // Notify
        notificationService.notifyAssessmentCompleted(assessment);

        return ReturnAssessmentDTO.fromEntity(assessment);
    }

    private double calculateOverduePenalty(RentalRequest request) {
        if (request.getReturnedAt() == null || request.getEndDate() == null) {
            return 0.0;
        }
        if (!request.getReturnedAt().isAfter(request.getEndDate())) {
            return 0.0;
        }
        long daysLate = ChronoUnit.DAYS.between(request.getEndDate(), request.getReturnedAt());
        return -Math.min(MAX_OVERDUE_PENALTY, daysLate);
    }

    public ReturnAssessmentDTO getAssessmentByRequestId(Long requestId) {
        ReturnAssessment assessment = returnAssessmentRepository.findByRentalRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnAssessment", "rentalRequestId", requestId));
        return ReturnAssessmentDTO.fromEntity(assessment);
    }

    public List<ReturnAssessmentDTO> getUserAssessmentHistory(Long userId) {
        return returnAssessmentRepository.findByRentalRequestUserId(userId).stream()
                .map(ReturnAssessmentDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
