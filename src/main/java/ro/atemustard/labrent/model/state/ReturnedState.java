package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

public class ReturnedState implements EquipmentState {

    @Override
    public void makeAvailable(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.AVAILABLE);
    }

    @Override
    public void sendToService(Equipment equipment) {
        equipment.setStatus(EquipmentStatus.IN_SERVICE);
    }
}
