package ro.atemustard.labrent.model;

/**
 * Stările unei cereri de închiriere:
 *
 *   PENDING → APPROVED → RENTED → RETURNED
 *      ↓
 *   REJECTED
 *
 * PENDING  = cererea a fost trimisă, așteaptă aprobarea adminului
 * APPROVED = adminul a aprobat, echipamentul e rezervat
 * REJECTED = adminul a respins cererea
 * RENTED   = echipamentul a fost predat clientului
 * RETURNED = clientul a returnat echipamentul
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    RENTED,
    RETURNED,
    COMPLETED
}
