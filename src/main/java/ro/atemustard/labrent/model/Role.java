package ro.atemustard.labrent.model;

/**
 * Rolurile din sistem — determină ce acțiuni poate face un utilizator.
 *
 * USER  = Client (student sau non-student) — poate crea cereri, vizualiza propriile închirieri
 * ADMIN = Operator / Staff — gestionează cereri, echipamente, stoc, verificări returnare
 *
 * Distincția student/non-student se face prin câmpul UserType de pe entitatea User,
 * NU prin roluri separate. Rolul controlează accesul; UserType influențează prioritizarea.
 */
public enum Role {
    USER,
    ADMIN
}
