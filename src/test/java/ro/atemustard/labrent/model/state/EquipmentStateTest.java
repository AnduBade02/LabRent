package ro.atemustard.labrent.model.state;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentStateTest {

    private Equipment equipment(EquipmentStatus status) {
        Equipment e = new Equipment("Scope", "d", "cat", 1);
        e.setStatus(status);
        return e;
    }

    // ---------- Available ----------

    @Test
    void available_canReserve_setsReserved() {
        Equipment e = equipment(EquipmentStatus.AVAILABLE);
        new AvailableState().reserve(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RESERVED);
    }

    @Test
    void available_cannotRent() {
        Equipment e = equipment(EquipmentStatus.AVAILABLE);
        assertThatThrownBy(() -> new AvailableState().rent(e))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void available_cannotMakeAvailable() {
        Equipment e = equipment(EquipmentStatus.AVAILABLE);
        assertThatThrownBy(() -> new AvailableState().makeAvailable(e))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ---------- Reserved ----------

    @Test
    void reserved_canRent() {
        Equipment e = equipment(EquipmentStatus.RESERVED);
        new ReservedState().rent(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RENTED);
    }

    @Test
    void reserved_canMakeAvailable() {
        Equipment e = equipment(EquipmentStatus.RESERVED);
        new ReservedState().makeAvailable(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void reserved_cannotReturn() {
        Equipment e = equipment(EquipmentStatus.RESERVED);
        assertThatThrownBy(() -> new ReservedState().returnEquipment(e))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ---------- Rented ----------

    @Test
    void rented_canReturn() {
        Equipment e = equipment(EquipmentStatus.RENTED);
        new RentedState().returnEquipment(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RETURNED);
    }

    @Test
    void rented_cannotReserve() {
        Equipment e = equipment(EquipmentStatus.RENTED);
        assertThatThrownBy(() -> new RentedState().reserve(e))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ---------- Returned ----------

    @Test
    void returned_canMakeAvailable() {
        Equipment e = equipment(EquipmentStatus.RETURNED);
        new ReturnedState().makeAvailable(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void returned_canSendToService() {
        Equipment e = equipment(EquipmentStatus.RETURNED);
        new ReturnedState().sendToService(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.IN_SERVICE);
    }

    // ---------- InService ----------

    @Test
    void inService_canMakeAvailable() {
        Equipment e = equipment(EquipmentStatus.IN_SERVICE);
        new InServiceState().makeAvailable(e);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void inService_cannotRent() {
        Equipment e = equipment(EquipmentStatus.IN_SERVICE);
        assertThatThrownBy(() -> new InServiceState().rent(e))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ---------- Factory ----------

    @Test
    void factory_returnsCorrectStateForEachStatus() {
        assertThat(EquipmentStateFactory.fromStatus(EquipmentStatus.AVAILABLE))
                .isInstanceOf(AvailableState.class);
        assertThat(EquipmentStateFactory.fromStatus(EquipmentStatus.RESERVED))
                .isInstanceOf(ReservedState.class);
        assertThat(EquipmentStateFactory.fromStatus(EquipmentStatus.RENTED))
                .isInstanceOf(RentedState.class);
        assertThat(EquipmentStateFactory.fromStatus(EquipmentStatus.RETURNED))
                .isInstanceOf(ReturnedState.class);
        assertThat(EquipmentStateFactory.fromStatus(EquipmentStatus.IN_SERVICE))
                .isInstanceOf(InServiceState.class);
    }
}
