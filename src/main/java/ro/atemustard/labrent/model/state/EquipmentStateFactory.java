package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.EquipmentStatus;

/**
 * Mapează EquipmentStatus (enum persistat în DB) la obiectul State corespunzător.
 * Conectează domeniul persistat de cel comportamental.
 */
public class EquipmentStateFactory {

    private EquipmentStateFactory() {
    }

    public static EquipmentState fromStatus(EquipmentStatus status) {
        return switch (status) {
            case AVAILABLE -> new AvailableState();
            case RESERVED -> new ReservedState();
            case RENTED -> new RentedState();
            case RETURNED -> new ReturnedState();
            case IN_SERVICE -> new InServiceState();
        };
    }
}
