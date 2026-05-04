package ro.atemustard.labrent.individual.membru1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.dto.RentalRequestDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.EquipmentService;
import ro.atemustard.labrent.service.NotificationService;
import ro.atemustard.labrent.service.PrioritizationService;
import ro.atemustard.labrent.service.RentalRequestService;
import ro.atemustard.labrent.service.UserService;
import ro.atemustard.labrent.service.factory.AcademicRentalRequestFactory;
import ro.atemustard.labrent.service.factory.RentalRequestFactory;
import ro.atemustard.labrent.service.factory.StandardRentalRequestFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Individual JUnit test — Membru 1.
 *
 * Caz de utilizare: «Client submitting a rental request».
 *
 * Diagrama de activități corespunzătoare descrie pașii:
 *   [Start]
 *     → autentificare implicită (username known)
 *     → verifică disponibilitate stoc      (decision)
 *     → verifică validitate interval date  (decision)
 *     → alege fabrica (Standard / Academic) (decision: student && isForExam)
 *     → construiește RentalRequest (Builder polimorfic)
 *     → persistă, apoi calculează priorityScore
 *     → recalculează scoruri concurenți
 *     → notifică observatori (email)
 *   [End]
 *
 * Diagrama UML adițională (individuală, Partea II): SECVENȚĂ.
 *
 * Acoperirea testului (cerințele 9 + 10 din barem):
 * - bucla decision pe stoc      → caz „no stock" → excepție
 * - bucla decision pe date      → caz „end ≤ start" → excepție
 * - ramura factory standard     → student fără exam, nonStudent (chiar cu exam)
 * - ramura factory academic     → student cu exam → AcademicRentalRequest
 * - efecte secundare            → recalculate competing queue + notify observers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Activity: Submit Rental Request — parameterized scenarios")
class SubmitRentalRequestActivityTest {

    @Mock private RentalRequestRepository rentalRequestRepository;
    @Mock private UserService userService;
    @Mock private EquipmentService equipmentService;
    @Mock private PrioritizationService prioritizationService;
    @Mock private NotificationService notificationService;

    private RentalRequestService service;
    private User student;
    private User nonStudent;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        Map<String, RentalRequestFactory> factories = Map.of(
                "standardFactory", new StandardRentalRequestFactory(),
                "academicFactory", new AcademicRentalRequestFactory()
        );
        service = new RentalRequestService(rentalRequestRepository, userService,
                equipmentService, prioritizationService, notificationService, factories);

        student = new User("alice", "alice@x.com", "p", Role.USER, UserType.STUDENT);
        student.setId(1L);
        student.setReputationScore(100.0);

        nonStudent = new User("bob", "bob@x.com", "p", Role.USER, UserType.NON_STUDENT);
        nonStudent.setId(2L);
        nonStudent.setReputationScore(100.0);

        equipment = new Equipment("Tektronix TBS2000", "scope", "instruments", 3);
        equipment.setId(10L);
        equipment.setAvailableQuantity(3);
    }

    // ---------- Happy path: factory selection branches ----------

    private static Stream<Arguments> factorySelectionCases() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                // userType, isForExam, examDate, expectedSubclassName
                Arguments.of("STUDENT",     true,  today.plusDays(7),  AcademicRentalRequest.class),
                Arguments.of("STUDENT",     false, null,               StandardRentalRequest.class),
                Arguments.of("NON_STUDENT", true,  today.plusDays(7),  StandardRentalRequest.class),
                Arguments.of("NON_STUDENT", false, null,               StandardRentalRequest.class)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} isForExam={1} → {3}")
    @MethodSource("factorySelectionCases")
    void factorySelectionMatchesActivityDiagramDecision(
            String userType, boolean isForExam, LocalDate examDate, Class<?> expectedSubclass) {

        User actor = "STUDENT".equals(userType) ? student : nonStudent;
        when(userService.findEntityByUsername(actor.getUsername())).thenReturn(actor);
        when(equipmentService.findEntityById(10L)).thenReturn(equipment);
        when(rentalRequestRepository.save(any(RentalRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(prioritizationService.calculatePriority(any())).thenReturn(80.0);

        RentalRequestCreateDTO dto = baseDto();
        dto.setIsForExam(isForExam);
        dto.setExamDate(examDate);

        RentalRequestDTO out = service.createRequest(dto, actor.getUsername());

        // The persisted entity must be the expected concrete subclass.
        org.mockito.ArgumentCaptor<RentalRequest> captor =
                org.mockito.ArgumentCaptor.forClass(RentalRequest.class);
        verify(rentalRequestRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(expectedSubclass);

        // Activity-diagram side-effects must always fire.
        assertThat(out.getPriorityScore()).isEqualTo(80.0);
        verify(prioritizationService).recalculateForEquipment(10L);
        verify(notificationService).notifyRequestCreated(any());
    }

    // ---------- Decision: stock available ----------

    @ParameterizedTest(name = "[{index}] availableQuantity={0} → expectException={1}")
    @MethodSource("stockDecisionCases")
    void stockDecisionGate(int availableQuantity, boolean expectException) {
        equipment.setAvailableQuantity(availableQuantity);
        when(userService.findEntityByUsername("alice")).thenReturn(student);
        when(equipmentService.findEntityById(10L)).thenReturn(equipment);
        if (!expectException) {
            when(rentalRequestRepository.save(any(RentalRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(prioritizationService.calculatePriority(any())).thenReturn(50.0);
        }

        if (expectException) {
            assertThatThrownBy(() -> service.createRequest(baseDto(), "alice"))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("No available units");
            verify(rentalRequestRepository, never()).save(any());
            verify(notificationService, never()).notifyRequestCreated(any());
        } else {
            assertThat(service.createRequest(baseDto(), "alice")).isNotNull();
        }
    }

    private static Stream<Arguments> stockDecisionCases() {
        return Stream.of(
                Arguments.of(0, true),
                Arguments.of(1, false),
                Arguments.of(5, false)
        );
    }

    // ---------- Decision: dates valid ----------

    @ParameterizedTest(name = "[{index}] start={0}, end={1} → expectException={2}")
    @MethodSource("dateDecisionCases")
    void dateRangeDecisionGate(int startDelta, int endDelta, boolean expectException) {
        when(userService.findEntityByUsername("alice")).thenReturn(student);
        when(equipmentService.findEntityById(10L)).thenReturn(equipment);
        if (!expectException) {
            when(rentalRequestRepository.save(any(RentalRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(prioritizationService.calculatePriority(any())).thenReturn(50.0);
        }

        RentalRequestCreateDTO dto = baseDto();
        dto.setStartDate(LocalDate.now().plusDays(startDelta));
        dto.setEndDate(LocalDate.now().plusDays(endDelta));

        if (expectException) {
            assertThatThrownBy(() -> service.createRequest(dto, "alice"))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("End date must be after start date");
        } else {
            assertThat(service.createRequest(dto, "alice")).isNotNull();
        }
    }

    private static Stream<Arguments> dateDecisionCases() {
        return Stream.of(
                Arguments.of(1, 5, false),  // valid
                Arguments.of(1, 1, true),   // end == start → invalid
                Arguments.of(5, 1, true)    // end < start  → invalid
        );
    }

    // ---------- Helpers ----------

    private RentalRequestCreateDTO baseDto() {
        RentalRequestCreateDTO dto = new RentalRequestCreateDTO();
        dto.setEquipmentId(10L);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        return dto;
    }
}
