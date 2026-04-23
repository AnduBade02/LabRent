package ro.atemustard.labrent.model;

/**
 * Equipment lifecycle (State pattern):
 *
 *   AVAILABLE → RESERVED → RENTED → RETURNED → AVAILABLE
 *                                       ↓
 *                                   IN_SERVICE → AVAILABLE
 *
 * AVAILABLE  = in stock, available for reservation
 * RESERVED   = approved, reserved for a client (not yet picked up)
 * RENTED     = picked up by the client, no longer available
 * RETURNED   = returned, awaiting operator verification
 * IN_SERVICE = at service/repairs (returned with defects)
 */
public enum EquipmentStatus {
    AVAILABLE,
    RESERVED,
    RENTED,
    RETURNED,
    IN_SERVICE
}
