package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.dto.ReturnAssessmentCreateDTO;
import ro.atemustard.labrent.dto.ReturnAssessmentDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnAssessmentService {

    private final ReturnAssessmentRepository returnAssessmentRepository;
    private final RentalRequestService rentalRequestService;
    private final UserService userService;
    private final EquipmentService equipmentService;
    private final NotificationService notificationService;

    public ReturnAssessmentService(ReturnAssessmentRepository returnAssessmentRepository,
                                    RentalRequestService rentalRequestService,
                                    UserService userService,
                                    EquipmentService equipmentService,
                                    NotificationService notificationService) {
        this.returnAssessmentRepository = returnAssessmentRepository;
        this.rentalRequestService = rentalRequestService;
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.notificationService = notificationService;
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

        assessment = returnAssessmentRepository.save(assessment);

        // Update user reputation
        userService.updateReputationScore(request.getUser().getId(), rating.getReputationImpact());

        // Release equipment unit back to available
        equipmentService.releaseUnit(request.getEquipment().getId());

        // Notify
        notificationService.notifyAssessmentCompleted(assessment);

        return ReturnAssessmentDTO.fromEntity(assessment);
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
