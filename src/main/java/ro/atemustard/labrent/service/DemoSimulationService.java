package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.dto.RentalRequestDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.Role;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;
import ro.atemustard.labrent.repository.EquipmentRepository;
import ro.atemustard.labrent.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DemoSimulationService {

    private static final String[] PROJECT_DESCRIPTIONS = {
            "Automated lab measurement session",
            "Prototype validation for embedded systems",
            "Signal acquisition and analysis task",
            "Power stability check for a lab prototype",
            "Sensor calibration and data logging",
            "Digital electronics debugging session",
            "Research demo for instrumentation workflow",
            "Microcontroller integration test"
    };

    private static final String[] EXAM_JUSTIFICATIONS = {
            "Required for the practical exam setup",
            "Needed to complete the exam project demonstration",
            "Used for final lab assessment measurements",
            "Required for validating the exam circuit"
    };

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final RentalRequestService rentalRequestService;

    public DemoSimulationService(UserRepository userRepository,
                                 EquipmentRepository equipmentRepository,
                                 RentalRequestService rentalRequestService) {
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.rentalRequestService = rentalRequestService;
    }

    public RentalRequestDTO createRandomRequest() {
        List<User> users = userRepository.findByRole(Role.USER);
        if (users.isEmpty()) {
            throw new InvalidOperationException("Simulation needs at least one regular user");
        }

        List<Equipment> availableEquipment = equipmentRepository.findAll().stream()
                .filter(e -> e.getAvailableQuantity() != null && e.getAvailableQuantity() > 0)
                .toList();
        if (availableEquipment.isEmpty()) {
            throw new InvalidOperationException("Simulation needs at least one available equipment item");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        User user = users.get(random.nextInt(users.size()));
        Equipment equipment = availableEquipment.get(random.nextInt(availableEquipment.size()));

        LocalDate startDate = LocalDate.now().plusDays(random.nextInt(0, 9));
        int durationDays = random.nextInt(2, 16);
        LocalDate endDate = startDate.plusDays(durationDays);

        RentalRequestCreateDTO dto = new RentalRequestCreateDTO();
        dto.setEquipmentId(equipment.getId());
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setProjectDescription(randomText(PROJECT_DESCRIPTIONS, random) + " - " + equipment.getCategory());

        boolean examRequest = user.getUserType() == UserType.STUDENT && random.nextInt(100) < 40;
        dto.setIsForExam(examRequest);
        if (examRequest) {
            dto.setExamDate(startDate.plusDays(random.nextInt(1, durationDays + 1)));
            dto.setJustification(randomText(EXAM_JUSTIFICATIONS, random));
        }

        return rentalRequestService.createRequest(dto, user.getUsername());
    }

    private String randomText(String[] values, ThreadLocalRandom random) {
        return values[random.nextInt(values.length)];
    }
}
