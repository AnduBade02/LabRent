package ro.atemustard.labrent.model;

/**
 * Ciclul de viață al unui echipament (State pattern):
 *
 *   AVAILABLE → RESERVED → RENTED → RETURNED → AVAILABLE
 *                                       ↓
 *                                   IN_SERVICE → AVAILABLE
 *
 * AVAILABLE  = în stoc, disponibil pentru rezervare
 * RESERVED   = aprobat, rezervat pentru un client (echipamentul încă nu a fost ridicat)
 * RENTED     = ridicat de client, nu mai e disponibil
 * RETURNED   = returnat, în așteptare pentru verificare de către operator
 * IN_SERVICE = la service/reparații (dacă a fost returnat cu defecțiuni)
 */
public enum EquipmentStatus {
    AVAILABLE,
    RESERVED,
    RENTED,
    RETURNED,
    IN_SERVICE
}
