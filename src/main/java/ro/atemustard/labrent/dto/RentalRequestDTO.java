package ro.atemustard.labrent.dto;

/*
 * ==========================================================================
 * RENTAL REQUEST DTO
 * ==========================================================================
 *
 * CONCEPT IMPORTANT: In DTO-uri NU punem entitati intregi!
 * ========================================================
 *
 * In entitatea RentalRequest avem:
 *   private User user;           // obiect intreg User
 *   private Equipment equipment; // obiect intreg Equipment
 *
 * In DTO punem doar ID-ul + cateva campuri utile pentru afisare:
 *   private Long userId;           // doar ID-ul
 *   private String username;       // pentru afisare in UI
 *   private Long equipmentId;      // doar ID-ul
 *   private String equipmentName;  // pentru afisare in UI
 *
 * De ce?
 *   1. Evitam dependente circulare (User → RentalRequest → User → ...)
 *   2. Reducem dimensiunea raspunsului JSON
 *   3. Frontend-ul primeste exact ce are nevoie — nimic in plus
 *
 * Cand frontend-ul trimite o CERERE NOUA, trimite doar:
 *   { equipmentId: 5, startDate: "2024-04-01", endDate: "2024-04-15", projectDescription: "..." }
 * userId-ul il luam din JWT token (userul logat) — nu il trimite frontend-ul!
 *
 * Cand backend-ul RASPUNDE cu o cerere, trimite:
 *   { id: 1, userId: 3, username: "ion", equipmentId: 5, equipmentName: "Arduino Uno",
 *     startDate: "2024-04-01", endDate: "2024-04-15", status: "PENDING", projectDescription: "..." }
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import jakarta.validation.constraints.NotBlank;
//   import jakarta.validation.constraints.NotNull;
//   import ro.atemustard.labrent.model.RentalRequest;
//   import java.time.LocalDate;

public class RentalRequestDTO {

    private Long id;

    // Informatii despre user — doar ID + username pentru afisare
    private Long userId;
    private String username;

    // Informatii despre echipament — doar ID + name pentru afisare
    // TODO 1: Adauga @NotNull(message = "Equipment ID is required")
    //   Cand clientul face o cerere, TREBUIE sa specifice ce echipament vrea.
    private Long equipmentId;
    private String equipmentName;

    // TODO 2: Adauga @NotNull(message = "Start date is required")
    private LocalDate startDate;

    // TODO 3: Adauga @NotNull(message = "End date is required")
    private LocalDate endDate;

    // Status-ul e gestionat de sistem — nu vine de la user.
    private String status;

    // Descrierea proiectului — optional, dar ajuta la scoring.
    private String projectDescription;

    // TODO 4: Constructor fara argumente

    // TODO 5: Constructor cu toti parametrii

    // TODO 6: Getteri si setteri pentru TOATE campurile
    //   9 campuri × 2 = 18 metode. Da, sunt multe — IDE-ul le genereaza instant.

    // TODO 7: Metoda statica fromEntity:
    //
    //   public static RentalRequestDTO fromEntity(RentalRequest request) {
    //       RentalRequestDTO dto = new RentalRequestDTO();
    //       dto.setId(request.getId());
    //       dto.setUserId(request.getUser().getId());
    //       dto.setUsername(request.getUser().getUsername());
    //       dto.setEquipmentId(request.getEquipment().getId());
    //       dto.setEquipmentName(request.getEquipment().getName());
    //       dto.setStartDate(request.getStartDate());
    //       dto.setEndDate(request.getEndDate());
    //       dto.setStatus(request.getStatus().name());
    //       dto.setProjectDescription(request.getProjectDescription());
    //       return dto;
    //   }
    //
    //   Observa: request.getUser().getId() — navigam prin relatia @ManyToOne.
    //   JPA incarca User-ul automat (chiar si cu LAZY, la .getUser() il incarca).
}
