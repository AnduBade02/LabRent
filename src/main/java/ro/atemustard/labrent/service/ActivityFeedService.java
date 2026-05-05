package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.dto.ActivityEventDTO;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;
import ro.atemustard.labrent.model.ReturnAssessment;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds a unified recent-activity feed from rental requests and return
 * assessments. The feed reflects each request's current status as a single
 * event (no separate audit table).
 */
@Service
public class ActivityFeedService {

    private final RentalRequestRepository rentalRequestRepository;
    private final ReturnAssessmentRepository returnAssessmentRepository;

    public ActivityFeedService(RentalRequestRepository rentalRequestRepository,
                               ReturnAssessmentRepository returnAssessmentRepository) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.returnAssessmentRepository = returnAssessmentRepository;
    }

    public List<ActivityEventDTO> getRecentEvents(int limit) {
        List<ActivityEventDTO> events = new ArrayList<>();

        for (RentalRequest req : rentalRequestRepository.findAll()) {
            events.add(buildRequestEvent(req));
        }

        for (ReturnAssessment assessment : returnAssessmentRepository.findAll()) {
            events.add(new ActivityEventDTO(
                    "ASSESSMENT_DONE",
                    String.format(Locale.ROOT, "Assessment submitted: %s (impact %+.1f)",
                            assessment.getConditionRating().name(),
                            assessment.getReputationImpact()),
                    assessment.getOperator().getUsername(),
                    assessment.getAssessedAt(),
                    assessment.getRentalRequest().getId()
            ));
        }

        events.sort(Comparator.comparing(ActivityEventDTO::getTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));

        if (events.size() > limit) {
            return new ArrayList<>(events.subList(0, limit));
        }
        return events;
    }

    private ActivityEventDTO buildRequestEvent(RentalRequest req) {
        String type;
        String message;
        RequestStatus status = req.getStatus();
        String equipmentName = req.getEquipment().getName();

        switch (status) {
            case APPROVED -> {
                type = "REQUEST_APPROVED";
                message = String.format("Request approved for %s", equipmentName);
            }
            case REJECTED -> {
                type = "REQUEST_REJECTED";
                message = String.format("Request rejected for %s", equipmentName);
            }
            case RENTED -> {
                type = "REQUEST_RENTED";
                message = String.format("%s rented out", equipmentName);
            }
            case RETURNED -> {
                type = "REQUEST_RETURNED";
                message = String.format("%s returned, awaiting assessment", equipmentName);
            }
            case COMPLETED -> {
                type = "REQUEST_COMPLETED";
                message = String.format("Rental completed for %s", equipmentName);
            }
            default -> {
                type = "REQUEST_CREATED";
                message = String.format("New request for %s", equipmentName);
            }
        }

        return new ActivityEventDTO(
                type,
                message,
                req.getUser().getUsername(),
                req.getCreatedAt() != null
                        ? req.getCreatedAt()
                        : req.getStartDate().atTime(LocalTime.NOON),
                req.getId()
        );
    }
}
