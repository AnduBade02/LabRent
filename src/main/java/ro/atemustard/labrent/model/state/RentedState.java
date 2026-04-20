package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

public class RentedState implements EquipmentState {

    @Override
    public void returnEquipment(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.RETURNED);
    }
}
