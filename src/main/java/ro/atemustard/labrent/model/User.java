package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * ENTITATEA USER — Prima ta clasa JPA!
 * ==========================================================================
 *
 * CE ESTE O ENTITATE JPA?
 * -----------------------
 * O entitate e o clasa Java care reprezinta un tabel din baza de date.
 * Fiecare instanta a clasei = un rand din tabel.
 * Fiecare camp al clasei = o coloana din tabel.
 *
 * Analogie C++: Gandeste-te la un struct care se salveaza automat intr-un
 * tabel SQL. Nu mai scrii INSERT/SELECT manual — JPA face totul pentru tine.
 *
 * ANNOTATIONS (adnotatii):
 * -----------------------
 * In Java, @ inaintea unei clase/camp/metode adauga "metadata" — informatii
 * suplimentare pe care framework-ul (Spring/JPA) le citeste la runtime.
 * Nu modifica logica codului, dar spun framework-ului CUM sa trateze acea clasa.
 *
 * ANNOTATIONS PE CARE LE VEI FOLOSI:
 *
 * @Entity
 *   → Spune lui JPA: "Aceasta clasa reprezinta un tabel in baza de date."
 *   → Fara @Entity, JPA ignora complet clasa.
 *
 * @Table(name = "users")
 *   → Specifica numele tabelului din MySQL.
 *   → De ce "users" si nu "user"? Pentru ca "user" e cuvant rezervat in SQL!
 *   → Daca nu pui @Table, JPA foloseste numele clasei (User) ca nume de tabel.
 *
 * @Id
 *   → Marcheaza campul ca PRIMARY KEY.
 *   → Fiecare entitate TREBUIE sa aiba exact un @Id.
 *
 * @GeneratedValue(strategy = GenerationType.IDENTITY)
 *   → AUTO_INCREMENT in MySQL — baza de date genereaza ID-ul automat.
 *   → GenerationType.IDENTITY = lasa baza de date sa gestioneze ID-ul.
 *
 * @Column(nullable = false, unique = true, length = 50)
 *   → Defineste proprietatile coloanei in tabel:
 *     - nullable = false  →  NOT NULL (campul e obligatoriu)
 *     - unique = true     →  UNIQUE constraint (nu pot exista 2 useri cu acelasi username)
 *     - length = 50       →  VARCHAR(50) (max 50 caractere)
 *   → Daca nu pui @Column, JPA creaza coloana cu valori default (nullable, VARCHAR(255)).
 *
 * @Enumerated(EnumType.STRING)
 *   → Spune lui JPA cum sa salveze enum-ul in baza de date.
 *   → EnumType.STRING = salveaza textul ("STUDENT", "ADMIN")
 *   → EnumType.ORDINAL = salveaza pozitia (0, 1, 2) — NU FOLOSI NICIODATA!
 *     De ce? Daca adaugi un nou rol intre STUDENT si PROFESSOR, toate valorile
 *     existente din DB se decaleaza si devin gresite.
 *
 * ==========================================================================
 * IMPORTANT: Dupa ce completezi TODO-urile, clasa trebuie sa compileze!
 * Ruleaza: mvn clean compile
 * ==========================================================================
 */

// TODO 0: Adauga importurile necesare. Vei avea nevoie de:
//   import jakarta.persistence.*;
//   (Aceasta importa toate annotations JPA: @Entity, @Id, @Column, etc.)
//   In Spring Boot 3.x se foloseste jakarta.persistence, NU javax.persistence!
//   (javax era in versiuni mai vechi — daca gasiti tutoriale cu javax, inlocuiti cu jakarta)

// TODO 1: Adauga @Entity deasupra clasei
//   Aceasta annotation transforma clasa intr-o entitate JPA.
//   Fara ea, Spring nu va crea tabel in baza de date pentru aceasta clasa.

// TODO 2: Adauga @Table(name = "users") deasupra clasei
//   "user" e cuvant rezervat in SQL, asa ca denumim tabelul "users".
public class User {

    // TODO 3: Adauga aceste 2 annotations deasupra campului 'id':
    //   @Id
    //   @GeneratedValue(strategy = GenerationType.IDENTITY)
    //
    //   @Id = marcheaza campul ca PRIMARY KEY
    //   @GeneratedValue = AUTO_INCREMENT — MySQL genereaza ID-ul automat
    //   Tip: folosim Long (nu int) — e conventia JPA pentru ID-uri.
    //   De ce Long si nu long? Long e clasa wrapper — poate fi null (inainte de salvare in DB, id-ul e null).
    private Long id;

    // TODO 4: Adauga @Column(nullable = false, unique = true, length = 50)
    //   nullable = false → acest camp e obligatoriu (NOT NULL in SQL)
    //   unique = true    → nu pot exista 2 useri cu acelasi username (UNIQUE in SQL)
    //   length = 50      → VARCHAR(50)
    private String username;

    // TODO 5: Adauga @Column(nullable = false, unique = true)
    //   Email-ul trebuie sa fie unic si obligatoriu.
    //   Nu punem length — default e VARCHAR(255), suficient pentru email.
    private String email;

    // TODO 6: Adauga @Column(nullable = false)
    //   Parola e obligatorie, DAR nu e unica (doi useri pot avea aceeasi parola hash-uita).
    //   IMPORTANT: Aici stocam HASH-ul parolei (generat cu BCrypt), nu parola in clar!
    //   Vom configura BCrypt in saptamana 3-4 cand facem SecurityConfig.
    private String password;

    // TODO 7: Adauga DOUA annotations:
    //   @Enumerated(EnumType.STRING)   → salveaza ca text in DB ("STUDENT", nu 0)
    //   @Column(nullable = false)      → rolul e obligatoriu
    //
    //   ATENTIE: Folositi EnumType.STRING, nu ORDINAL!
    //   ORDINAL salveaza numarul (0, 1, 2). Daca adaugi un rol nou intre cele existente,
    //   toate valorile din DB se decaleaza si devin gresite. STRING e safe.
    private Role role;

    // TODO 8: Adauga @Column(nullable = false)
    //   Scorul de prioritate — e baza sistemului de scoring din saptamanile 5-6.
    //   Fiecare user nou incepe cu 100 puncte (valoare default).
    //   Hint: poti pune valoarea default direct aici: private Integer priorityScore = 100;
    private Integer priorityScore;

    // TODO 9: Adauga un CONSTRUCTOR FARA ARGUMENTE (no-arg constructor)
    //
    //   De ce? JPA/Hibernate creaza obiecte prin REFLECTIE — citeste date din DB,
    //   creaza un obiect gol, apoi seteaza campurile cu setterii.
    //   Daca nu ai constructor fara argumente, Hibernate nu poate crea obiectul si crapa!
    //
    //   In C++, daca nu definesti niciun constructor, compilatorul genereaza unul default.
    //   In Java e la fel, DAR daca definesti un constructor CU parametri (TODO 10),
    //   cel default dispare automat. De aceea trebuie sa-l scrii explicit.
    //
    //   Exemplu:
    //   public User() {
    //   }

    // TODO 10: Adauga un CONSTRUCTOR CU PARAMETRI (username, email, password, role)
    //   Acest constructor e pentru comoditate — il folosim in cod cand cream useri noi.
    //   NU include 'id' (il genereaza DB-ul) si NU include 'priorityScore' (are default 100).
    //
    //   Exemplu:
    //   public User(String username, String email, String password, Role role) {
    //       this.username = username;
    //       this.email = email;
    //       this.password = password;
    //       this.role = role;
    //       this.priorityScore = 100;
    //   }

    // TODO 11: Adauga GETTERI si SETTERI pentru TOATE campurile (id, username, email, password, role, priorityScore)
    //
    //   In Java, conventia "JavaBean" cere:
    //     - getter: public Long getId() { return id; }
    //     - setter: public void setId(Long id) { this.id = id; }
    //
    //   JPA foloseste acesti getteri/setteri intern.
    //   Spring ii foloseste cand converteste JSON ↔ obiecte Java.
    //
    //   Shortcut IntelliJ: Click dreapta in clasa → Generate → Getter and Setter → Select All
    //   Shortcut VS Code: Click dreapta → Source Action → Generate Getters and Setters
    //
    //   Sunt 6 campuri × 2 (getter + setter) = 12 metode.
    //   Pare mult, dar IDE-ul le genereaza in 3 secunde.
}
