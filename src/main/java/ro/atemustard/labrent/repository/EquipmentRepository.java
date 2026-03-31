package ro.atemustard.labrent.repository;

/*
 * ==========================================================================
 * EQUIPMENT REPOSITORY
 * ==========================================================================
 *
 * Acelasi principiu ca UserRepository — interfata care extinde JpaRepository.
 * Primesti GRATUIT: findAll(), findById(), save(), deleteById(), etc.
 *
 * Metodele custom de aici sunt orientate pe CAUTARE:
 *   - Gaseste echipamente disponibile (status = AVAILABLE)
 *   - Gaseste echipamente dintr-o categorie
 *   - Gaseste echipament dupa codul QR (pentru scanare)
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import org.springframework.data.jpa.repository.JpaRepository;
//   import ro.atemustard.labrent.model.Equipment;
//   import ro.atemustard.labrent.model.EquipmentStatus;
//   import java.util.List;
//   import java.util.Optional;

public interface EquipmentRepository /* TODO 1: extends JpaRepository<Equipment, Long> */ {

    // TODO 2: Gaseste toate echipamentele cu un anumit status:
    //   List<Equipment> findByStatus(EquipmentStatus status);
    //
    //   Spring genereaza: SELECT * FROM equipment WHERE status = ?
    //   Folosim: findByStatus(EquipmentStatus.AVAILABLE) → toate echipamentele disponibile.
    //   Returneaza List (nu Optional) — pot fi 0 sau mai multe rezultate.

    // TODO 3: Gaseste echipamentele dintr-o categorie:
    //   List<Equipment> findByCategory(String category);
    //
    //   Spring genereaza: SELECT * FROM equipment WHERE category = ?
    //   Folosim: findByCategory("Oscilloscope") → toate osciloscopourile.

    // TODO 4: Gaseste un echipament dupa codul QR:
    //   Optional<Equipment> findByQrCode(String qrCode);
    //
    //   Folosim cand cineva scaneaza un QR → cautam echipamentul asociat.
    //   Optional pentru ca QR-ul scanat poate fi invalid.

    // BONUS (optional):
    //   List<Equipment> findByCategoryAndStatus(String category, EquipmentStatus status);
    //   → Echipamente disponibile dintr-o categorie specifica.
    //   → Spring parseaza: find + By + Category + And + Status → WHERE category = ? AND status = ?
}
