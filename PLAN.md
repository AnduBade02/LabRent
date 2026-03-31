# LabRent — Plan de dezvoltare

## Echipa
- 3 persoane, toti noi la Java/Spring Boot (background C++)
- Deadline: mai/iunie 2026

---

## Pregatire (Saptamana 0)

### Toti 3:
1. **Basics Java (2-3 ore fiecare)**
   - Clase + obiecte (similar cu C++, doar sintaxa difera)
   - Interfete (`interface`) — echivalentul clasei abstracte pure din C++
   - Annotations (`@Entity`, `@Service`, etc.) — etichete pe clase/metode
   - Getteri/Setteri (`getName()` / `setName()`)
   - Recomandare: YouTube "Java for C++ developers" sau "Java OOP basics"

2. **Instalare MySQL**
   ```bash
   brew install mysql
   brew services start mysql
   mysql -u root -e "CREATE DATABASE IF NOT EXISTS labrent;"
   ```
   Verificati ca merge: `mysql -u root -e "SHOW DATABASES;"` — trebuie sa vedeti `labrent` in lista.

3. **Clonare repo + verificare build**
   ```bash
   git clone <repo-url>
   cd labrent
   mvn clean install
   ```

---

## Saptamana 1-2 — Entitati + Repository + DTO

Fiecare persoana scrie aceeasi structura, dar pe entitati diferite.
Dupa ce terminati, comparati codul intre voi — toate 3 arata similar.

| Persoana   | Fisiere                                                                      |
|------------|------------------------------------------------------------------------------|
| Persoana A | `User.java` + `Role.java` (enum) + `UserDTO.java` + `UserRepository.java`   |
| Persoana B | `Equipment.java` + `EquipmentStatus.java` + `EquipmentDTO.java` + `EquipmentRepository.java` |
| Persoana C | `RentalRequest.java` + `RequestStatus.java` + `RentalRequestDTO.java` + `RentalRequestRepository.java` |
| Impreuna   | `application.properties` (config DB) + `SecurityConfig.java` (basic)         |

### Ce inveti in etapa asta:
- Ce e o entitate JPA (`@Entity`, `@Id`, `@Column`)
- Ce e un Repository (interfata care vorbeste cu baza de date)
- Ce e un DTO si de ce nu expui entitatea direct
- Cum genereaza Spring tabelele automat din clase Java

---

## Saptamana 3-4 — Service-uri + Controller-e + Primele pagini

| Persoana   | Fisiere                                                                        |
|------------|--------------------------------------------------------------------------------|
| Persoana A | `UserService.java` + `AuthController.java` + pagina login/register             |
| Persoana B | `EquipmentService.java` + `EquipmentController.java` + pagina echipamente      |
| Persoana C | `RentalRequestService.java` + `RentalController.java` + pagina cereri          |

### Rezultat:
Aplicatie functionala de baza — login, vezi echipamente, faci cereri de inchiriere.

### Ce inveti in etapa asta:
- Cum functioneaza un Controller (primeste HTTP request, returneaza raspuns)
- Cum leaga Spring un URL de o metoda Java (`@GetMapping`, `@PostMapping`)
- Cum functioneaza Thymeleaf (HTML cu placeholder-uri pe care Spring le completeaza)
- Fluxul complet: Browser → Controller → Service → Repository → DB si inapoi

---

## Saptamana 5-6 — Scoring (feature principal) + Observer + Exceptii

### Feature-ul principal: sistem de prioritizare stoc
Cand sunt mai multi clienti decat echipamente disponibile, un scor decide cine primeste.

**Factori de scor:**
1. Returnari cu intarziere (negativ) — vizibil clientului
2. Echipament returnat cu defectiuni (negativ) — vizibil clientului
3. Scopul/urgenta proiectului (bonus ascuns) — licenta > proiect facultate > personal
4. Returnari la timp (pozitiv) — recompenseaza comportament bun
5. Timp de asteptare (pozitiv) — fairness pentru cei refuzati repetat
6. Inchirieri active (negativ) — anti-hoarding

**Vizibilitate:** Clientul vede doar factorii negativi si cei pozitivi evidenti.
Factorii ascunsi (scop proiect) NU sunt vizibili — doar admin-ul ii vede.

| Persoana   | Fisiere                                                                     |
|------------|-----------------------------------------------------------------------------|
| Persoana A | `PriorityScoreService.java` + `ScoringStrategy.java` (interfata) + `LateReturnStrategy.java` + `DamageHistoryStrategy.java` |
| Persoana B | `GoodBehaviorStrategy.java` + `WaitTimeStrategy.java` + Observer pattern (notificari email la aprobare/respingere) |
| Persoana C | `ProjectUrgencyStrategy.java` + `ActiveRentalsStrategy.java` + `GlobalExceptionHandler.java` + exceptii custom |

### Design patterns folosite in etapa asta:
- **Strategy** — fiecare factor de scor e o strategie separata
- **Observer** — notificari la schimbare status cerere

### Ce inveti in etapa asta:
- Cum functioneaza Strategy pattern in practica
- Cum functioneaza Observer pattern
- Cum gestionezi erorile cu `@ControllerAdvice`

---

## Saptamana 7-8 — Teste + Polish + (optional) QR Code

| Persoana   | Fisiere                                                     |
|------------|-------------------------------------------------------------|
| Toti       | Fiecare scrie teste unitare (JUnit + Mockito) pentru ce a implementat |
| Toti       | Bug fixes, imbunatatiri UI, documentatie                    |
| Optional   | `QrCodeGenerator.java` (ZXing) — doar daca mai e timp      |

### Ce inveti in etapa asta:
- Cum scrii teste unitare cu JUnit 5
- Cum folosesti Mockito pentru mock-uri
- Cum pregatesti un proiect pentru prezentare

---

## Design Patterns — unde se folosesc

| Pattern      | Unde                          | Explicatie                                      |
|--------------|-------------------------------|-------------------------------------------------|
| **Strategy** | Scoring (saptamana 5-6)       | Fiecare factor de scor = o strategie separata   |
| **Observer** | Notificari (saptamana 5-6)    | Email la schimbare status cerere                |
| **State**    | Equipment lifecycle           | AVAILABLE → RENTED → RETURNED → IN_SERVICE     |
| **Builder**  | RentalRequest                 | Construire cerere complexa pas cu pas           |
| **Factory**  | Categorii echipament/cereri   | Creare obiecte diferite prin aceeasi interfata  |

---

## Structura finala a proiectului

```
ro/atemustard/labrent/
├── LabrentApplication.java
├── model/
│   ├── User.java
│   ├── Role.java
│   ├── Equipment.java
│   ├── EquipmentStatus.java
│   ├── RentalRequest.java
│   └── RequestStatus.java
├── repository/
│   ├── UserRepository.java
│   ├── EquipmentRepository.java
│   └── RentalRequestRepository.java
├── dto/
│   ├── UserDTO.java
│   ├── EquipmentDTO.java
│   └── RentalRequestDTO.java
├── service/
│   ├── UserService.java
│   ├── EquipmentService.java
│   ├── RentalRequestService.java
│   └── scoring/
│       ├── PriorityScoreService.java
│       └── strategy/
│           ├── ScoringStrategy.java
│           ├── LateReturnStrategy.java
│           ├── DamageHistoryStrategy.java
│           ├── ProjectUrgencyStrategy.java
│           ├── WaitTimeStrategy.java
│           ├── ActiveRentalsStrategy.java
│           └── GoodBehaviorStrategy.java
├── controller/
│   ├── AuthController.java
│   ├── EquipmentController.java
│   └── RentalController.java
├── config/
│   ├── SecurityConfig.java
│   └── MailConfig.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── GlobalExceptionHandler.java
└── util/
    └── QrCodeGenerator.java          (optional)
```
