package ro.atemustard.labrent.repository;

/*
 * ==========================================================================
 * RENTAL REQUEST REPOSITORY
 * ==========================================================================
 *
 * Acest repository are query-uri mai complexe pentru ca RentalRequest
 * leaga User de Equipment — cautam dupa user ID, equipment ID, status, etc.
 *
 * CONCEPT NOU: Query pe relatii (@ManyToOne)
 * ------------------------------------------
 * Cand ai un camp 'user' de tip User cu @ManyToOne, Spring stie sa caute
 * dupa campurile user-ului folosind underscore sau camelCase:
 *
 *   findByUserId(Long userId)
 *   → Spring parseaza: find + By + User + Id
 *   → Genereaza: SELECT * FROM rental_requests WHERE user_id = ?
 *
 *   findByUserIdAndStatus(Long userId, RequestStatus status)
 *   → SELECT * FROM rental_requests WHERE user_id = ? AND status = ?
 *
 * Spring stie ca 'user' e un @ManyToOne si ca 'id' e campul @Id al lui User,
 * deci traduce automat in user_id (coloana FK din tabel).
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import org.springframework.data.jpa.repository.JpaRepository;
//   import ro.atemustard.labrent.model.RentalRequest;
//   import ro.atemustard.labrent.model.RequestStatus;
//   import java.util.List;

public interface RentalRequestRepository /* TODO 1: extends JpaRepository<RentalRequest, Long> */ {

    // TODO 2: Toate cererile unui user:
    //   List<RentalRequest> findByUserId(Long userId);
    //
    //   Spring genereaza: SELECT * FROM rental_requests WHERE user_id = ?
    //   Folosim: pagina "Cererile mele" — clientul vede toate cererile lui.

    // TODO 3: Toate cererile pentru un echipament:
    //   List<RentalRequest> findByEquipmentId(Long equipmentId);
    //
    //   Folosim: admin-ul vede cine a cerut un anumit echipament.

    // TODO 4: Toate cererile cu un anumit status:
    //   List<RentalRequest> findByStatus(RequestStatus status);
    //
    //   Folosim: admin-ul vede toate cererile PENDING (de aprobat).

    // TODO 5: Cererile unui user cu un anumit status:
    //   List<RentalRequest> findByUserIdAndStatus(Long userId, RequestStatus status);
    //
    //   Combinatie — Spring parseaza: find + By + UserId + And + Status
    //   Genereaza: SELECT * FROM rental_requests WHERE user_id = ? AND status = ?
    //   Folosim: "Arata-mi doar cererile mele aprobate"
}
