package ro.atemustard.labrent.individual.membru2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;
import ro.atemustard.labrent.model.state.EquipmentState;
import ro.atemustard.labrent.model.state.EquipmentStateFactory;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Individual JUnit test — Membru 2.
 *
 * Caz de utilizare: «Equipment lifecycle — operator transitions an Equipment
 * unit through its state machine».
 *
 * Diagrama de activități corespunzătoare reflectă diagrama de stare a
 * Equipment-ului:
 *   AVAILABLE  --reserve()-->        RESERVED
 *   RESERVED   --rent()-->           RENTED
 *   RESERVED   --makeAvailable()-->  AVAILABLE   (cancellation)
 *   RENTED     --returnEquipment-->  RETURNED
 *   RETURNED   --makeAvailable()-->  AVAILABLE   (good condition)
 *   RETURNED   --sendToService()-->  IN_SERVICE  (damaged)
 *   IN_SERVICE --makeAvailable()-->  AVAILABLE   (repaired)
 *
 * Diagrama UML adițională (individuală, Partea II): STARE.
 *
 * Parametrizat pe (stareInițială, acțiune, rezultatAșteptat). Acoperă și
 * tranzițiile invalide, care trebuie să arunce {@link InvalidOperationException}
 * (limita inferioară: stare validă fără acțiune validă disponibilă).
 */
@DisplayName("Activity: Equipment Lifecycle — full state-machine coverage")
class EquipmentLifecycleActivityTest {

    /** Action keys correspond to EquipmentState methods. */
    private static final BiConsumer<EquipmentState, Equipment> RESERVE = EquipmentState::reserve;
    private static final BiConsumer<EquipmentState, Equipment> RENT = EquipmentState::rent;
    private static final BiConsumer<EquipmentState, Equipment> RETURN_EQ = EquipmentState::returnEquipment;
    private static final BiConsumer<EquipmentState, Equipment> SEND_SERVICE = EquipmentState::sendToService;
    private static final BiConsumer<EquipmentState, Equipment> MAKE_AVAIL = EquipmentState::makeAvailable;

    private Equipment equipment(EquipmentStatus status) {
        Equipment e = new Equipment("Scope", "d", "instruments", 1);
        e.setStatus(status);
        return e;
    }

    // ---------- Valid transitions ----------

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(EquipmentStatus.AVAILABLE,  RESERVE,      "reserve",      EquipmentStatus.RESERVED),
                Arguments.of(EquipmentStatus.RESERVED,   RENT,         "rent",         EquipmentStatus.RENTED),
                Arguments.of(EquipmentStatus.RESERVED,   MAKE_AVAIL,   "makeAvailable",EquipmentStatus.AVAILABLE),
                Arguments.of(EquipmentStatus.RENTED,     RETURN_EQ,    "returnEquip",  EquipmentStatus.RETURNED),
                Arguments.of(EquipmentStatus.RETURNED,   MAKE_AVAIL,   "makeAvailable",EquipmentStatus.AVAILABLE),
                Arguments.of(EquipmentStatus.RETURNED,   SEND_SERVICE, "sendToService",EquipmentStatus.IN_SERVICE),
                Arguments.of(EquipmentStatus.IN_SERVICE, MAKE_AVAIL,   "makeAvailable",EquipmentStatus.AVAILABLE)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} --{2}--> {3}")
    @MethodSource("validTransitions")
    void validTransitions_updateStatus(
            EquipmentStatus initial,
            BiConsumer<EquipmentState, Equipment> action,
            String label,
            EquipmentStatus expected) {

        Equipment e = equipment(initial);
        EquipmentState state = EquipmentStateFactory.fromStatus(initial);

        action.accept(state, e);

        assertThat(e.getStatus()).isEqualTo(expected);
    }

    // ---------- Invalid transitions ----------

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(EquipmentStatus.AVAILABLE,  RENT,         "rent"),
                Arguments.of(EquipmentStatus.AVAILABLE,  RETURN_EQ,    "returnEquip"),
                Arguments.of(EquipmentStatus.AVAILABLE,  SEND_SERVICE, "sendToService"),
                Arguments.of(EquipmentStatus.AVAILABLE,  MAKE_AVAIL,   "makeAvailable"),
                Arguments.of(EquipmentStatus.RESERVED,   RESERVE,      "reserve"),
                Arguments.of(EquipmentStatus.RESERVED,   RETURN_EQ,    "returnEquip"),
                Arguments.of(EquipmentStatus.RESERVED,   SEND_SERVICE, "sendToService"),
                Arguments.of(EquipmentStatus.RENTED,     RESERVE,      "reserve"),
                Arguments.of(EquipmentStatus.RENTED,     RENT,         "rent"),
                Arguments.of(EquipmentStatus.RENTED,     SEND_SERVICE, "sendToService"),
                Arguments.of(EquipmentStatus.RENTED,     MAKE_AVAIL,   "makeAvailable"),
                Arguments.of(EquipmentStatus.RETURNED,   RESERVE,      "reserve"),
                Arguments.of(EquipmentStatus.RETURNED,   RENT,         "rent"),
                Arguments.of(EquipmentStatus.RETURNED,   RETURN_EQ,    "returnEquip"),
                Arguments.of(EquipmentStatus.IN_SERVICE, RESERVE,      "reserve"),
                Arguments.of(EquipmentStatus.IN_SERVICE, RENT,         "rent"),
                Arguments.of(EquipmentStatus.IN_SERVICE, RETURN_EQ,    "returnEquip"),
                Arguments.of(EquipmentStatus.IN_SERVICE, SEND_SERVICE, "sendToService")
        );
    }

    @ParameterizedTest(name = "[{index}] {0} --{2}--> InvalidOperationException")
    @MethodSource("invalidTransitions")
    void invalidTransitions_throw(
            EquipmentStatus initial,
            BiConsumer<EquipmentState, Equipment> action,
            String label) {

        Equipment e = equipment(initial);
        EquipmentState state = EquipmentStateFactory.fromStatus(initial);

        assertThatThrownBy(() -> action.accept(state, e))
                .isInstanceOf(InvalidOperationException.class);

        // Status must remain unchanged after a failed transition.
        assertThat(e.getStatus()).isEqualTo(initial);
    }

    // ---------- End-to-end full lifecycle ----------

    @Test
    @DisplayName("Full happy-path lifecycle: AVAILABLE → RESERVED → RENTED → RETURNED → AVAILABLE")
    void fullHappyPathLifecycle() {
        Equipment e = equipment(EquipmentStatus.AVAILABLE);

        EquipmentStateFactory.fromStatus(e.getStatus()).reserve(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RESERVED);

        EquipmentStateFactory.fromStatus(e.getStatus()).rent(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RENTED);

        EquipmentStateFactory.fromStatus(e.getStatus()).returnEquipment(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RETURNED);

        EquipmentStateFactory.fromStatus(e.getStatus()).makeAvailable(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Damaged-return branch: AVAILABLE → … → RETURNED → IN_SERVICE → AVAILABLE")
    void damagedReturnBranch() {
        Equipment e = equipment(EquipmentStatus.AVAILABLE);

        EquipmentStateFactory.fromStatus(e.getStatus()).reserve(e);
        EquipmentStateFactory.fromStatus(e.getStatus()).rent(e);
        EquipmentStateFactory.fromStatus(e.getStatus()).returnEquipment(e);
        EquipmentStateFactory.fromStatus(e.getStatus()).sendToService(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.IN_SERVICE);

        EquipmentStateFactory.fromStatus(e.getStatus()).makeAvailable(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }
}
