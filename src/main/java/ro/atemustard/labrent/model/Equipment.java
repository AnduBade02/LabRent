package ro.atemustard.labrent.model;

/*
 * ==========================================================================
 * ENTITATEA EQUIPMENT — Echipamentele de laborator
 * ==========================================================================
 *
 * Aceasta entitate reprezinta un echipament fizic din laborator
 * (osciloscop, multimetru, Arduino, etc.).
 *
 * Daca ai inteles User.java, aceasta clasa e similara.
 * Concepte NOI in acest fisier:
 *
 * @Column(columnDefinition = "TEXT")
 *   → Cand ai nevoie de mai mult de 255 caractere (default VARCHAR),
 *     folosesti columnDefinition = "TEXT" care permite texte lungi.
 *   → Util pentru 'description' — descrierea unui echipament poate fi lunga.
 *
 * Campul 'qrCode':
 *   → Fiecare echipament va avea un cod QR unic (generat cu libraria ZXing).
 *   → Deocamdata salvam doar un string unic — generarea QR vine in saptamanile 7-8.
 *   → Poate fi null initial (nu toate echipamentele au QR generat inca).
 *
 * ==========================================================================
 * ATENTIE: Aceasta clasa are aceleasi tipuri de annotations ca User.java.
 * Daca nu le intelegi, citeste mai intai comentariile din User.java!
 * ==========================================================================
 */

// TODO 0: Adauga importul:
//   import jakarta.persistence.*;

// TODO 1: Adauga @Entity si @Table(name = "equipment")
//   "equipment" nu e cuvant rezervat, dar e bine sa fim expliciti cu @Table.
public class Equipment {

    // TODO 2: Adauga @Id si @GeneratedValue(strategy = GenerationType.IDENTITY)
    //   Identic cu User — primary key auto-increment.
    private Long id;

    // TODO 3: Adauga @Column(nullable = false, length = 100)
    //   Numele echipamentului — obligatoriu, max 100 caractere.
    //   Exemple: "Osciloscop Rigol DS1054Z", "Arduino Uno R3", "Multimetru Fluke 117"
    private String name;

    // TODO 4: Adauga @Column(columnDefinition = "TEXT")
    //   Descrierea poate fi lunga, de aceea folosim TEXT in loc de VARCHAR(255).
    //   TEXT in MySQL permite pana la 65,535 caractere.
    //   Poate fi null — nu toate echipamentele au descriere detaliata.
    private String description;

    // TODO 5: Adauga @Column(nullable = false, length = 50)
    //   Categoria echipamentului — "Oscilloscope", "Microcontroller", "Multimeter", etc.
    //   Deocamdata e un simplu String. In viitor am putea face un enum sau o entitate separata.
    private String category;

    // TODO 6: Adauga @Enumerated(EnumType.STRING) si @Column(nullable = false)
    //   Status-ul echipamentului — AVAILABLE, RENTED, RETURNED, IN_SERVICE.
    //   Echipamentele noi incep ca AVAILABLE.
    //   Hint: poti pune default direct: private EquipmentStatus status = EquipmentStatus.AVAILABLE;
    private EquipmentStatus status;

    // TODO 7: Adauga @Column(unique = true)
    //   Codul QR trebuie sa fie unic (daca exista).
    //   Poate fi null — QR-ul se genereaza separat, nu la creare.
    //   Nu punem nullable = false pentru ca initial nu avem QR.
    private String qrCode;

    // TODO 8: Constructor fara argumente (OBLIGATORIU pentru JPA — vezi explicatia din User.java)

    // TODO 9: Constructor cu parametri (name, description, category)
    //   Nu include id (auto-generat), status (default AVAILABLE), qrCode (generat separat).
    //
    //   Exemplu:
    //   public Equipment(String name, String description, String category) {
    //       this.name = name;
    //       this.description = description;
    //       this.category = category;
    //       this.status = EquipmentStatus.AVAILABLE;
    //   }

    // TODO 10: Getteri si setteri pentru TOATE campurile
    //   6 campuri × 2 = 12 metode.
    //   Foloseste Generate din IDE (vezi instructiunile din User.java).
}
