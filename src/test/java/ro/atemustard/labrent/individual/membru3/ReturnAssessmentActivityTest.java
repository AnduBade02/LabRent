package ro.atemustard.labrent.individual.membru3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.dto.ReturnAssessmentCreateDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.ReturnAssessmentRepository;
import ro.atemustard.labrent.service.EquipmentService;
import ro.atemustard.labrent.service.NotificationService;
import ro.atemustard.labrent.service.PrioritizationService;
import ro.atemustard.labrent.service.RentalRequestService;
import ro.atemustard.labrent.service.ReturnAssessmentService;
import ro.atemustard.labrent.service.UserService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Individual JUnit test — Membru 3.
 *
 * Caz de utilizare: «Operator submits a Return Assessment after an item is
 * returned, which updates the user reputation and releases the unit».
 *
 * Diagrama de activități corespunzătoare descrie pașii:
 *   [Start]
 *     → găsește RentalRequest (decision: există?)
 *     → status == RETURNED          (decision)
 *     → assessment deja existent?   (decision: respinge dublura)
 *     → construiește ReturnAssessment (Builder)
 *     → marchează request COMPLETED
 *     → calculează delta = ratingImpact + overduePenalty (capped at -10)
 *     → updateReputationScore (clamped la [0, 200])
 *     → recalculate priorități pentru user
 *     → release unit (Equipment availableQuantity++)
 *     → notifică observatori
 *   [End]
 *
 * Diagrama UML adițională (individuală, Partea II): COMUNICARE.
 *
 * Acoperire (cerințele 9 + 10 din barem):
 * - 5 ratings × {on time, late} = 10 cazuri parametrizate pentru delta
 * - plafon overdue penalty (-10) — limita superioară a penalizării
 * - status invalid → excepție
 * - assessment dublu → excepție
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Activity: Return Assessment & Reputation Update")
class ReturnAssessmentActivityTest {

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

    @BeforeEach
    void setUp() {
        client = new User("alice", "alice@x.com", "p", Role.USER, UserType.STUDENT);
        client.setId(1L);
        operator = new User("admin", "admin@x.com", "p", Role.ADMIN, UserType.NON_STUDENT);
        operator.setId(99L);
        equipment = new Equipment("Tektronix TBS2000", "scope", "instruments", 3);
        equipment.setId(10L);
    }

    // ---------- Reputation delta matrix ----------

    private static Stream<Arguments> ratingAndOverdueCases() {
        return Stream.of(
                // rating, daysLate, expectedDelta
                Arguments.of("EXCELLENT",  0,   5.0),
                Arguments.of("GOOD",       0,   2.0),
                Arguments.of("FAIR",       0,   0.0),
                Arguments.of("POOR",       0,  -5.0),
                Arguments.of("DAMAGED",    0, -15.0),
                Arguments.of("EXCELLENT",  3,   2.0),   //  +5 - 3
                Arguments.of("GOOD",       3,  -1.0),   //  +2 - 3
                Arguments.of("FAIR",       3,  -3.0),   //   0 - 3
                Arguments.of("POOR",       3,  -8.0),   //  -5 - 3
                Arguments.of("DAMAGED",    3, -18.0),   // -15 - 3
                Arguments.of("EXCELLENT", 50,  -5.0),   //  +5 - cap(10) = -5
                Arguments.of("DAMAGED",   50, -25.0)    // -15 - cap(10)
        );
    }

    @ParameterizedTest(name = "[{index}] rating={0}, daysLate={1} → delta={2}")
    @MethodSource("ratingAndOverdueCases")
    void delta_isRatingPlusOverdueCappedAtTen(String rating, int daysLate, double expectedDelta) {
        RentalRequest request = aReturnedRequest(daysLate);
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReturnAssessmentCreateDTO dto = new ReturnAssessmentCreateDTO();
        dto.setRentalRequestId(10L);
        dto.setConditionRating(rating);
        dto.setNotes("auto");

        service.submitAssessment(dto, "admin");

        ArgumentCaptor<Double> deltaCaptor = ArgumentCaptor.forClass(Double.class);
        verify(userService).updateReputationScore(eq(1L), deltaCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(deltaCaptor.getValue())
                .isEqualTo(expectedDelta);
    }

    // ---------- Mandatory side-effects on success ----------

    @ParameterizedTest(name = "[{index}] rating={0} triggers full activity end")
    @MethodSource("happyPathRatings")
    void successCallsAllCollaborators(String rating) {
        RentalRequest request = aReturnedRequest(0);
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L)).thenReturn(Optional.empty());
        when(userService.findEntityByUsername("admin")).thenReturn(operator);
        when(returnAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReturnAssessmentCreateDTO dto = new ReturnAssessmentCreateDTO();
        dto.setRentalRequestId(10L);
        dto.setConditionRating(rating);

        service.submitAssessment(dto, "admin");

        org.assertj.core.api.Assertions.assertThat(request.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        verify(equipmentService).releaseUnit(10L);
        verify(prioritizationService).recalculateForUser(1L);
        verify(notificationService).notifyAssessmentCompleted(any());
    }

    private static Stream<String> happyPathRatings() {
        return Stream.of("EXCELLENT", "GOOD", "FAIR", "POOR", "DAMAGED");
    }

    // ---------- Decision gates → exception branches ----------

    @ParameterizedTest(name = "[{index}] status={0} → InvalidOperationException")
    @MethodSource("nonReturnedStatuses")
    void rejectsAssessmentForNonReturnedStatus(RequestStatus status) {
        RentalRequest request = aReturnedRequest(0);
        request.setStatus(status);
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);

        ReturnAssessmentCreateDTO dto = new ReturnAssessmentCreateDTO();
        dto.setRentalRequestId(10L);
        dto.setConditionRating("FAIR");

        assertThatThrownBy(() -> service.submitAssessment(dto, "admin"))
                .isInstanceOf(InvalidOperationException.class);
        verify(returnAssessmentRepository, never()).save(any());
        verify(userService, never()).updateReputationScore(anyLong(), anyDouble());
    }

    private static Stream<RequestStatus> nonReturnedStatuses() {
        return Stream.of(RequestStatus.PENDING, RequestStatus.APPROVED,
                RequestStatus.REJECTED, RequestStatus.RENTED, RequestStatus.COMPLETED);
    }

    @ParameterizedTest(name = "[{index}] duplicate assessment → InvalidOperationException")
    @MethodSource("happyPathRatings")
    void rejectsDuplicateAssessment(String rating) {
        RentalRequest request = aReturnedRequest(0);
        when(rentalRequestService.findEntityById(10L)).thenReturn(request);
        when(returnAssessmentRepository.findByRentalRequestId(10L))
                .thenReturn(Optional.of(new ReturnAssessment()));

        ReturnAssessmentCreateDTO dto = new ReturnAssessmentCreateDTO();
        dto.setRentalRequestId(10L);
        dto.setConditionRating(rating);

        assertThatThrownBy(() -> service.submitAssessment(dto, "admin"))
                .isInstanceOf(InvalidOperationException.class);
        verify(userService, never()).updateReputationScore(anyLong(), anyDouble());
    }

    // ---------- Helpers ----------

    /**
     * Builds a RETURNED RentalRequest where {@code returnedAt = endDate +
     * daysLate}. The end date is anchored in the past so daysLate=0 means
     * exactly on time, daysLate>0 produces overdue penalty.
     */
    private RentalRequest aReturnedRequest(int daysLate) {
        LocalDate end = LocalDate.now().minusDays(5);
        RentalRequest request = RentalRequest.builder()
                .user(client).equipment(equipment)
                .startDate(end.minusDays(7))
                .endDate(end)
                .build();
        request.setId(10L);
        request.setStatus(RequestStatus.RETURNED);
        request.setReturnedAt(end.plusDays(daysLate));
        return request;
    }
}
