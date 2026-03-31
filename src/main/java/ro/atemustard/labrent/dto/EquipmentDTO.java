package ro.atemustard.labrent.dto;

/*
 * ==========================================================================
 * EQUIPMENT DTO
 * ==========================================================================
 *
 * Acelasi principiu ca UserDTO — transporta date despre echipament
 * fara a expune entitatea JPA direct.
 *
 * Aici nu avem un camp "secret" de ascuns (ca parola la User),
 * dar DTO-ul e totusi util pentru:
 *   - Validare input (cand admin-ul adauga echipament nou)
 *   - Decuplare (entitatea poate evolua independent de API)
 *   - Consistenta (toate entitatile au DTO — regula proiectului)
 *
 * ==========================================================================
 */

// TODO 0: Adauga importurile:
//   import jakarta.validation.constraints.NotBlank;
//   import jakarta.validation.constraints.Size;
//   import ro.atemustard.labrent.model.Equipment;

public class EquipmentDTO {

    private Long id;

    // TODO 1: Adauga validare:
    //   @NotBlank(message = "Equipment name is required")
    //   @Size(max = 100, message = "Equipment name must be at most 100 characters")
    private String name;

    // NOTA: description nu e obligatorie — nu punem @NotBlank
    private String description;

    // TODO 2: Adauga validare:
    //   @NotBlank(message = "Category is required")
    private String category;

    // Status-ul e String in DTO (nu enum EquipmentStatus).
    // Nu punem validare — status-ul e gestionat de sistem, nu setat de user.
    private String status;

    // QR code — generat de sistem, nu vine de la user.
    private String qrCode;

    // TODO 3: Constructor fara argumente

    // TODO 4: Constructor cu toti parametrii (id, name, description, category, status, qrCode)

    // TODO 5: Getteri si setteri pentru toate campurile
    //   6 campuri × 2 = 12 metode.

    // TODO 6: Metoda statica fromEntity:
    //
    //   public static EquipmentDTO fromEntity(Equipment equipment) {
    //       EquipmentDTO dto = new EquipmentDTO();
    //       dto.setId(equipment.getId());
    //       dto.setName(equipment.getName());
    //       dto.setDescription(equipment.getDescription());
    //       dto.setCategory(equipment.getCategory());
    //       dto.setStatus(equipment.getStatus().name());  // enum → String
    //       dto.setQrCode(equipment.getQrCode());
    //       return dto;
    //   }
}
