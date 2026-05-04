package ro.atemustard.labrent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.dto.RentalRequestDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.factory.AcademicRentalRequestFactory;
import ro.atemustard.labrent.service.factory.RentalRequestFactory;
import ro.atemustard.labrent.service.factory.StandardRentalRequestFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalRequestServiceTest {

    @Mock private RentalRequestRepository rentalRequestRepository;
    @Mock private UserService userService;
    @Mock private EquipmentService equipmentService;
    @Mock private PrioritizationService prioritizationService;
    @Mock private NotificationService notificationService;

    private RentalRequestService service;
    private User user;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        Map<String, RentalRequestFactory> factories = Map.of(
                "standardFactory", new StandardRentalRequestFactory(),
                "academicFactory", new AcademicRentalRequestFactory()
        );
        service = new RentalRequestService(rentalRequestRepository, userService,
                equipmentService, prioritizationService, notificationService, factories);

        user = new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT);
        user.setId(1L);
        equipment = new Equipment("Scope", "d", "cat", 3);
        equipment.setId(2L);
        equipment.setAvailableQuantity(3);
    }

    private RentalRequestCreateDTO createDto(boolean exam) {
        RentalRequestCreateDTO dto = new RentalRequestCreateDTO();
        dto.setEquipmentId(2L);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setIsForExam(exam);
        if (exam) {
            dto.setExamDate(LocalDate.now().plusDays(7));
        }
        return dto;
    }

    @Test
    void createRequest_studentExam_usesAcademicFactoryAndScores() {
        when(userService.findEntityByUsername("alice")).thenReturn(user);
        when(equipmentService.findEntityById(2L)).thenReturn(equipment);
        when(rentalRequestRepository.save(any(RentalRequest.class)))
                .thenAnswer(inv -> { RentalRequest r = inv.getArgument(0); r.setId(10L); return r; });
        when(prioritizationService.calculatePriority(any())).thenReturn(123.0);

        RentalRequestDTO out = service.createRequest(createDto(true), "alice");

        assertThat(out.getIsForExam()).isTrue();
        assertThat(out.getPriorityScore()).isEqualTo(123.0);
        verify(prioritizationService).recalculateForEquipment(2L);
        verify(notificationService).notifyRequestCreated(any());
    }

    @Test
    void createRequest_nonStudentSetExam_stillUsesStandardFactory() {
        user.setUserType(UserType.NON_STUDENT);
        when(userService.findEntityByUsername("alice")).thenReturn(user);
        when(equipmentService.findEntityById(2L)).thenReturn(equipment);
        when(rentalRequestRepository.save(any(RentalRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(prioritizationService.calculatePriority(any())).thenReturn(70.0);

        RentalRequestDTO out = service.createRequest(createDto(true), "alice");

        // non-student → factory dropped exam fields
        assertThat(out.getIsForExam()).isFalse();
        assertThat(out.getExamDate()).isNull();
    }

    @Test
    void createRequest_noStock_throws() {
        equipment.setAvailableQuantity(0);
        when(userService.findEntityByUsername("alice")).thenReturn(user);
        when(equipmentService.findEntityById(2L)).thenReturn(equipment);

        assertThatThrownBy(() -> service.createRequest(createDto(false), "alice"))
                .isInstanceOf(InvalidOperationException.class);

        verify(rentalRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_endBeforeStart_throws() {
        when(userService.findEntityByUsername("alice")).thenReturn(user);
        when(equipmentService.findEntityById(2L)).thenReturn(equipment);
        RentalRequestCreateDTO dto = createDto(false);
        dto.setEndDate(dto.getStartDate());

        assertThatThrownBy(() -> service.createRequest(dto, "alice"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void approveRequest_pendingOnly_reservesAndNotifies() {
        RentalRequest r = pending();
        when(rentalRequestRepository.findById(10L)).thenReturn(Optional.of(r));
        when(rentalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalRequestDTO out = service.approveRequest(10L);

        assertThat(out.getStatus()).isEqualTo(RequestStatus.APPROVED.name());
        verify(equipmentService).reserveUnit(2L);
        verify(notificationService).notifyRequestApproved(any());
    }

    @Test
    void approveRequest_wrongStatus_throws() {
        RentalRequest r = pending();
        r.setStatus(RequestStatus.APPROVED);
        when(rentalRequestRepository.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.approveRequest(10L))
                .isInstanceOf(InvalidOperationException.class);
        verify(equipmentService, never()).reserveUnit(anyLong());
    }

    @Test
    void rejectRequest_setsStatusAndNotifies() {
        RentalRequest r = pending();
        when(rentalRequestRepository.findById(10L)).thenReturn(Optional.of(r));
        when(rentalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rejectRequest(10L);

        assertThat(r.getStatus()).isEqualTo(RequestStatus.REJECTED);
        verify(notificationService).notifyRequestRejected(any());
    }

    @Test
    void markAsRented_requiresApproved() {
        RentalRequest r = pending();
        r.setStatus(RequestStatus.APPROVED);
        when(rentalRequestRepository.findById(10L)).thenReturn(Optional.of(r));
        when(rentalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsRented(10L);
        assertThat(r.getStatus()).isEqualTo(RequestStatus.RENTED);
    }

    @Test
    void markAsReturned_setsReturnedAtToToday() {
        RentalRequest r = pending();
        r.setStatus(RequestStatus.RENTED);
        when(rentalRequestRepository.findById(10L)).thenReturn(Optional.of(r));
        when(rentalRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsReturned(10L);

        assertThat(r.getStatus()).isEqualTo(RequestStatus.RETURNED);
        assertThat(r.getReturnedAt()).isEqualTo(LocalDate.now());
        verify(notificationService).notifyEquipmentReturned(any());
    }

    @Test
    void getQueuePositionsForUser_returnsOneBasedPositions() {
        RentalRequest mine = pending();
        mine.setId(10L);
        RentalRequest other = pending();
        other.setId(20L);
        other.setUser(new User("bob", "b@x.com", "p", Role.USER, UserType.STUDENT));
        other.getUser().setId(99L);

        when(userService.findEntityByUsername("alice")).thenReturn(user);
        when(rentalRequestRepository.findByUserIdAndStatus(1L, RequestStatus.PENDING))
                .thenReturn(List.of(mine));
        when(prioritizationService.getPrioritizedQueue(2L)).thenReturn(List.of(other, mine));

        Map<Long, Integer> positions = service.getQueuePositionsForUser("alice");
        assertThat(positions).containsEntry(10L, 2);
    }

    private RentalRequest pending() {
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();
        r.setId(10L);
        return r;
    }
}
