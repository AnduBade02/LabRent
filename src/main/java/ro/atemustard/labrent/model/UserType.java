package ro.atemustard.labrent.model;

/**
 * User type — chosen at registration.
 *
 * Used by the intelligent prioritization system:
 * - STUDENT can add academic urgency (exam flag, exam date)
 * - NON_STUDENT does not benefit from the academic bonus
 */
public enum UserType {
    STUDENT,
    NON_STUDENT
}
