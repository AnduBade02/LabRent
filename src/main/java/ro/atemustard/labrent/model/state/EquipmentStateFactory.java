package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.model.EquipmentStatus;

/**
 * Maps EquipmentStatus (enum persisted in the DB) to its corresponding State object.
 * Bridges the persisted domain to the behavioural one.
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
