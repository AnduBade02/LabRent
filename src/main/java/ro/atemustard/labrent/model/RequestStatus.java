package ro.atemustard.labrent.model;

/**
 * States of a rental request:
 *
 *   PENDING → APPROVED → RENTED → RETURNED → COMPLETED
 *      ↓
 *   REJECTED
 *
 * PENDING   = request submitted, awaiting admin approval
 * APPROVED  = admin approved, equipment is reserved
 * REJECTED  = admin rejected the request
 * RENTED    = equipment handed over to the client
 * RETURNED  = client returned the equipment, awaiting assessment
 * COMPLETED = assessment done, reputation updated, stock released
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    RENTED,
    RETURNED,
    COMPLETED
}
