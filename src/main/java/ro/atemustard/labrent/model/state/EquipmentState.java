package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.Equipment;

/**
 * Design Pattern: STATE
 *
 * Interface that defines all possible equipment transitions.
 * Each concrete state implements only the valid transitions;
 * invalid ones throw InvalidOperationException (default behaviour).
 *
 * Valid transitions:
 *   AVAILABLE  → reserve()      → RESERVED
 *   RESERVED   → rent()         → RENTED
 *   RESERVED   → makeAvailable()→ AVAILABLE  (cancel)
 *   RENTED     → returnEquip()  → RETURNED
 *   RETURNED   → makeAvailable()→ AVAILABLE  (good condition)
 *   RETURNED   → sendToService()→ IN_SERVICE (damaged)
 *   IN_SERVICE → makeAvailable()→ AVAILABLE  (repaired)
 */
public interface EquipmentState {

    default void reserve(Equipment equipment) {
        throw new InvalidOperationException("Cannot reserve equipment in state: " + equipment.getStatus());
    }

    default void rent(Equipment equipment) {
        throw new InvalidOperationException("Cannot rent equipment in state: " + equipment.getStatus());
    }

    default void returnEquipment(Equipment equipment) {
        throw new InvalidOperationException("Cannot return equipment in state: " + equipment.getStatus());
    }

    default void sendToService(Equipment equipment) {
        throw new InvalidOperationException("Cannot send to service in state: " + equipment.getStatus());
    }

    default void makeAvailable(Equipment equipment) {
        throw new InvalidOperationException("Cannot make available in state: " + equipment.getStatus());
    }
}
