package ro.atemustard.labrent.model.state;

import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.model.Equipment;

/**
 * Design Pattern: STATE
 *
 * Interfața definește toate tranzițiile posibile ale unui echipament.
 * Fiecare stare concretă implementează doar tranzițiile valide;
 * cele invalide aruncă InvalidOperationException (comportament default).
 *
 * Tranziții valide:
 *   AVAILABLE  → reserve()      → RESERVED
 *   RESERVED   → rent()         → RENTED
 *   RESERVED   → makeAvailable()→ AVAILABLE  (cancel)
 *   RENTED     → returnEquip()  → RETURNED
 *   RETURNED   → makeAvailable()→ AVAILABLE  (stare bună)
 *   RETURNED   → sendToService()→ IN_SERVICE (deteriorat)
 *   IN_SERVICE → makeAvailable()→ AVAILABLE  (reparat)
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
