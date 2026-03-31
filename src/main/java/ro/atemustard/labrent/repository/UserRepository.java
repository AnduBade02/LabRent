package ro.atemustard.labrent.repository;

/*
 * ==========================================================================
 * CE ESTE UN REPOSITORY?
 * ==========================================================================
 *
 * Un Repository e o INTERFATA (nu o clasa!) care se ocupa de comunicarea
 * cu baza de date pentru o anumita entitate.
 *
 * Analogie C++: Imaginati-va ca cineva va scrie automat toate functiile CRUD
 * (Create, Read, Update, Delete) doar din numele metodei. Nu scrieti SQL!
 *
 * CUM FUNCTIONEAZA?
 * ----------------
 * 1. Definesti o interfata care extinde JpaRepository<Entity, IdType>
 * 2. Spring genereaza AUTOMAT implementarea la runtime (nu scrii nicio clasa!)
 * 3. Primesti GRATUIT aceste metode:
 *      - findAll()          → SELECT * FROM users
 *      - findById(Long id)  → SELECT * FROM users WHERE id = ?
 *      - save(User user)    → INSERT sau UPDATE (autodetecteaza)
 *      - deleteById(Long id)→ DELETE FROM users WHERE id = ?
 *      - count()            → SELECT COUNT(*) FROM users
 *      - existsById(Long id)→ SELECT COUNT(*) > 0 FROM users WHERE id = ?
 *
 * QUERY DERIVATION (metode custom):
 * ---------------------------------
 * Spring poate genera query-uri doar din NUMELE METODEI!
 *
 *   findByUsername(String username)
 *   → Spring parseaza: find + By + Username
 *   → Genereaza: SELECT * FROM users WHERE username = ?
 *
 *   findByRole(Role role)
 *   → SELECT * FROM users WHERE role = ?
 *
 *   existsByEmail(String email)
 *   → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *
 *   findByRoleAndPriorityScoreGreaterThan(Role role, Integer score)
 *   → SELECT * FROM users WHERE role = ? AND priority_score > ?
 *
 * Cuvinte cheie suportate: And, Or, Between, LessThan, GreaterThan,
 *   Like, OrderBy, Not, In, etc.
 *   Documentatie: cauta "Spring Data JPA query derivation" pe Google.
 *
 * Optional<User> — Ce inseamna?
 * → E echivalentul lui std::optional<User> din C++.
 * → Inseamna: "poate returna un User, sau poate fi gol (null)."
 * → Folosesti: userRepository.findByUsername("ion").orElse(null)
 *              sau: .orElseThrow(() -> new RuntimeException("User not found"))
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import org.springframework.data.jpa.repository.JpaRepository;
//   import ro.atemustard.labrent.model.User;
//   import ro.atemustard.labrent.model.Role;
//   import java.util.Optional;
//   import java.util.List;

// TODO 1: Defineste interfata:
//   public interface UserRepository extends JpaRepository<User, Long> {
//
//   JpaRepository<User, Long> inseamna:
//     - User = entitatea pe care o gestioneaza acest repository
//     - Long = tipul campului @Id din entitate
//
//   ATENTIE: e INTERFATA (interface), nu CLASA (class)!
//   Spring creaza implementarea automat — tu nu scrii nicio logica.

public interface UserRepository /* TODO: extends JpaRepository<User, Long> */ {

    // TODO 2: Metoda pentru gasirea unui user dupa username:
    //   Optional<User> findByUsername(String username);
    //
    //   Spring genereaza automat: SELECT * FROM users WHERE username = ?
    //   Returneaza Optional — poate fi gol daca username-ul nu exista.
    //   Folosim la login: cautam user-ul dupa username-ul introdus.

    // TODO 3: Metoda pentru gasirea unui user dupa email:
    //   Optional<User> findByEmail(String email);
    //
    //   Util pentru: "am uitat parola" / verificare email duplicat.

    // TODO 4: Metoda pentru verificare daca exista un username:
    //   Boolean existsByUsername(String username);
    //
    //   Spring genereaza: SELECT COUNT(*) > 0 FROM users WHERE username = ?
    //   Returneaza true/false — mai eficient decat findByUsername + verificare null.
    //   Folosim la register: verificam daca username-ul e deja ocupat.

    // TODO 5: Metoda pentru verificare daca exista un email:
    //   Boolean existsByEmail(String email);
    //
    //   La fel ca TODO 4, dar pentru email.

    // BONUS (optional, nu e necesar acum):
    //   List<User> findByRole(Role role);
    //   → Gaseste toti userii cu un anumit rol (ex: toti studentii).
    //   → Util pentru admin cand vrea sa vada lista de studenti.
}
