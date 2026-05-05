package ro.atemustard.labrent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.*;
import ro.atemustard.labrent.service.PrioritizationService;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds the database with rich demo data on the first application start.
 *
 * The scenario is designed to make every feature visible without interaction:
 * - 11 users with reputations spanning 40..175 to show how trust shifts priority
 * - 10 equipment types, including 2 scarce premium units with competing queues
 * - PENDING queues with 2..4 competitors on the same equipment to demo
 *   weighted-scoring vs. FIFO (switch strategies in the Admin UI and watch
 *   the queue reorder)
 * - 3 currently-overdue rentals so the red badges, dashboard counters, and
 *   overdue-penalty logic are visible from the start
 * - 2 RETURNED rentals awaiting assessment (one on-time, one 4 days late)
 *   so the admin can submit assessments and see the penalty applied live
 * - 9 COMPLETED rentals spanning the full 5-rating spectrum (EXCELLENT ..
 *   DAMAGED) that together justify each user's current reputation and
 *   populate the activity feed
 * - 3 REJECTED requests to round out the status distribution
 *
 * Runs only when the users table is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ReturnAssessmentRepository returnAssessmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PrioritizationService prioritizationService;

    public DataSeeder(UserRepository userRepository,
                      EquipmentRepository equipmentRepository,
                      RentalRequestRepository rentalRequestRepository,
                      ReturnAssessmentRepository returnAssessmentRepository,
                      PasswordEncoder passwordEncoder,
                      PrioritizationService prioritizationService) {
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.returnAssessmentRepository = returnAssessmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.prioritizationService = prioritizationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding database with rich demo data...");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // ========================================================================
        // USERS
        // ========================================================================
        // Reputations are set explicitly; the COMPLETED history below justifies
        // each score narratively (e.g. radu has 3 EXCELLENT returns; alex has
        // a DAMAGED + POOR history).

        User admin = saveUser("admin", "admin@labrent.ro", "admin123",
                Role.ADMIN, UserType.NON_STUDENT, 100.0);
        User operator2 = saveUser("operator2", "operator2@labrent.ro", "admin123",
                Role.ADMIN, UserType.NON_STUDENT, 100.0);

        // Students
        User ion = saveUser("ion.popescu", "ion@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 100.0);
        User maria = saveUser("maria.ionescu", "maria@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 145.0);
        User radu = saveUser("radu.georgescu", "radu@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 175.0);
        User diana = saveUser("diana.stoica", "diana@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 115.0);
        User cristina = saveUser("cristina.dobre", "cristina@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 135.0);
        User elena = saveUser("elena.vasile", "elena@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 65.0);
        User alex = saveUser("alex.marinescu", "alex@student.ro", "parola123",
                Role.USER, UserType.STUDENT, 40.0);

        // Non-students
        User andrei = saveUser("andrei.dumitrescu", "andrei@extern.ro", "parola123",
                Role.USER, UserType.NON_STUDENT, 105.0);
        User mihai = saveUser("mihai.constantin", "mihai@research.ro", "parola123",
                Role.USER, UserType.NON_STUDENT, 90.0);

        // ========================================================================
        // EQUIPMENT
        // ========================================================================

        Equipment rigolScope = saveEquipment("Rigol DS1054Z Oscilloscope",
                "Digital oscilloscope, 4 channels, 50MHz", "Oscilloscope", 5);
        Equipment tektronixScope = saveEquipment("Tektronix TBS2000 Oscilloscope",
                "Premium digital oscilloscope, 200MHz, 2 channels — scarce unit",
                "Oscilloscope", 2);
        Equipment flukeMulti = saveEquipment("Fluke 117 Multimeter",
                "Digital True-RMS multimeter", "Multimeter", 10);
        Equipment keysightMulti = saveEquipment("Keysight 34461A Multimeter",
                "6½-digit benchtop multimeter — premium, limited availability",
                "Multimeter", 2);
        Equipment arduino = saveEquipment("Arduino Uno R3",
                "Development board with ATmega328P microcontroller",
                "Microcontroller", 15);
        Equipment stm32 = saveEquipment("STM32 Nucleo-F401RE",
                "ARM Cortex-M4 development board, 84MHz, 512KB flash",
                "Microcontroller", 10);
        Equipment rpi = saveEquipment("Raspberry Pi 4 Model B",
                "Single-board computer, 4GB RAM", "Single-Board Computer", 8);
        Equipment signalGen = saveEquipment("Rigol DG1022 Signal Generator",
                "Arbitrary signal generator, 2 channels, 25MHz",
                "Signal Generator", 3);
        Equipment powerSupply = saveEquipment("RXN-305D Power Supply",
                "Adjustable power supply 0-30V, 0-5A", "Power Supply", 6);
        Equipment logicAnalyzer = saveEquipment("Saleae Logic 8 Analyzer",
                "Logic analyzer, 8 channels, 25MHz", "Logic Analyzer", 4);

        // ========================================================================
        // COMPLETED rentals + return assessments
        // ------------------------------------------------------------------------
        // These are the "history" that justifies each user's reputation and
        // populates the recent activity feed. They don't consume current
        // availability (units were released on assessment).
        // ========================================================================

        // radu — top student: 3 EXCELLENT returns
        completed(radu, rigolScope, today.minusDays(75), today.minusDays(62),
                "Signal Processing lab — harmonic analysis",
                admin, ConditionRating.EXCELLENT,
                "Returned pristine, all probes present and calibrated.",
                now.minusDays(62));
        completed(radu, rpi, today.minusDays(60), today.minusDays(48),
                "Embedded Linux project — custom kernel build",
                admin, ConditionRating.EXCELLENT,
                "Perfect condition, heatsink and power adapter included.",
                now.minusDays(48));

        // maria — reliable: 1 EXCELLENT + 1 GOOD
        completed(maria, arduino, today.minusDays(55), today.minusDays(42),
                "Digital Electronics lab — sequential logic",
                operator2, ConditionRating.EXCELLENT,
                "Excellent care, breadboard kit also returned organised.",
                now.minusDays(42));
        completed(maria, logicAnalyzer, today.minusDays(35), today.minusDays(26),
                "I2C bus debugging on custom sensor board",
                admin, ConditionRating.GOOD,
                "Minor scuff on the clip cable, otherwise fully functional.",
                now.minusDays(26));

        // diana — 1 EXCELLENT
        completed(diana, stm32, today.minusDays(45), today.minusDays(32),
                "Real-Time OS project — FreeRTOS port",
                admin, ConditionRating.EXCELLENT,
                "Outstanding handling, all adapters and cables present.",
                now.minusDays(32));

        // cristina — 1 GOOD
        completed(cristina, powerSupply, today.minusDays(30), today.minusDays(22),
                "Bench setup for LED driver characterization",
                operator2, ConditionRating.GOOD,
                "Light cosmetic wear on the front knob, functionally OK.",
                now.minusDays(22));

        // alex — poor track record: 1 DAMAGED + 1 POOR late
        completed(alex, signalGen, today.minusDays(68), today.minusDays(58),
                "Modulation lab — FM/AM experiments",
                admin, ConditionRating.DAMAGED,
                "BNC connector on channel 2 snapped off, needs repair. Visible drop marks on the chassis.",
                now.minusDays(58));
        // This one was returned 5 days late; reputation took a double hit.
        completedLate(alex, flukeMulti, today.minusDays(40), today.minusDays(30),
                today.minusDays(25),
                "Field measurements for thesis prototype",
                operator2, ConditionRating.POOR,
                "Test leads frayed, battery compartment cracked. Also returned 5 days past due.",
                now.minusDays(25));

        // elena — 1 POOR
        completed(elena, rigolScope, today.minusDays(50), today.minusDays(38),
                "Analog filters project",
                admin, ConditionRating.POOR,
                "Several probes missing, case scratched heavily.",
                now.minusDays(38));

        // ========================================================================
        // REJECTED requests
        // ========================================================================

        rejected(elena, signalGen, today.minusDays(8), today.plusDays(5),
                "Personal project", 35.0, now.minusDays(12));
        rejected(alex, keysightMulti, today.minusDays(6), today.plusDays(10),
                "Precision measurements for hobby build", 28.0, now.minusDays(10));
        rejected(mihai, tektronixScope, today.minusDays(4), today.plusDays(7),
                "Research benchmark — no justification provided", 42.0,
                now.minusDays(8));

        // ========================================================================
        // RETURNED awaiting assessment
        // ------------------------------------------------------------------------
        // These units are still "out" physically (availableQuantity is
        // decremented). Admin will complete the assessment from the UI and
        // see reputation impact applied — including overdue penalty for maria.
        // ========================================================================

        // radu returned Arduino on time — submit EXCELLENT to see +5 on his score
        returnedAwaiting(radu, arduino,
                today.minusDays(10), today, today.minusDays(1), 82.0,
                "IoT prototype — temperature + humidity monitoring",
                now.minusDays(10));

        // maria returned Power Supply 4 days late — submit to see combined
        // rating impact + overdue penalty (up to -4 extra)
        returnedAwaiting(maria, powerSupply,
                today.minusDays(16), today.minusDays(6), today.minusDays(2), 78.0,
                "Battery charger design — constant-current source",
                now.minusDays(16));

        // ========================================================================
        // RENTED — currently overdue
        // ------------------------------------------------------------------------
        // These flip the red OVERDUE badge, inflate the dashboard counter, and
        // give manage-requests an obvious "fix me" list sorted first.
        // ========================================================================

        // alex: 8 days overdue (heavy) — his active penalty + reputation
        //        already low, will be even lower after eventual assessment
        rented(alex, flukeMulti,
                today.minusDays(18), today.minusDays(8), 58.0,
                "Voltage profiling for sensor calibration",
                now.minusDays(18));

        // elena: 3 days overdue
        rented(elena, rpi,
                today.minusDays(17), today.minusDays(3), 62.0,
                "Home-automation gateway development",
                now.minusDays(17));

        // mihai: 1 day overdue — edge case, just tipped over
        rented(mihai, signalGen,
                today.minusDays(12), today.minusDays(1), 55.0,
                "RF testing for research paper",
                now.minusDays(12));

        // ========================================================================
        // RENTED — on time
        // ========================================================================

        // cristina has RPi, 5 days remaining
        rented(cristina, rpi,
                today.minusDays(4), today.plusDays(5), 77.0,
                "Embedded Linux course final project",
                now.minusDays(4));

        // radu has STM32, 8 days remaining
        rented(radu, stm32,
                today.minusDays(3), today.plusDays(8), 85.0,
                "Motor control — field-oriented control on BLDC",
                now.minusDays(3));

        // ion has Arduino, 12 days remaining
        rented(ion, arduino,
                today.minusDays(2), today.plusDays(12), 70.0,
                "First-year Intro to Microcontrollers lab",
                now.minusDays(2));

        // ========================================================================
        // APPROVED — waiting to be handed out
        // ========================================================================

        approved(maria, arduino,
                today.plusDays(1), today.plusDays(14), 78.0,
                "Digital Signal Processing — FIR filter implementation",
                now.minusDays(1));

        approved(andrei, flukeMulti,
                today, today.plusDays(10), 65.0,
                "Electrical audit for commercial installation",
                now.minusDays(2));

        approved(diana, signalGen,
                today.plusDays(2), today.plusDays(9), 80.0,
                "Lab for Telecommunications course",
                now.minusDays(1));

        // ========================================================================
        // PENDING — where the priority system is most visible.
        // ------------------------------------------------------------------------
        // Scores are placeholders here; recalculateAllPending() at the end
        // replaces them with values computed by the real strategy, which lets
        // admins verify the algorithm by switching strategies in the UI.
        //
        // createdAt order matters here — FIFO ranks by age, so the submission
        // timestamps are staggered to make the two strategies produce visibly
        // different orderings.
        // ========================================================================

        // --- Tektronix TBS2000 (2 units, 4 competitors) ------------------
        // Weighted expected winner: alex (exam in 2 days beats radu's top rep)
        // FIFO expected winner: andrei (submitted first)
        pending(andrei, tektronixScope,
                today.plusDays(2), today.plusDays(12),
                "Cross-lab oscilloscope benchmarking for research report",
                false, null, null,
                now.minusDays(4));
        pending(maria, tektronixScope,
                today.plusDays(1), today.plusDays(9),
                "High-frequency probe response testing",
                false, null, null,
                now.minusDays(3));
        pending(radu, tektronixScope,
                today.plusDays(3), today.plusDays(14),
                "Bachelor's thesis — PCB signal integrity measurements",
                false, null, null,
                now.minusDays(2));
        pending(alex, tektronixScope,
                today.plusDays(1), today.plusDays(7),
                "Catch-up lab session — missed previous deadline",
                true, today.plusDays(2),
                "Practical exam in 2 days; need oscilloscope for the filter lab I missed.",
                now.minusHours(3));

        // --- Rigol DS1054Z Oscilloscope (5 units, 3 competitors) ---------
        // Weighted winner: diana (exam in 4 days)
        pending(mihai, rigolScope,
                today.plusDays(3), today.plusDays(15),
                "External research — control-loop characterization",
                false, null, null,
                now.minusDays(2));
        pending(cristina, rigolScope,
                today.plusDays(2), today.plusDays(10),
                "Signal Processing elective — lab demo",
                false, null, null,
                now.minusDays(1));
        pending(diana, rigolScope,
                today.plusDays(1), today.plusDays(8),
                "Telecommunications course — pre-exam review session",
                true, today.plusDays(4),
                "Telecommunications final exam in 4 days; oscilloscope required.",
                now.minusHours(6));

        // --- Keysight 34461A premium multimeter (2 units, 2 competitors) -
        // Demonstrates active-request penalty: radu already has the
        // Tektronix PENDING + STM32 RENTED, so active=3 penalises him.
        pending(radu, keysightMulti,
                today.plusDays(3), today.plusDays(11),
                "Thesis: ADC noise characterization (requires 6½-digit precision)",
                false, null, null,
                now.minusDays(2));
        pending(andrei, keysightMulti,
                today.plusDays(5), today.plusDays(15),
                "External research — precision instrumentation setup",
                false, null, null,
                now.minusDays(3));

        // --- Saleae Logic 8 Analyzer (4 units, 1 user) -------------------
        // Single-applicant case for baseline display.
        pending(ion, logicAnalyzer,
                today.plusDays(2), today.plusDays(16),
                "First-year digital design lab — debugging state machines",
                false, null, null,
                now.minusDays(4));

        // ========================================================================
        // Recalculate all PENDING scores using the real strategy.
        // ------------------------------------------------------------------------
        // This makes the scores match the actual algorithm and — crucially —
        // correctly reflects each user's active-request count, which depends on
        // everything above being seeded first.
        // ========================================================================
        prioritizationService.recalculateAllPending();

        logSummary();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private User saveUser(String username, String email, String rawPassword,
                          Role role, UserType userType, double reputation) {
        User u = new User(username, email, passwordEncoder.encode(rawPassword), role, userType);
        u.setReputationScore(reputation);
        return userRepository.save(u);
    }

    private Equipment saveEquipment(String name, String description,
                                    String category, int totalQuantity) {
        return equipmentRepository.save(
                new Equipment(name, description, category, totalQuantity));
    }

    /**
     * Creates a COMPLETED rental with a matching ReturnAssessment. The unit is
     * considered already released (availableQuantity unchanged).
     */
    private void completed(User user, Equipment equipment,
                           LocalDate start, LocalDate end,
                           String projectDescription,
                           User operator, ConditionRating rating, String notes,
                           LocalDateTime assessedAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.COMPLETED);
        req.setReturnedAt(end.minusDays(1));  // returned just before due date
        req.setPriorityScore(70.0);
        RentalRequest savedReq = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(savedReq.getId(), assessedAt.minusDays(2));

        ReturnAssessment a = ReturnAssessment.builder()
                .rentalRequest(savedReq).operator(operator)
                .conditionRating(rating).notes(notes)
                .build();
        ReturnAssessment savedA = returnAssessmentRepository.save(a);
        returnAssessmentRepository.overrideAssessedAt(savedA.getId(), assessedAt);
    }

    /**
     * COMPLETED variant with an explicit late return date for illustrating the
     * historical overdue penalty.
     */
    private void completedLate(User user, Equipment equipment,
                               LocalDate start, LocalDate end, LocalDate returnedAt,
                               String projectDescription,
                               User operator, ConditionRating rating, String notes,
                               LocalDateTime assessedAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.COMPLETED);
        req.setReturnedAt(returnedAt);
        req.setPriorityScore(60.0);
        RentalRequest savedReq = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(savedReq.getId(), assessedAt.minusDays(6));

        ReturnAssessment a = ReturnAssessment.builder()
                .rentalRequest(savedReq).operator(operator)
                .conditionRating(rating).notes(notes)
                .build();
        ReturnAssessment savedA = returnAssessmentRepository.save(a);
        returnAssessmentRepository.overrideAssessedAt(savedA.getId(), assessedAt);
    }

    private void rejected(User user, Equipment equipment,
                          LocalDate start, LocalDate end,
                          String projectDescription, double priority,
                          LocalDateTime createdAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.REJECTED);
        req.setPriorityScore(priority);
        RentalRequest saved = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(saved.getId(), createdAt);
    }

    /**
     * Creates a RETURNED rental where the unit is still physically out
     * pending operator assessment. Decrements availableQuantity.
     */
    private void returnedAwaiting(User user, Equipment equipment,
                                  LocalDate start, LocalDate end, LocalDate returnedAt,
                                  double priority, String projectDescription,
                                  LocalDateTime createdAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.RETURNED);
        req.setReturnedAt(returnedAt);
        req.setPriorityScore(priority);
        RentalRequest saved = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(saved.getId(), createdAt);

        decrementAvailable(equipment);
    }

    /**
     * Creates a RENTED rental. Decrements availableQuantity. End date may be
     * in the past to trigger overdue logic in the DTO layer.
     */
    private void rented(User user, Equipment equipment,
                        LocalDate start, LocalDate end, double priority,
                        String projectDescription, LocalDateTime createdAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.RENTED);
        req.setPriorityScore(priority);
        RentalRequest saved = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(saved.getId(), createdAt);

        decrementAvailable(equipment);
    }

    /**
     * Creates an APPROVED rental (reservation active). Decrements
     * availableQuantity, just like the real approveRequest flow.
     */
    private void approved(User user, Equipment equipment,
                          LocalDate start, LocalDate end, double priority,
                          String projectDescription, LocalDateTime createdAt) {
        RentalRequest req = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .build();
        req.setStatus(RequestStatus.APPROVED);
        req.setPriorityScore(priority);
        RentalRequest saved = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(saved.getId(), createdAt);

        decrementAvailable(equipment);
    }

    private void pending(User user, Equipment equipment,
                         LocalDate start, LocalDate end,
                         String projectDescription,
                         boolean isForExam, LocalDate examDate, String justification,
                         LocalDateTime createdAt) {
        RentalRequest.Builder b = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(start).endDate(end)
                .projectDescription(projectDescription)
                .isForExam(isForExam);
        if (isForExam) {
            b.examDate(examDate).justification(justification);
        }
        RentalRequest req = b.build();
        req.setPriorityScore(0.0);  // replaced by recalculateAllPending()
        RentalRequest saved = rentalRequestRepository.save(req);
        rentalRequestRepository.overrideCreatedAt(saved.getId(), createdAt);
    }

    private void decrementAvailable(Equipment equipment) {
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() - 1);
        if (equipment.getAvailableQuantity() == 0
                && equipment.getStatus() == EquipmentStatus.AVAILABLE) {
            equipment.setStatus(EquipmentStatus.RESERVED);
        }
        equipmentRepository.save(equipment);
    }

    private void logSummary() {
        log.info("Database seeded successfully — {} users, {} equipment, {} requests, {} assessments.",
                userRepository.count(),
                equipmentRepository.count(),
                rentalRequestRepository.count(),
                returnAssessmentRepository.count());
        log.info("");
        log.info("=== Test Accounts (all passwords are \"parola123\" unless noted) ===");
        log.info("  admin             / admin123     — ADMIN  (primary operator)");
        log.info("  operator2         / admin123     — ADMIN  (secondary operator)");
        log.info("  radu.georgescu    / parola123    — STUDENT, rep 175  (top user, no current exam)");
        log.info("  maria.ionescu     / parola123    — STUDENT, rep 145  (reliable; has late return awaiting assessment)");
        log.info("  cristina.dobre    / parola123    — STUDENT, rep 135  (currently renting)");
        log.info("  diana.stoica      / parola123    — STUDENT, rep 115  (exam in 4 days — priority boost visible)");
        log.info("  andrei.dumitrescu / parola123    — NON_STUDENT, rep 105  (external collaborator)");
        log.info("  ion.popescu       / parola123    — STUDENT, rep 100  (fresh, baseline)");
        log.info("  mihai.constantin  / parola123    — NON_STUDENT, rep 90   (currently 1 day overdue)");
        log.info("  elena.vasile      / parola123    — STUDENT, rep 65   (currently 3 days overdue)");
        log.info("  alex.marinescu    / parola123    — STUDENT, rep 40   (damaged history, 8 days overdue, exam in 2 days)");
        log.info("");
        log.info("=== Things to check in the Admin UI ===");
        log.info("  * Dashboard  → 3 overdue rentals show in the KPI card");
        log.info("  * Equipment  → click Tektronix TBS2000: queue with 4 ranked competitors");
        log.info("  * Strategy   → switch weightedScoring ↔ fifo: Tektronix queue reorders visibly");
        log.info("  * Assessments → submit EXCELLENT for radu's Arduino return (+5 rep)");
        log.info("  * Assessments → submit any rating for maria's Power Supply return (late by 4 days)");
        log.info("                  → total impact = rating + overdue penalty, visible in history");
    }
}
