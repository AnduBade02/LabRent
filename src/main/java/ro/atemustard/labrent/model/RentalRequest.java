package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * ENTITATEA RENTAL REQUEST — Cererile de inchiriere
 * ==========================================================================
 *
 * Aceasta e cea mai complexa entitate din proiect, pentru ca leaga User de Equipment.
 * Un client (User) face o cerere (RentalRequest) pentru un echipament (Equipment).
 *
 * CONCEPT NOU: RELATII INTRE ENTITATI (@ManyToOne)
 * ================================================
 *
 * In C++, cand o clasa are referinta la alta clasa, faci un pointer:
 *   class RentalRequest {
 *       User* user;          // pointer catre User
 *       Equipment* equipment; // pointer catre Equipment
 *   };
 *
 * In JPA, faci exact acelasi lucru, dar JPA traduce automat in FOREIGN KEY:
 *   @ManyToOne
 *   private User user;       // JPA creaza coloana 'user_id' ca FOREIGN KEY
 *
 * @ManyToOne — Ce inseamna?
 *   → "MANY requests can belong to ONE user"
 *   → "MANY requests can reference ONE equipment"
 *   → In baza de date, asta devine o coloana FOREIGN KEY.
 *   → Cand incarci un RentalRequest, JPA incarca automat si User-ul asociat.
 *
 * @JoinColumn(name = "user_id") — Ce inseamna?
 *   → Specifica numele coloanei FOREIGN KEY din tabelul rental_request.
 *   → Fara @JoinColumn, JPA genereaza un nume automat (de obicei user_id oricum),
 *     dar e buna practica sa fii explicit.
 *
 * FetchType.LAZY vs FetchType.EAGER:
 *   → EAGER: Cand incarci un RentalRequest, JPA incarca automat si User-ul.
 *            Asta inseamna un JOIN la fiecare query — poate fi lent cu multe date.
 *   → LAZY: JPA NU incarca User-ul automat. Il incarca doar cand accesezi getUser().
 *           Mai eficient, dar trebuie sa fii atent la "LazyInitializationException"
 *           (daca accesezi user-ul dupa ce s-a inchis conexiunea la DB).
 *   → Recomandat: LAZY — e mai performant. Problemele le rezolvam cand apar.
 *
 * CONCEPT NOU: LocalDate
 * ======================
 * Java are mai multe tipuri pentru date:
 *   - LocalDate      → doar data (2024-03-15) — fara ora
 *   - LocalDateTime  → data + ora (2024-03-15T14:30:00)
 *   - Instant        → timestamp exact (pentru logging)
 *
 * Pentru start/end date ale unei inchirieri, LocalDate e perfect —
 * ne intereseaza doar ziua, nu ora exacta.
 *
 * Import: java.time.LocalDate (din Java 8+, inlocuieste vechiul java.util.Date)
 *
 * ==========================================================================
 * ATENTIE: Aceasta clasa DEPINDE de User.java si Equipment.java!
 * Asigurati-va ca acelea compileaza inainte de a lucra la aceasta.
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import jakarta.persistence.*;
//   import java.time.LocalDate;

// TODO 1: Adauga @Entity si @Table(name = "rental_requests")
public class RentalRequest {

    // TODO 2: Adauga @Id si @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO 3: Adauga relatia catre User:
    //   @ManyToOne(fetch = FetchType.LAZY)
    //   @JoinColumn(name = "user_id", nullable = false)
    //
    //   Asta creeaza o coloana 'user_id' in tabelul rental_requests
    //   care e FOREIGN KEY catre tabelul 'users'.
    //   nullable = false → fiecare cerere TREBUIE sa aiba un user asociat.
    //   fetch = FetchType.LAZY → nu incarca User-ul din DB pana nu-l accesezi explicit.
    private User user;

    // TODO 4: Adauga relatia catre Equipment:
    //   @ManyToOne(fetch = FetchType.LAZY)
    //   @JoinColumn(name = "equipment_id", nullable = false)
    //
    //   Acelasi principiu — coloana 'equipment_id' ca FK catre tabelul 'equipment'.
    private Equipment equipment;

    // TODO 5: Adauga @Column(nullable = false)
    //   Data de inceput a inchirierii.
    //   JPA stie automat sa salveze LocalDate ca DATE in MySQL.
    //   Nu ai nevoie de @Temporal (ala era pentru vechiul java.util.Date).
    private LocalDate startDate;

    // TODO 6: Adauga @Column(nullable = false)
    //   Data de sfarsit a inchirierii (deadline-ul de returnare).
    private LocalDate endDate;

    // TODO 7: Adauga @Enumerated(EnumType.STRING) si @Column(nullable = false)
    //   Status-ul cererii — PENDING, APPROVED, REJECTED, RETURNED.
    //   Cererile noi incep ca PENDING.
    //   Hint: private RequestStatus status = RequestStatus.PENDING;
    private RequestStatus status;

    // TODO 8: Adauga @Column(columnDefinition = "TEXT")
    //   Descrierea proiectului pentru care se inchiriaza echipamentul.
    //   Acest camp e important pentru sistemul de scoring (saptamana 5-6):
    //   admin-ul vede scopul proiectului si acorda bonus ascuns de prioritate.
    //   Poate fi null — nu e obligatoriu sa scrii descriere.
    private String projectDescription;

    // TODO 9: Constructor fara argumente (OBLIGATORIU pentru JPA)

    // TODO 10: Constructor cu parametri (user, equipment, startDate, endDate, projectDescription)
    //   NU include id (auto-generat) si NU include status (default PENDING).
    //
    //   Exemplu:
    //   public RentalRequest(User user, Equipment equipment, LocalDate startDate,
    //                        LocalDate endDate, String projectDescription) {
    //       this.user = user;
    //       this.equipment = equipment;
    //       this.startDate = startDate;
    //       this.endDate = endDate;
    //       this.projectDescription = projectDescription;
    //       this.status = RequestStatus.PENDING;
    //   }

    // TODO 11: Getteri si setteri pentru TOATE campurile
    //   8 campuri × 2 = 16 metode. Foloseste Generate din IDE!
}
