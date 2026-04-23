package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.dto.AdminDashboardDTO;
import ro.atemustard.labrent.dto.UserSummaryDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.repository.EquipmentRepository;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.repository.UserRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates counts and ratios for the admin dashboard.
 */
@Service
public class AdminDashboardService {

    private static final int TOP_USERS_LIMIT = 5;

    private final RentalRequestRepository rentalRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public AdminDashboardService(RentalRequestRepository rentalRequestRepository,
                                 EquipmentRepository equipmentRepository,
                                 UserRepository userRepository) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
    }

    public AdminDashboardDTO getStats() {
        AdminDashboardDTO dto = new AdminDashboardDTO();

        List<RentalRequest> allRequests = rentalRequestRepository.findAll();
        List<Equipment> allEquipment = equipmentRepository.findAll();
        LocalDate today = LocalDate.now();

        long pending = allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        long active = allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.RENTED).count();
        long overdue = allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.RENTED
                        && r.getEndDate() != null
                        && r.getEndDate().isBefore(today))
                .count();

        dto.setPendingCount(pending);
        dto.setActiveRentalsCount(active);
        dto.setOverdueCount(overdue);
        dto.setTotalEquipment((long) allEquipment.size());

        int totalUnits = allEquipment.stream()
                .mapToInt(e -> e.getTotalQuantity() != null ? e.getTotalQuantity() : 0)
                .sum();
        int availableUnits = allEquipment.stream()
                .mapToInt(e -> e.getAvailableQuantity() != null ? e.getAvailableQuantity() : 0)
                .sum();
        int rentedUnits = Math.max(0, totalUnits - availableUnits);
        double utilization = totalUnits == 0 ? 0.0
                : ((double) rentedUnits / totalUnits) * 100.0;
        dto.setUtilizationPct(Math.round(utilization * 10.0) / 10.0);

        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (RequestStatus status : RequestStatus.values()) {
            statusDist.put(status.name(), 0L);
        }
        for (RentalRequest r : allRequests) {
            statusDist.merge(r.getStatus().name(), 1L, Long::sum);
        }
        dto.setStatusDistribution(statusDist);

        List<UserSummaryDTO> topUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        User::getReputationScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_USERS_LIMIT)
                .map(UserSummaryDTO::fromEntity)
                .collect(Collectors.toList());
        dto.setTopUsers(topUsers);

        Map<String, Double> perEquipment = new LinkedHashMap<>();
        for (Equipment eq : allEquipment) {
            int total = eq.getTotalQuantity() != null ? eq.getTotalQuantity() : 0;
            int avail = eq.getAvailableQuantity() != null ? eq.getAvailableQuantity() : 0;
            double util = total == 0 ? 0.0
                    : ((double) (total - avail) / total) * 100.0;
            perEquipment.put(eq.getName(), Math.round(util * 10.0) / 10.0);
        }
        dto.setPerEquipmentUtilization(perEquipment);

        return dto;
    }
}
