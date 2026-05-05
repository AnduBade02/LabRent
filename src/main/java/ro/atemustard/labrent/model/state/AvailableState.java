package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

public class AvailableState implements EquipmentState {

    @Override
    public void reserve(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.RESERVED);
    }
}
