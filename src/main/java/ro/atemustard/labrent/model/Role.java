package ro.atemustard.labrent.model;

/**
 * System roles — determine what actions a user can take.
 *
 * USER  = Client (student or non-student) — can create requests, view own rentals
 * ADMIN = Operator / Staff — manages requests, equipment, stock, return checks
 *
 * The student/non-student distinction is captured on the User entity via UserType,
 * NOT via separate roles. Role controls access; UserType influences prioritization.
 */
public enum Role {
    USER,
    ADMIN
}
