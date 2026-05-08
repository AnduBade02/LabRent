package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.dto.DemoSimulationResultDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.EquipmentRepository;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;
import ro.atemustard.labrent.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DemoSimulationService {

    private static final int SIMULATED_DAYS = 30;
    private static final Set<Integer> SNAPSHOT_DAYS = Set.of(1, 10, 20, 30);
    private static final double MAX_OVERDUE_PENALTY = 10.0;

    private static final String[] PROJECT_TEMPLATES = {
            "Demo simulation - sensor calibration",
            "Demo simulation - embedded systems lab",
            "Demo simulation - signal integrity analysis",
            "Demo simulation - digital electronics practice",
            "Demo simulation - measurement benchmark",
            "Demo simulation - control systems experiment"
    };

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ReturnAssessmentRepository returnAssessmentRepository;
    private final EquipmentService equipmentService;
    private final UserService userService;
    private final PrioritizationService prioritizationService;

    public DemoSimulationService(UserRepository userRepository,
                                 EquipmentRepository equipmentRepository,
                                 RentalRequestRepository rentalRequestRepository,
                                 ReturnAssessmentRepository returnAssessmentRepository,
                                 EquipmentService equipmentService,
                                 UserService userService,
                                 PrioritizationService prioritizationService) {
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.returnAssessmentRepository = returnAssessmentRepository;
        this.equipmentService = equipmentService;
        this.userService = userService;
        this.prioritizationService = prioritizationService;
    }

    @Transactional
    public DemoSimulationResultDTO runThirtyDaySimulation(String operatorUsername) {
        User operator = userRepository.findByUsername(operatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", operatorUsername));
        if (operator.getRole() != Role.ADMIN) {
            throw new InvalidOperationException("Only admins can run the demo simulation");
        }

        List<User> users = userRepository.findByRole(Role.USER).stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Equipment> equipment = equipmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Equipment::getId))
                .collect(Collectors.toCollection(ArrayList::new));

        if (users.isEmpty() || equipment.isEmpty()) {
            throw new InvalidOperationException("Demo simulation needs at least one user and one equipment item");
        }

        LocalDate simulationStart = LocalDate.now().minusDays(SIMULATED_DAYS - 1L);
        LocalDateTime timestampStart = LocalDateTime.of(simulationStart, LocalTime.of(9, 0));

        DemoSimulationResultDTO result = new DemoSimulationResultDTO();
        result.setSimulatedDays(SIMULATED_DAYS);
        result.setSimulationStartDate(simulationStart);
        result.setSimulationEndDate(simulationStart.plusDays(SIMULATED_DAYS - 1L));

        Map<Long, Double> reputationBefore = users.stream()
                .collect(Collectors.toMap(User::getId, User::getReputationScore));
        List<RepeatPlan> repeatPlans = buildRepeatPlans(users, equipment);

        int created = 0;
        int approved = 0;
        int rejected = 0;
        int rented = 0;
        int returned = 0;
        int completed = 0;

        for (int day = 1; day <= SIMULATED_DAYS; day++) {
            LocalDate simulatedDate = simulationStart.plusDays(day - 1L);
            LocalDateTime dayTimestamp = timestampStart.plusDays(day - 1L);

            for (RepeatPlan plan : repeatPlans) {
                if (plan.seedDay() == day) {
                    RentalRequest seed = createRequest(
                            plan.user(), plan.equipment(), plan.projectDescription(),
                            simulatedDate, dayTimestamp.plusMinutes(5), false, false);
                    seed.setStatus(RequestStatus.REJECTED);
                    rentalRequestRepository.save(seed);
                    created++;
                    rejected++;
                    addEvent(result, day, simulatedDate, "REJECTED",
                            "Rejected initial request #" + seed.getId() + " for "
                                    + seed.getEquipment().getName() + "; a later identical retry can receive a boost.");
                }
                if (plan.retryDay() == day) {
                    RentalRequest retry = createRequest(
                            plan.user(), plan.equipment(), plan.projectDescription(),
                            simulatedDate, dayTimestamp.plusMinutes(15), false, false);
                    created++;
                    addEvent(result, day, simulatedDate, "RETRY",
                            "Created repeated request #" + retry.getId() + " after an earlier rejection; "
                                    + "priority now includes the retry boost.");
                }
            }

            int dailyRequests = day % 5 == 0 ? 3 : 2;
            for (int slot = 0; slot < dailyRequests; slot++) {
                User user = users.get((day + slot) % users.size());
                Equipment item = equipment.get((day * 2 + slot) % equipment.size());
                boolean wantsExam = user.getUserType() == UserType.STUDENT && (day + slot) % 4 == 0;
                String project = PROJECT_TEMPLATES[(day + slot) % PROJECT_TEMPLATES.length]
                        + " D" + String.format(Locale.ROOT, "%02d", day)
                        + "-S" + slot;

                RentalRequest request = createRequest(
                        user, item, project, simulatedDate, dayTimestamp.plusMinutes(20 + slot * 10L),
                        wantsExam, true);
                created++;
                addEvent(result, day, simulatedDate, "CREATED",
                        "Created request #" + request.getId() + " by " + user.getUsername()
                                + " for " + item.getName()
                                + " (priority " + formatScore(request.getPriorityScore()) + ").");
            }

            DecisionStats decisions = processPendingDecisions(day, simulatedDate, result);
            approved += decisions.approved();
            rejected += decisions.rejected();

            int rentedToday = processApprovedPickups(day, simulatedDate, result);
            rented += rentedToday;

            CompletionStats completions = processDueRentals(day, simulatedDate, operator, result);
            returned += completions.returned();
            completed += completions.completed();

            prioritizationService.recalculateAllPending(simulatedDate);
            if (SNAPSHOT_DAYS.contains(day)) {
                capturePrioritySnapshots(result, day, simulatedDate);
            }
        }

        prioritizationService.recalculateAllPending(LocalDate.now());

        result.setCreatedRequests(created);
        result.setApprovedRequests(approved);
        result.setRejectedRequests(rejected);
        result.setRentedRequests(rented);
        result.setReturnedRequests(returned);
        result.setCompletedAssessments(completed);
        result.setReputationChanges(buildReputationChanges(reputationBefore));
        return result;
    }

    private RentalRequest createRequest(User user, Equipment equipment, String projectDescription,
                                        LocalDate simulatedDate, LocalDateTime createdAt,
                                        boolean wantsExam, boolean includeDemoDateInProject) {
        LocalDate start = simulatedDate.plusDays(1 + Math.floorMod(user.getId().intValue(), 3));
        LocalDate end = start.plusDays(4 + Math.floorMod(equipment.getId().intValue(), 5));
        boolean academic = wantsExam && user.getUserType() == UserType.STUDENT;
        String description = includeDemoDateInProject
                ? projectDescription + " (" + simulatedDate + ")"
                : projectDescription;

        RentalRequest.Builder builder = RentalRequest.builder()
                .user(user)
                .equipment(equipment)
                .startDate(start)
                .endDate(end)
                .projectDescription(description)
                .isForExam(academic);
        if (academic) {
            builder.examDate(start.plusDays(2))
                    .justification("Demo exam scenario generated by the 30-day simulation.");
        }

        RentalRequest request = rentalRequestRepository.save(builder.build());
        rentalRequestRepository.overrideCreatedAt(request.getId(), createdAt);
        request.setCreatedAt(createdAt);
        request.setPriorityScore(prioritizationService.calculatePriority(request, simulatedDate));
        request = rentalRequestRepository.save(request);
        prioritizationService.recalculateForEquipment(equipment.getId(), simulatedDate);
        return request;
    }

    private DecisionStats processPendingDecisions(int day, LocalDate simulatedDate,
                                                  DemoSimulationResultDTO result) {
        List<RentalRequest> pending = rentalRequestRepository.findByStatus(RequestStatus.PENDING).stream()
                .filter(r -> !r.getStartDate().isAfter(simulatedDate.plusDays(2)))
                .sorted(priorityComparator())
                .limit(day % 3 == 0 ? 3 : 2)
                .toList();

        int approved = 0;
        int rejected = 0;
        for (RentalRequest request : pending) {
            if (shouldReject(day, request)) {
                request.setStatus(RequestStatus.REJECTED);
                rentalRequestRepository.save(request);
                rejected++;
                addEvent(result, day, simulatedDate, "REJECTED",
                        "Rejected request #" + request.getId() + " for " + request.getEquipment().getName()
                                + " (priority " + formatScore(request.getPriorityScore()) + ").");
                continue;
            }

            Equipment freshEquipment = equipmentRepository.findById(request.getEquipment().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id",
                            request.getEquipment().getId()));
            if (freshEquipment.getAvailableQuantity() <= 0) {
                request.setStatus(RequestStatus.REJECTED);
                rentalRequestRepository.save(request);
                rejected++;
                addEvent(result, day, simulatedDate, "REJECTED",
                        "Rejected request #" + request.getId() + " because no unit was available.");
                continue;
            }

            equipmentService.reserveUnit(freshEquipment.getId());
            request.setStatus(RequestStatus.APPROVED);
            rentalRequestRepository.save(request);
            approved++;
            addEvent(result, day, simulatedDate, "APPROVED",
                    "Approved request #" + request.getId() + " for " + request.getEquipment().getName()
                            + " (priority " + formatScore(request.getPriorityScore()) + ").");
        }
        return new DecisionStats(approved, rejected);
    }

    private int processApprovedPickups(int day, LocalDate simulatedDate, DemoSimulationResultDTO result) {
        List<RentalRequest> approved = rentalRequestRepository.findByStatus(RequestStatus.APPROVED).stream()
                .filter(r -> !r.getStartDate().isAfter(simulatedDate))
                .sorted(Comparator.comparing(RentalRequest::getStartDate))
                .limit(4)
                .toList();

        int rented = 0;
        for (RentalRequest request : approved) {
            request.setStatus(RequestStatus.RENTED);
            rentalRequestRepository.save(request);
            rented++;
            addEvent(result, day, simulatedDate, "RENTED",
                    "Marked request #" + request.getId() + " as rented.");
        }
        return rented;
    }

    private CompletionStats processDueRentals(int day, LocalDate simulatedDate, User operator,
                                              DemoSimulationResultDTO result) {
        List<RentalRequest> due = rentalRequestRepository.findByStatus(RequestStatus.RENTED).stream()
                .filter(r -> !r.getEndDate().isAfter(simulatedDate))
                .sorted(Comparator.comparing(RentalRequest::getEndDate))
                .limit(4)
                .toList();

        int returned = 0;
        int completed = 0;
        for (RentalRequest request : due) {
            int lateDays = Math.floorMod(request.getId().intValue() + day, 4);
            LocalDate returnedAt = request.getEndDate().plusDays(lateDays);
            if (returnedAt.isAfter(simulatedDate)) {
                returnedAt = simulatedDate;
            }

            request.setStatus(RequestStatus.RETURNED);
            request.setReturnedAt(returnedAt);
            rentalRequestRepository.save(request);
            returned++;

            ConditionRating rating = ratingFor(request, day, lateDays);
            double impact = rating.getReputationImpact() + calculateOverduePenalty(request);
            ReturnAssessment assessment = ReturnAssessment.builder()
                    .rentalRequest(request)
                    .operator(operator)
                    .conditionRating(rating)
                    .notes("Demo simulation assessment; late days = "
                            + ChronoUnit.DAYS.between(request.getEndDate(), request.getReturnedAt()) + ".")
                    .build();
            assessment.setReputationImpact(impact);
            ReturnAssessment savedAssessment = returnAssessmentRepository.save(assessment);
            returnAssessmentRepository.overrideAssessedAt(
                    savedAssessment.getId(), LocalDateTime.of(simulatedDate, LocalTime.of(16, 0)));

            request.setStatus(RequestStatus.COMPLETED);
            rentalRequestRepository.save(request);
            userService.updateReputationScore(request.getUser().getId(), impact);
            equipmentService.releaseUnit(request.getEquipment().getId());
            prioritizationService.recalculateForUser(request.getUser().getId(), simulatedDate);
            completed++;

            addEvent(result, day, simulatedDate, "COMPLETED",
                    "Completed request #" + request.getId() + " with " + rating.name()
                            + " assessment (reputation impact " + formatSigned(impact) + ").");
        }
        return new CompletionStats(returned, completed);
    }

    private boolean shouldReject(int day, RentalRequest request) {
        return Math.floorMod(request.getId().intValue() + day, 7) == 0
                || (request.getPriorityScore() != null && request.getPriorityScore() < 45.0);
    }

    private ConditionRating ratingFor(RentalRequest request, int day, int lateDays) {
        if (lateDays >= 3) {
            return Math.floorMod(day + request.getId().intValue(), 2) == 0
                    ? ConditionRating.POOR : ConditionRating.FAIR;
        }
        ConditionRating[] cycle = {
                ConditionRating.EXCELLENT,
                ConditionRating.GOOD,
                ConditionRating.FAIR,
                ConditionRating.POOR
        };
        return cycle[Math.floorMod(day + request.getId().intValue(), cycle.length)];
    }

    private double calculateOverduePenalty(RentalRequest request) {
        if (request.getReturnedAt() == null || request.getEndDate() == null
                || !request.getReturnedAt().isAfter(request.getEndDate())) {
            return 0.0;
        }
        long daysLate = ChronoUnit.DAYS.between(request.getEndDate(), request.getReturnedAt());
        return -Math.min(MAX_OVERDUE_PENALTY, daysLate);
    }

    private Comparator<RentalRequest> priorityComparator() {
        return Comparator
                .comparing((RentalRequest r) -> r.getPriorityScore() == null ? 0.0 : r.getPriorityScore())
                .reversed()
                .thenComparing(RentalRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void capturePrioritySnapshots(DemoSimulationResultDTO result, int day, LocalDate simulatedDate) {
        Map<Equipment, Long> pendingByEquipment = rentalRequestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .collect(Collectors.groupingBy(RentalRequest::getEquipment,
                        LinkedHashMap::new, Collectors.counting()));

        pendingByEquipment.entrySet().stream()
                .sorted(Map.Entry.<Equipment, Long>comparingByValue().reversed())
                .limit(2)
                .forEach(entry -> {
                    Equipment equipment = entry.getKey();
                    List<RentalRequest> prioritizedQueue =
                            prioritizationService.getPrioritizedQueue(equipment.getId());
                    List<DemoSimulationResultDTO.DemoQueueEntryDTO> queue = new ArrayList<>();
                    for (int i = 0; i < prioritizedQueue.size() && i < 5; i++) {
                        queue.add(toQueueEntry(prioritizedQueue.get(i), simulatedDate, i + 1));
                    }
                    result.getPrioritySnapshots().add(
                            new DemoSimulationResultDTO.DemoPrioritySnapshotDTO(
                                    day, simulatedDate, equipment.getId(), equipment.getName(), queue));
                });
    }

    private DemoSimulationResultDTO.DemoQueueEntryDTO toQueueEntry(RentalRequest request,
                                                                   LocalDate simulatedDate,
                                                                   int rank) {
        int waitingDays = request.getCreatedAt() == null ? 0
                : (int) Math.max(0, ChronoUnit.DAYS.between(
                request.getCreatedAt().toLocalDate(), simulatedDate));
        int previousRejected = previousRejectedSimilarCount(request);
        boolean exam = request instanceof AcademicRentalRequest;
        return new DemoSimulationResultDTO.DemoQueueEntryDTO(
                rank,
                request.getId(),
                request.getUser().getUsername(),
                request.getPriorityScore(),
                waitingDays,
                previousRejected,
                exam,
                request.getUser().getReputationScore()
        );
    }

    private int previousRejectedSimilarCount(RentalRequest request) {
        if (request.getProjectDescription() == null || request.getProjectDescription().isBlank()) {
            return 0;
        }
        return (int) rentalRequestRepository
                .countByUserIdAndEquipmentIdAndStatusAndProjectDescriptionIgnoreCase(
                        request.getUser().getId(),
                        request.getEquipment().getId(),
                        RequestStatus.REJECTED,
                        request.getProjectDescription().trim()
                );
    }

    private List<DemoSimulationResultDTO.DemoReputationChangeDTO> buildReputationChanges(
            Map<Long, Double> reputationBefore) {
        return userRepository.findByRole(Role.USER).stream()
                .sorted(Comparator.comparing(User::getUsername))
                .filter(user -> Math.abs(user.getReputationScore()
                        - reputationBefore.getOrDefault(user.getId(), user.getReputationScore())) >= 0.01)
                .map(user -> new DemoSimulationResultDTO.DemoReputationChangeDTO(
                        user.getId(),
                        user.getUsername(),
                        reputationBefore.getOrDefault(user.getId(), user.getReputationScore()),
                        user.getReputationScore()))
                .toList();
    }

    private List<RepeatPlan> buildRepeatPlans(List<User> users, List<Equipment> equipment) {
        List<RepeatPlan> plans = new ArrayList<>();
        int[] seedDays = {3, 11, 19};
        for (int i = 0; i < seedDays.length; i++) {
            User user = users.get(Math.floorMod(i * 3 + 1, users.size()));
            Equipment item = equipment.get(Math.floorMod(i * 4 + 2, equipment.size()));
            String project = "Demo simulation - repeated rejected request " + (i + 1);
            plans.add(new RepeatPlan(seedDays[i], seedDays[i] + 4, user, item, project));
        }
        return plans;
    }

    private void addEvent(DemoSimulationResultDTO result, int day, LocalDate date,
                          String type, String message) {
        result.getTimeline().add(new DemoSimulationResultDTO.DemoSimulationEventDTO(
                day, date, type, message));
    }

    private String formatScore(Double score) {
        return score == null ? "N/A" : String.format(Locale.ROOT, "%.1f", score);
    }

    private String formatSigned(double value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private record DecisionStats(int approved, int rejected) {}

    private record CompletionStats(int returned, int completed) {}

    private record RepeatPlan(int seedDay, int retryDay, User user, Equipment equipment,
                              String projectDescription) {}
}
