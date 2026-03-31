package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * EQUIPMENT STATUS — Ciclul de viata al unui echipament
 * ==========================================================================
 *
 * Fiecare echipament de laborator trece prin mai multe stari:
 *
 *   AVAILABLE  →  RENTED  →  RETURNED  →  IN_SERVICE (optional)
 *       ↑                                      |
 *       └──────────────────────────────────────┘
 *
 * - AVAILABLE:   Echipamentul e in stoc, disponibil pentru inchiriere
 * - RENTED:      Cineva l-a inchiriat, nu mai e disponibil
 * - RETURNED:    A fost returnat, dar inca nu a fost verificat de admin
 * - IN_SERVICE:  E la service/reparatii (daca a fost returnat cu defectiuni)
 *
 * Aceasta e baza pentru DESIGN PATTERN-ul STATE pe care il vom implementa
 * in saptamanile 5-6. Fiecare stare va avea reguli diferite:
 *   - Din AVAILABLE poti trece doar in RENTED
 *   - Din RENTED poti trece doar in RETURNED
 *   - Din RETURNED poti trece in AVAILABLE sau IN_SERVICE
 *   - Din IN_SERVICE poti trece doar in AVAILABLE
 *
 * ==========================================================================
 */
public enum EquipmentStatus {

    AVAILABLE,
    RENTED,
    RETURNED,
    IN_SERVICE
}
