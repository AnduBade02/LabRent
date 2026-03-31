package ro.atemustard.labrent.dto;

/*
 * ==========================================================================
 * CE ESTE UN DTO (Data Transfer Object)?
 * ==========================================================================
 *
 * Un DTO e o clasa simpla care transporta date intre layere (Controller ↔ Frontend).
 * E ca un "plic" — contine doar datele necesare, nimic in plus.
 *
 * DE CE AVEM NEVOIE DE DTO-URI? (3 motive importante)
 * ====================================================
 *
 * 1. SECURITATE
 *    Entitatea User contine campul 'password' (hash-ul parolei).
 *    Daca trimitem entitatea direct ca raspuns JSON, parola ajunge in browser!
 *    DTO-ul NU are campul password — e filtrat automat.
 *
 *    Entitate User:  { id, username, email, PASSWORD, role, priorityScore }
 *    UserDTO:        { id, username, email, role, priorityScore }  ← fara parola!
 *
 * 2. DECUPLARE
 *    Daca modifici entitatea (adaugi un camp, redenumesti ceva), API-ul ramane stabil.
 *    Frontend-ul depinde de DTO, nu de entitate — schimbarile interne nu il afecteaza.
 *
 * 3. VALIDARE
 *    Pe DTO punem annotations de validare (@NotBlank, @Email, @Size).
 *    Pe entitate punem annotations JPA (@Entity, @Column).
 *    Fiecare clasa are o singura responsabilitate — asta e "Separation of Concerns".
 *
 * Analogie C++: E ca un struct simplificat pe care il trimiti prin retea.
 * Entitatea e structura completa interna; DTO-ul e ce expui public.
 *
 * ANNOTATIONS DE VALIDARE (Jakarta Validation):
 * =============================================
 * @NotBlank  → campul nu poate fi null, gol, sau doar spatii
 * @NotNull   → campul nu poate fi null (dar poate fi gol "")
 * @Email     → verifica format email (contine @, are domeniu, etc.)
 * @Size(min = 3, max = 50) → lungimea stringului trebuie sa fie intre 3 si 50
 *
 * Aceste annotations sunt verificate AUTOMAT de Spring cand primeste date de la frontend,
 * daca pui @Valid pe parametrul din Controller (vom face asta in saptamana 3-4).
 *
 * METODA fromEntity():
 * ===================
 * O metoda statica care converteste o entitate in DTO.
 * E un pattern comun — in loc sa scrii conversia in Controller de fiecare data,
 * o pui intr-un singur loc (pe DTO) si o apelezi: UserDTO.fromEntity(user).
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import jakarta.validation.constraints.Email;
//   import jakarta.validation.constraints.NotBlank;
//   import jakarta.validation.constraints.Size;
//   import ro.atemustard.labrent.model.User;

public class UserDTO {

    // NOTA: 'id' nu are validare — e generat de DB, nu vine de la user.
    private Long id;

    // TODO 1: Adauga annotations de validare:
    //   @NotBlank(message = "Username is required")
    //   @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    //
    //   @NotBlank = nu poate fi null, gol, sau doar spatii.
    //   @Size = limiteaza lungimea. 'message' = textul erorii daca validarea esueaza.
    private String username;

    // TODO 2: Adauga annotations de validare:
    //   @NotBlank(message = "Email is required")
    //   @Email(message = "Email must be valid")
    //
    //   @Email verifica automat formatul (contine @, domeniu valid, etc.)
    private String email;

    // TODO 3: Adauga annotation:
    //   @NotBlank(message = "Role is required")
    //
    //   Rolul e String in DTO (nu enum Role) — asa vine de la frontend.
    //   Vom converti String → Role in Service layer.
    private String role;

    // NOTA: priorityScore nu are validare — e calculat de sistem, nu setat de user.
    private Integer priorityScore;

    // OBSERVATIE: NU avem camp 'password' in DTO!
    // Asta e tot scopul — nu expunem parola in raspunsurile API.

    // TODO 4: Constructor fara argumente
    //   Necesar pentru deserializare JSON → obiect Java.
    //   Cand frontend-ul trimite JSON, Spring il converteste automat in DTO
    //   folosind constructorul gol + setterii.

    // TODO 5: Constructor cu toti parametrii (id, username, email, role, priorityScore)
    //   Util cand construim DTO-ul din cod (ex: in fromEntity).

    // TODO 6: Getteri si setteri pentru toate campurile
    //   5 campuri × 2 = 10 metode. Foloseste Generate din IDE.

    // TODO 7: Metoda statica fromEntity — converteste User → UserDTO
    //
    //   public static UserDTO fromEntity(User user) {
    //       UserDTO dto = new UserDTO();
    //       dto.setId(user.getId());
    //       dto.setUsername(user.getUsername());
    //       dto.setEmail(user.getEmail());
    //       dto.setRole(user.getRole().name());   // Role enum → String
    //       dto.setPriorityScore(user.getPriorityScore());
    //       return dto;
    //   }
    //
    //   .name() pe un enum returneaza textul: Role.STUDENT.name() → "STUDENT"
    //   Asa convertim enum-ul in String pentru raspunsul JSON.
}
