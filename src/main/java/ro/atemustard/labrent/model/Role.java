package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * CE ESTE UN ENUM?
 * ==========================================================================
 *
 * Un enum in Java e similar cu 'enum class' din C++.
 * Defineste un set FIX de valori posibile — nu poti crea altele la runtime.
 *
 * De ce folosim enum pentru roluri in loc de String?
 *   - Daca ai String, cineva poate scrie "STUDNET" (typo) si nu primesti eroare.
 *   - Cu enum, compilatorul te opreste — Role.STUDNET nu exista, eroare la compilare.
 *   - Autocomplete in IDE — scrii "Role." si vezi toate optiunile.
 *
 * Cum se salveaza in baza de date?
 *   - Pe entitatea User, vom pune @Enumerated(EnumType.STRING)
 *   - Asta inseamna ca in coloana din MySQL va aparea textul "STUDENT", "PROFESSOR", "ADMIN"
 *   - Alternativa (EnumType.ORDINAL) ar salva 0, 1, 2 — DAR daca reordonezi enum-ul,
 *     toate datele din DB se strica! De aceea MEREU folosim STRING.
 *
 * ==========================================================================
 */
public enum Role {

    STUDENT,    // Student — poate face cereri de inchiriere, vede propriile inchirieri
    PROFESSOR,  // Profesor — aceleasi drepturi ca studentul + acces prioritar
    ADMIN       // Sales Agent / Staff — gestioneaza tot (cereri, echipamente, stoc, useri)
}
