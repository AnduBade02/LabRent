package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * REQUEST STATUS — Starile unei cereri de inchiriere
 * ==========================================================================
 *
 * Cand un client face o cerere de inchiriere, cererea trece prin aceste stari:
 *
 *   PENDING  →  APPROVED  →  RETURNED
 *      |
 *      └──→  REJECTED
 *
 * - PENDING:   Cererea a fost trimisa, asteapta aprobare de la admin
 * - APPROVED:  Admin-ul a aprobat cererea, clientul poate ridica echipamentul
 * - REJECTED:  Admin-ul a respins cererea (stoc insuficient, scor prea mic, etc.)
 * - RETURNED:  Clientul a returnat echipamentul
 *
 * ==========================================================================
 */
public enum RequestStatus {

    PENDING,
    APPROVED,
    REJECTED,
    RETURNED
}
