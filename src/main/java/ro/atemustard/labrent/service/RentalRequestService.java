package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.dto.RentalRequestDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.factory.RentalRequestFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service central pentru cererile de închiriere.
 *
 * Coordonează: Factory (creare), PrioritizationService (scoring),
 * EquipmentService (stoc), NotificationService (Observer notifications).
 */
@Service
public class RentalRequestService {

    private final RentalRequestRepository rentalRequestRepository;
    private final UserService userService;
    private final EquipmentService equipmentService;
    private final PrioritizationService prioritizationService;
    private final NotificationService notificationService;
    private final Map<String, RentalRequestFactory> factories;

    public RentalRequestService(RentalRequestRepository rentalRequestRepository,
                                 UserService userService,
                                 EquipmentService equipmentService,
                                 PrioritizationService prioritizationService,
                                 NotificationService notificationService,
                                 Map<String, RentalRequestFactory> factories) {
        this.rentalRequestRepository = rentalRequestRepository;
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.prioritizationService = prioritizationService;
        this.notificationService = notificationService;
        this.factories = factories;
    }

    @Transactional
    public RentalRequestDTO createRequest(RentalRequestCreateDTO dto, String username) {
        User user = userService.findEntityByUsername(username);
        Equipment equipment = equipmentService.findEntityById(dto.getEquipmentId());

        if (equipment.getAvailableQuantity() <= 0) {
            throw new InvalidOperationException("No available units for: " + equipment.getName());
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new InvalidOperationException("End date must be after start date");
        }

        // Factory pattern: select factory based on request type
        String factoryKey = (Boolean.TRUE.equals(dto.getIsForExam())
                && user.getUserType() == UserType.STUDENT)
                ? "academicFactory" : "standardFactory";
        RentalRequestFactory factory = factories.get(factoryKey);
        RentalRequest request = factory.createRequest(user, equipment, dto);

        // Save first to get createdAt for priority calculation
        request = rentalRequestRepository.save(request);

        // Calculate and set priority score
        double priority = prioritizationService.calculatePriority(request);
        request.setPriorityScore(priority);
        request = rentalRequestRepository.save(request);

        // Notify observers
        notificationService.notifyRequestCreated(request);

        return RentalRequestDTO.fromEntity(request);
    }

    public List<RentalRequestDTO> getUserRequests(String username) {
        User user = userService.findEntityByUsername(username);
        return rentalRequestRepository.findByUserId(user.getId()).stream()
                .map(RentalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RentalRequestDTO> getPendingRequests() {
        return rentalRequestRepository.findByStatus(RequestStatus.PENDING).stream()
                .map(RentalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RentalRequestDTO> getAllRequests() {
        return rentalRequestRepository.findAll().stream()
                .map(RentalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RentalRequestDTO> getPrioritizedPendingRequests(Long equipmentId) {
        return prioritizationService.getPrioritizedQueue(equipmentId).stream()
                .map(RentalRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public RentalRequestDTO approveRequest(Long requestId) {
        RentalRequest request = findEntityById(requestId);
        validateStatus(request, RequestStatus.PENDING, "approve");

        equipmentService.reserveUnit(request.getEquipment().getId());
        request.setStatus(RequestStatus.APPROVED);
        request = rentalRequestRepository.save(request);

        notificationService.notifyRequestApproved(request);
        return RentalRequestDTO.fromEntity(request);
    }

    @Transactional
    public RentalRequestDTO rejectRequest(Long requestId) {
        RentalRequest request = findEntityById(requestId);
        validateStatus(request, RequestStatus.PENDING, "reject");

        request.setStatus(RequestStatus.REJECTED);
        request = rentalRequestRepository.save(request);

        notificationService.notifyRequestRejected(request);
        return RentalRequestDTO.fromEntity(request);
    }

    @Transactional
    public RentalRequestDTO markAsRented(Long requestId) {
        RentalRequest request = findEntityById(requestId);
        validateStatus(request, RequestStatus.APPROVED, "mark as rented");

        request.setStatus(RequestStatus.RENTED);
        request = rentalRequestRepository.save(request);
        return RentalRequestDTO.fromEntity(request);
    }

    @Transactional
    public RentalRequestDTO markAsReturned(Long requestId) {
        RentalRequest request = findEntityById(requestId);
        validateStatus(request, RequestStatus.RENTED, "mark as returned");

        request.setStatus(RequestStatus.RETURNED);
        request = rentalRequestRepository.save(request);

        notificationService.notifyEquipmentReturned(request);
        return RentalRequestDTO.fromEntity(request);
    }

    public RentalRequest findEntityById(Long id) {
        return rentalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RentalRequest", "id", id));
    }

    private void validateStatus(RentalRequest request, RequestStatus expected, String action) {
        if (request.getStatus() != expected) {
            throw new InvalidOperationException(
                    String.format("Cannot %s: request is %s (expected %s)",
                            action, request.getStatus(), expected));
        }
    }
}
