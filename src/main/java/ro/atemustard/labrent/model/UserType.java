package ro.atemustard.labrent.model;

/**
 * Tipul utilizatorului — setat la înregistrare.
 *
 * Folosit de sistemul de prioritizare inteligentă:
 * - STUDENT poate adăuga urgență academică (examen, dată examen)
 * - NON_STUDENT nu beneficiază de bonusul academic
 */
public enum UserType {
    STUDENT,
    NON_STUDENT
}
