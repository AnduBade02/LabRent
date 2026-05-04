package ro.atemustard.labrent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.dto.ReturnAssessmentCreateDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnAssessmentServiceTest {

    @Mock private ReturnAssessmentRepository returnAssessmentRepository;
    @Mock private RentalRequestService rentalRequestService;
    @Mock private UserService userService;
    @Mock private EquipmentService equipmentService;
    @Mock private NotificationService notificationService;
    @Mock private PrioritizationService prioritizationService;

    @InjectMocks private ReturnAssessmentService service;

    private User client;
    private User operator;
    private Equipment equipment;
    private RentalRequest request;

    @BeforeEach
    void setUp() {
        client = new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT);
        client.setId(1L);
        operator = new User("admin", "ad@x.com", "p", Role.ADMIN, UserType.NON_STUDENT);
        operator.setId(99L);
        equipment = new Equipment("Scope", "d", "cat", 3);
        equipment.setId(2L);
        request = RentalRequest.builder()
                .user(client).equipment(equipment)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(2))
                .build();
        request.setId(10L);
        request.setStatus(RequestStatus.RETURNED);
        request.setReturnedAt(LocalDate.now().minusDays(2));
    }

    private ReturnAssessmentCreateDTO dto(String rating) {
        ReturnAssessmentCreateDTO d = new ReturnAssessmentCreateDTO();
        d.setRentalRequestId(10L);
        d.setConditionRating(rating);
        d.setNotes("ok");
        return d;
    }

    @Test
    void submit_excellent_marksCompletedAndAppliesRatingDelta() {
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitAssessment(dto("EXCELLENT"), "admin");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        verify(userService).updateReputationScore(eq(1L), eq(5.0));
        verify(equipmentService).releaseUnit(2L);
        verify(prioritizationService).recalculateForUser(1L);
        verify(notificationService).notifyAssessmentCompleted(any());
    }

    @Test
    void submit_overdueAddsAdditionalPenalty_cappedAtTen() {
        request.setReturnedAt(LocalDate.now()); // 2 days late vs endDate (now-2)
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitAssessment(dto("FAIR"), "admin");

        // FAIR = 0.0 + overdue penalty -2.0
        verify(userService).updateReputationScore(1L, -2.0);
    }

    @Test
    void submit_overduePenaltyCapped() {
        request.setEndDate(LocalDate.now().minusDays(50));
        request.setReturnedAt(LocalDate.now());
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitAssessment(dto("GOOD"), "admin");

        // GOOD = +2.0, penalty capped at -10.0 => -8.0
        verify(userService).updateReputationScore(1L, -8.0);
    }

    @Test
    void submit_returnedOnTime_noOverduePenalty() {
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setReturnedAt(LocalDate.now());
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitAssessment(dto("DAMAGED"), "admin");

        verify(userService).updateReputationScore(1L, -15.0);
    }

    @Test
    void submit_wrongStatus_throws() {
        request.setStatus(RequestStatus.RENTED);
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);

        assertThatThrownBy(() -> service.submitAssessment(dto("FAIR"), "admin"))
                .isInstanceOf(InvalidOperationException.class);
        verify(returnAssessmentRepository, never()).save(any());
    }

    @Test
    void submit_duplicateAssessment_throws() {
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L))
                .thenReturn(Optional.of(new ReturnAssessment()));

        assertThatThrownBy(() -> service.submitAssessment(dto("FAIR"), "admin"))
                .isInstanceOf(InvalidOperationException.class);
        verify(userService, never()).updateReputationScore(anyLong(), anyDouble());
    }
}
