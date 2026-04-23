package ro.atemustard.labrent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.*;

import java.time.LocalDate;

/**
 * Seeds the database with test data on application startup.
 * Runs only when the users table is empty (first start).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final ReturnAssessmentRepository returnAssessmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      EquipmentRepository equipmentRepository,
                      RentalRequestRepository rentalRequestRepository,
                      ReturnAssessmentRepository returnAssessmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.returnAssessmentRepository = returnAssessmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding database with test data...");

        // --- Users ---
        User admin = new User("admin", "admin@labrent.ro", passwordEncoder.encode("admin123"), Role.ADMIN, UserType.NON_STUDENT);
        admin = userRepository.save(admin);

        User student1 = new User("ion.popescu", "ion@student.ro", passwordEncoder.encode("parola123"), Role.USER, UserType.STUDENT);
        student1 = userRepository.save(student1);

        User student2 = new User("maria.ionescu", "maria@student.ro", passwordEncoder.encode("parola123"), Role.USER, UserType.STUDENT);
        student2.setReputationScore(120.0);
        student2 = userRepository.save(student2);

        User nonstudent = new User("andrei.dumitrescu", "andrei@extern.ro", passwordEncoder.encode("parola123"), Role.USER, UserType.NON_STUDENT);
        nonstudent = userRepository.save(nonstudent);

        User student3 = new User("elena.vasile", "elena@student.ro", passwordEncoder.encode("parola123"), Role.USER, UserType.STUDENT);
        student3.setReputationScore(80.0);
        student3 = userRepository.save(student3);

        // --- Equipment ---
        Equipment osciloscop = new Equipment("Rigol DS1054Z Oscilloscope", "Digital oscilloscope, 4 channels, 50MHz", "Oscilloscope", 5);
        osciloscop = equipmentRepository.save(osciloscop);

        Equipment multimetru = new Equipment("Fluke 117 Multimeter", "Digital True-RMS multimeter", "Multimeter", 10);
        multimetru = equipmentRepository.save(multimetru);

        Equipment arduino = new Equipment("Arduino Uno R3", "Development board with ATmega328P microcontroller", "Microcontroller", 15);
        arduino = equipmentRepository.save(arduino);

        Equipment raspberryPi = new Equipment("Raspberry Pi 4 Model B", "Single-board computer, 4GB RAM", "Single-Board Computer", 8);
        raspberryPi = equipmentRepository.save(raspberryPi);

        Equipment generator = new Equipment("Rigol DG1022 Signal Generator", "Arbitrary signal generator, 2 channels, 25MHz", "Signal Generator", 3);
        generator = equipmentRepository.save(generator);

        Equipment sursa = new Equipment("RXN-305D Power Supply", "Adjustable power supply 0-30V, 0-5A", "Power Supply", 6);
        sursa = equipmentRepository.save(sursa);

        Equipment analizor = new Equipment("Saleae Logic 8 Analyzer", "Logic analyzer, 8 channels, 25MHz", "Logic Analyzer", 4);
        analizor = equipmentRepository.save(analizor);

        // --- Rental Requests in various states ---

        // 1. PENDING request (student1 wants osciloscop)
        RentalRequest req1 = RentalRequest.builder()
                .user(student1)
                .equipment(osciloscop)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(14))
                .projectDescription("Analog Electronics lab project")
                .build();
        req1.setPriorityScore(65.0);
        rentalRequestRepository.save(req1);

        // 2. PENDING request with exam urgency (student2 wants osciloscop)
        RentalRequest req2 = RentalRequest.builder()
                .user(student2)
                .equipment(osciloscop)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .projectDescription("Preparation for Electric Circuits exam")
                .isForExam(true)
                .examDate(LocalDate.now().plusDays(8))
                .justification("Practical exam — need the oscilloscope for exercises")
                .build();
        req2.setPriorityScore(92.0);
        rentalRequestRepository.save(req2);

        // 3. APPROVED request (student1 has arduino approved)
        RentalRequest req3 = RentalRequest.builder()
                .user(student1)
                .equipment(arduino)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(21))
                .projectDescription("IoT project — temperature monitoring system")
                .build();
        req3.setStatus(RequestStatus.APPROVED);
        req3.setPriorityScore(70.0);
        arduino.setAvailableQuantity(arduino.getAvailableQuantity() - 1);
        equipmentRepository.save(arduino);
        rentalRequestRepository.save(req3);

        // 4. RENTED request (nonstudent has multimetru)
        RentalRequest req4 = RentalRequest.builder()
                .user(nonstudent)
                .equipment(multimetru)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .projectDescription("Electrical installation checks")
                .build();
        req4.setStatus(RequestStatus.RENTED);
        req4.setPriorityScore(55.0);
        multimetru.setAvailableQuantity(multimetru.getAvailableQuantity() - 1);
        equipmentRepository.save(multimetru);
        rentalRequestRepository.save(req4);

        // 5. RETURNED request (student3 returned generator, awaiting assessment)
        RentalRequest req5 = RentalRequest.builder()
                .user(student3)
                .equipment(generator)
                .startDate(LocalDate.now().minusDays(14))
                .endDate(LocalDate.now().minusDays(1))
                .projectDescription("Telecommunications lab")
                .build();
        req5.setStatus(RequestStatus.RETURNED);
        req5.setPriorityScore(60.0);
        rentalRequestRepository.save(req5);

        // 6. RETURNED + assessed (student2 returned raspberryPi — excellent condition)
        RentalRequest req6 = RentalRequest.builder()
                .user(student2)
                .equipment(raspberryPi)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(10))
                .projectDescription("Embedded Systems project")
                .build();
        req6.setStatus(RequestStatus.COMPLETED);
        req6.setPriorityScore(75.0);
        rentalRequestRepository.save(req6);

        ReturnAssessment assessment1 = ReturnAssessment.builder()
                .rentalRequest(req6)
                .operator(admin)
                .conditionRating(ConditionRating.EXCELLENT)
                .notes("Equipment returned in perfect condition, all accessories present.")
                .build();
        returnAssessmentRepository.save(assessment1);

        // 7. REJECTED request (elena wanted sursa but was rejected)
        RentalRequest req7 = RentalRequest.builder()
                .user(student3)
                .equipment(sursa)
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(7))
                .projectDescription("Personal project")
                .build();
        req7.setStatus(RequestStatus.REJECTED);
        req7.setPriorityScore(35.0);
        rentalRequestRepository.save(req7);

        // 8. Another PENDING (nonstudent wants analizor)
        RentalRequest req8 = RentalRequest.builder()
                .user(nonstudent)
                .equipment(analizor)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(17))
                .projectDescription("SPI protocol debugging on custom board")
                .build();
        req8.setPriorityScore(50.0);
        rentalRequestRepository.save(req8);

        log.info("Database seeded successfully!");
        log.info("=== Test Accounts ===");
        log.info("ADMIN:  admin / admin123");
        log.info("USER:   ion.popescu / parola123  (student, reputation=100)");
        log.info("USER:   maria.ionescu / parola123 (student, reputation=120)");
        log.info("USER:   andrei.dumitrescu / parola123 (non-student, reputation=100)");
        log.info("USER:   elena.vasile / parola123  (student, reputation=80)");
    }
}
