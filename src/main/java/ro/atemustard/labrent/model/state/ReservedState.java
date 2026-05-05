package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

public class ReservedState implements EquipmentState {

    @Override
    public void rent(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.RENTED);
    }

    @Override
    public void makeAvailable(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.AVAILABLE);
    }
}
