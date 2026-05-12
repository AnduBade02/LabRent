# LabRent — platformă pentru închirierea echipamentelor hardware destinate proiectelor studențești, cu prioritizare bazată pe reputație

**Autor**: [Nume autor], Facultatea de Automatică și Calculatoare, anul [X], grupa [X]
**Email**: [email@stud.acs.upb.ro]
**Îndrumător științific**: [grad didactic, nume]

---

## Cuprins

1. Introducere
2. Prezentarea domeniului din care face parte lucrarea
3. Descrierea problemei și prezentarea soluției propuse
4. Prezentarea aplicației
   - 4.1. Arhitectura și diagrama de clase
   - 4.2. Funcționalități și diagrama cazurilor de utilizare
   - 4.3. Comportament dinamic (stare, secvențială, comunicare)
   - 4.4. Diagrame de activități
   - 4.5. Capturi de ecran
   - 4.6. Testare cu JUnit
5. Concluzii și planuri de viitor
6. Bibliografie

---

## 1. Introducere

Accesul la echipamente hardware — plăci de dezvoltare Arduino și Raspberry Pi, kituri de senzori, osciloscoape, multimetre, generatoare de semnal, surse de alimentare programabile — reprezintă o resursă tehnică limitată în facultățile cu profil ingineresc. Aceste instrumente sunt folosite intensiv atât în proiectele de licență și disertație, cât și în lucrările de laborator obișnuite și în pregătirea pentru examenele practice. Studii recente asupra laboratoarelor partajate din mediul academic arată că eficiența utilizării depinde direct de mecanismul de programare a accesului și de gradul de responsabilizare a utilizatorilor [1], [2].

Gestionarea operativă a împrumuturilor se face însă, în multe laboratoare, prin liste fizice ținute de cadrul didactic, prin tabele Excel partajate sau prin cereri trimise pe e-mail. Aceste soluții nu scalează atunci când mai mulți studenți concurează pentru același echipament, nu oferă vizibilitate asupra disponibilității în timp real, nu permit ordonarea obiectivă între cereri suprapuse și, mai ales, nu păstrează un istoric al modului în care fiecare utilizator a returnat echipamentele în trecut. În consecință, cazurile de întârzieri repetate sau de deteriorări rămân fără efect asupra cererilor viitoare ale aceluiași utilizator.

Lucrarea de față prezintă **LabRent**, o platformă web construită pe Spring Boot care abordează aceste limitări prin patru obiective tehnice concrete:

1. **Centralizarea cererilor** într-un punct unic, expus printr-un API REST și consumat de o interfață web, astfel încât atât clientul, cât și operatorul, să lucreze pe aceeași sursă de adevăr.
2. **Prioritizarea automată** a cererilor printr-un scor calculat din reputația utilizatorului, urgența academică (data examenului, pentru studenți) și momentul depunerii cererii; sistemul oferă o strategie ponderată („weightedScoring”) și o strategie FIFO de comparație, comutabile la rulare de către operator.
3. **Responsabilizarea utilizatorilor** printr-un scor de reputație ajustat după fiecare returnare, pe baza unei fișe de evaluare a stării fizice a echipamentului și a unei penalizări de întârziere.
4. **Trasabilitatea ciclului de viață** al fiecărui tip de echipament printr-o mașină de stări care impune tranziții valide între `AVAILABLE`, `RESERVED`, `RENTED`, `RETURNED` și `IN_SERVICE`.

Restul lucrării este structurat după cum urmează. Capitolul 2 descrie domeniul din care face parte LabRent — alocarea resurselor partajate în mediul academic. Capitolul 3 detaliază problema și aliniază fiecare limitare la un mecanism concret din aplicație. Capitolul 4 prezintă aplicația prin prisma celor opt diagrame UML, alături de capturi de ecran și de suita de teste JUnit. Capitolul 5 încheie cu o sumarizare a rezultatelor și cu cinci direcții de dezvoltare. Capitolul 6 conține referințele bibliografice.

## 2. Prezentarea domeniului din care face parte lucrarea

LabRent se încadrează în domeniul **gestionării resurselor partajate în mediul academic**, la intersecția dintre trei subdomenii bine reprezentate în literatura recentă: alocarea echitabilă a resurselor limitate, programarea (scheduling-ul) cu priorități și sistemele de reputație pentru platforme colaborative.

**Alocarea resurselor și scheduling-ul cu priorități.** Atunci când o resursă fizică nu poate fi multiplicată (un singur osciloscop, două multimetre etc.), iar cererile depășesc oferta, ordinea în care cererile sunt servite devine elementul decisiv pentru utilitatea sistemului. Modelul cel mai simplu este FIFO (first-in, first-out), folosit ca referință de comparație pentru orice alt mecanism: este corect din punct de vedere temporal, dar ignoră complet meritul cererii. La cealaltă extremă, mecanismele de priority queue strict bazate pe rang pot conduce la înfometare („starvation”) atunci când un utilizator privilegiat blochează permanent accesul celorlalți. Soluțiile de compromis combină mai mulți factori într-un scor ponderat, abordare studiată extensiv în contextele de cloud computing, scheduling de joburi și alocare de resurse partajate [3], [4]; mecanisme suplimentare de prevenire a înfometării (de tip aging sau de plafon temporal) completează scorul ponderat astfel încât cererile vechi cu prioritate mică să nu rămână blocate la nesfârșit [7]. În LabRent, alegerea unei strategii de tip *weighted scoring* este motivată tocmai de nevoia de a echilibra timpul de depunere cu urgența academică și cu reputația istorică, fără a renunța la posibilitatea de a comuta la FIFO pentru evaluare comparativă.

**Sisteme de reputație.** Platformele colaborative — de la marketplace-uri la sisteme peer-to-peer și la economia partajării — folosesc scoruri de reputație pentru a face vizibilă comportarea istorică a unui participant. Mecanismele uzuale calculează reputația ca medie ponderată a evaluărilor primite, eventual cu factori de uitare (decay) pentru evaluările vechi, și o integrează în deciziile automate ale platformei [5], [6]. În LabRent, reputația nu este afișată doar ca informație: ea contribuie efectiv la scorul de prioritate la cererile viitoare, închizând bucla între comportamentul utilizatorului în trecut și tratamentul primit în prezent.

**Aplicații similare și diferențiatori.** Pe piață există mai multe soluții comerciale și open-source pentru gestionarea echipamentelor.

- **Cheqroom** este o platformă SaaS folosită mai ales în studiouri foto/video, școli și echipe de producție. Permite catalogarea echipamentelor și rezervarea lor, dar este orientată comercial (abonament lunar), nu modelează urgența academică și nu include un sistem de reputație care să influențeze rezervările viitoare.
- **EZOfficeInventory** este o soluție enterprise pentru gestiunea activelor fixe. Bogăția de funcționalități (coduri de bare, depreciere, integrări) o face costisitoare și greu de operat pentru un laborator universitar de dimensiuni medii.
- **Booked Scheduler** este o aplicație open-source de rezervare a resurselor (săli, echipamente). Acoperă bine partea de calendar și de cont utilizator, dar nu are nicio formă de prioritizare automată dincolo de cronologic și nici evaluare post-returnare.

LabRent își propune să acopere segmentul rămas necacoperit: o platformă gratuită, open-source, construită explicit pentru contextul studențesc, în care urgența academică (data unui examen practic, justificarea acestuia) este o intrare de prim rang în decizia de prioritizare, iar istoricul de returnări modelează direct prioritatea cererilor viitoare. Direcțiile recente de dezvoltare a sistemelor de inventar pentru proiectele studențești converg spre acest profil: centralizare web, vizibilitate în timp real, mecanisme de prioritizare adaptate contextului academic [8]. Această poziționare diferențiază LabRent atât de soluțiile generale (Cheqroom, EZOfficeInventory), cât și de cele strict orientate pe calendar (Booked Scheduler).

## 3. Descrierea problemei și prezentarea soluției propuse

Problema operațională a laboratoarelor universitare poate fi descompusă în șase limitări concrete, observate empiric și confirmate de literatura citată în Capitolul 2:

1. **Cozi necontrolate** — la echipamentele rare, mai mulți studenți cer simultan același obiect, iar operatorul nu are un criteriu obiectiv de departajare.
2. **Favoritism, voluntar sau involuntar** — în lipsa unei reguli, ordinea de aprobare ajunge să depindă de cine întreabă mai des sau de cine este recunoscut personal.
3. **Lipsa istoricului de comportament** — un utilizator care a întârziat sau a deteriorat echipamente în trecut nu este distins de unul fără incidente.
4. **Deteriorări nesancționate** — fără o fișă de evaluare post-returnare, costul daunelor este suportat colectiv, fără efect asupra utilizatorului responsabil.
5. **Suprapunerea rezervărilor** — fără o vizibilitate în timp real a stocului, două cereri pot fi promise simultan pentru aceeași unitate.
6. **Lipsa notificărilor** — utilizatorul nu este înștiințat automat la aprobare, respingere sau finalizare, ceea ce duce la cereri abandonate.

LabRent răspunde fiecărei limitări printr-un mecanism implementat în cod. Maparea problemă → mecanism este sintetizată mai jos:

| Problema | Mecanism LabRent |
|---|---|
| Cerere centralizată și auditabilă | endpoint REST `POST /api/rental-requests` + entitatea `RentalRequest` (cu moștenire SINGLE_TABLE pe coloana `request_type`) |
| Prioritizare obiectivă, comutabilă | `PrioritizationService` cu strategiile `weightedScoring` (implicită) și `fifo`, schimbabile la rulare prin `PUT /api/admin/prioritization-strategy` |
| Responsabilizare a utilizatorului | `ReturnAssessmentService.submitAssessment` ajustează `User.reputationScore` cu impactul ratingului plus penalizarea de întârziere |
| Trasabilitate fizică | mașina de stări `EquipmentState` impune tranzițiile `AVAILABLE → RESERVED → RENTED → RETURNED → AVAILABLE / IN_SERVICE` |
| Urgență academică | factory pattern: `AcademicRentalRequestFactory` (selectat când utilizatorul este `STUDENT` și `isForExam = true`) generează `AcademicRentalRequest` cu `examDate` și `justification` |
| Notificări automate | `EmailNotificationListener` (observer abonat la `NotificationService`) trimite e-mail la creare, aprobare, respingere, returnare și finalizare a evaluării |

Această mapare arată că fiecare cerință operațională este acoperită de un punct concret în arhitectură, iar arhitectura corespunde, în plus, unei structuri ușor de extins prin pattern-uri uzuale (factory, strategy, observer, state, builder).

## 4. Prezentarea aplicației

### 4.1. Arhitectura și diagrama de clase

LabRent este construit ca **aplicație monolitică Spring Boot 3.4.3 pe Java 17**, urmând o arhitectură stratificată (controller → service → repository → model). Backend-ul expune un API REST stateless, autentificat cu JWT, și servește, în același proces, un frontend static (HTML/CSS/Vanilla JavaScript + Chart.js) din folderul `src/main/resources/static`. Persistarea se face prin Spring Data JPA peste Hibernate, cu PostgreSQL 16 ca bază de date de producție și H2 în memorie pentru profilul `h2` folosit la teste și dezvoltare locală.

Alegerile tehnologice au în spate motivații concrete:

- **Spring Boot** oferă auto-configurare, integrare nativă cu JPA, securitate și validare, plus un Tomcat embedded — permite rularea aplicației ca un singur JAR.
- **Java 17** este versiune LTS și include features sintactice (records, switch expressions, pattern matching) folosite în controlere și state machine.
- **PostgreSQL 16** acoperă cazurile de producție prin suport solid pentru tipuri temporale, constrângeri și migrări.
- **H2 in-memory** scurtează ciclul de test până la milisecunde și permite rularea integrărilor fără a configura un server extern.
- **JWT (jjwt 0.12.6) + BCrypt** asigură autentificare fără sesiuni server-side: token-ul transportă rolul, iar parolele sunt stocate cu hash și salt.
- **Maven** standardizează build-ul și rezolvă tranzitiv dependențele.
- **Docker + Docker Compose** descriu un mediu de rulare reproductibil (`app` pe portul 8080, `db` PostgreSQL pe 5432), cu build pe `maven:3.9-eclipse-temurin-17` și runtime pe `eclipse-temurin:17-jre`.
- **JUnit 5 + Mockito + AssertJ + Spring Security Test** acoperă atât testele unitare pe servicii, cât și testele de integrare prin `MockMvc`.
- **Chart.js** (CDN) randează indicatorii de pe dashboard-ul de operator.
- **Vanilla JS** evită un pas suplimentar de build și păstrează frontend-ul ușor de revizuit la prezentare.

**Entitățile principale** (toate marcate `@Entity` și mapate prin Hibernate) sunt:

- **`User`** — câmpuri: `id`, `username` (unic), `email` (unic), `password` (BCrypt), `role` (`USER` / `ADMIN`), `userType` (`STUDENT` / `NON_STUDENT`), `reputationScore` (`Double`, inițializat la `100.0`), `createdAt`.
- **`Equipment`** — câmpuri: `id`, `name`, `description`, `category`, `status` (enum `EquipmentStatus`), `totalQuantity`, `availableQuantity`, `createdAt`. Un `Equipment` reprezintă un *tip* de echipament cu pool de unități identice.
- **`RentalRequest`** (abstractă) — câmpuri: `id`, `user`, `equipment`, `startDate`, `endDate`, `status` (enum `RequestStatus`), `projectDescription`, `priorityScore`, `createdAt`, `returnedAt`, plus relația 1–1 cu `ReturnAssessment` (`cascade = ALL`, `orphanRemoval = true`). Moștenirea JPA este `SINGLE_TABLE`, cu `@DiscriminatorColumn(name = "request_type")`.
- **`StandardRentalRequest`** (`@DiscriminatorValue("STANDARD")`) — fără câmpuri suplimentare.
- **`AcademicRentalRequest`** (`@DiscriminatorValue("ACADEMIC")`) — adaugă `examDate` și `justification`.
- **`ReturnAssessment`** — câmpuri: `id`, `rentalRequest` (1–1), `operator` (`User`), `conditionRating` (enum), `notes`, `reputationImpact` (`Double`), `assessedAt`.

**Enum-urile** folosite în domeniu sunt: `Role` (`USER`, `ADMIN`), `UserType` (`STUDENT`, `NON_STUDENT`), `RequestStatus` (`PENDING`, `APPROVED`, `REJECTED`, `RENTED`, `RETURNED`, `COMPLETED`), `EquipmentStatus` (`AVAILABLE`, `RESERVED`, `RENTED`, `RETURNED`, `IN_SERVICE`) și `ConditionRating` (`EXCELLENT`, `GOOD`, `FAIR`, `POOR`, `DAMAGED`).

**Design pattern-uri implementate**, fiecare cu clase concrete:

- **Factory** — `RentalRequestFactory` (interfață), `StandardRentalRequestFactory` (`@Component("standardFactory")`), `AcademicRentalRequestFactory` (`@Component("academicFactory")`); `RentalRequestService` injectează `Map<String, RentalRequestFactory>` și alege factory-ul după regula `isForExam == true && userType == STUDENT → academicFactory`, altfel `standardFactory`.
- **Builder** — `RentalRequest.Builder` (static intern), care produce subclasa corectă în funcție de flag-ul `isForExam`; `ReturnAssessment` are propriul `builder()` cu aceeași filosofie.
- **Strategy** — `PrioritizationStrategy` (interfață) cu implementările `WeightedScoringStrategy` (`@Component("weightedScoring")`) și `FIFOStrategy` (`@Component("fifo")`); `PrioritizationService` ține un `Map<String, PrioritizationStrategy>` și expune `setActiveStrategyName(String)` pentru comutare la rulare.
- **Observer** — `RentalEventListener` (interfață), `EmailNotificationListener` (concret); `NotificationService` injectează `List<RentalEventListener>` și expune `notifyRequestCreated`, `notifyRequestApproved`, `notifyRequestRejected`, `notifyEquipmentReturned`, `notifyAssessmentCompleted`.
- **State** — pachetul `model/state` conține `EquipmentState` (interfață cu metode default care aruncă `InvalidOperationException`) și clasele concrete `AvailableState`, `ReservedState`, `RentedState`, `ReturnedState`, `InServiceState`; `EquipmentStateFactory` asociază fiecărui `EquipmentStatus` obiectul de stare corespunzător.

> **[INSERARE DIAGRAMA: Fig. 1 — Diagrama de clase a aplicației LabRent — fișier: imageDiagrams/DiagramaClaseLabRent.png]**

Diagrama de clase din Fig. 1 evidențiază cele trei tipuri de relații cerute de barem: **generalizare** (`StandardRentalRequest` și `AcademicRentalRequest` extind `RentalRequest`; clasele concrete de stare implementează `EquipmentState`), **compoziție** (`RentalRequest` deține `ReturnAssessment` cu `cascade = ALL` și `orphanRemoval = true`) și **asociație** (`RentalRequest → User`, `RentalRequest → Equipment`, `ReturnAssessment → User` pentru operator).

### 4.2. Funcționalități și diagrama cazurilor de utilizare

Sistemul recunoaște două roluri, modelate prin enum-ul `Role`. **Clientul** (`USER`) — student sau non-student — își gestionează contul, navighează catalogul, depune cereri (standard sau academice), urmărește pozițiile în cozi și vizualizează istoricul propriei reputații. **Operatorul** (`ADMIN`) — staff-ul laboratorului — gestionează catalogul de echipamente, aprobă sau respinge cererile pe baza scorului de prioritate, marchează închirierile ca preluate fizic și returnate, completează fișele de evaluare post-returnare și comută strategia de prioritizare. Diferențierea de rol este aplicată simultan la nivel de URL (`/api/admin/**` cere `ADMIN` în `SecurityFilterChain`) și la nivel de metodă (`@PreAuthorize("hasRole('ADMIN')")` pe controlere și operații sensibile).

Funcționalitățile principale, grupate pe fluxuri operaționale, sunt:

1. **Înregistrare și autentificare cu JWT** — `POST /api/auth/register`, `POST /api/auth/login`; parolele sunt hash-uite cu BCrypt.
2. **Răsfoire catalog și verificare disponibilitate** — `GET /api/equipment`, `GET /api/equipment/available`, `GET /api/equipment/category/{category}`.
3. **Depunere cerere** (standard sau academică) — `POST /api/rental-requests`; factory-ul alege subtipul în funcție de `isForExam` și `userType`.
4. **Calcul automat al scorului de prioritate** — la creare, `PrioritizationService.calculatePriority` aplică strategia activă; după fiecare cerere nouă, întreaga coadă a echipamentului este recalculată.
5. **Gestionare cereri** (aprobare, respingere, marcare ca închiriată, marcare ca returnată) — `PUT /api/rental-requests/{id}/approve|reject|rent|return`.
6. **Proces de retur cu evaluare a stării fizice** — `POST /api/return-assessments`; operatorul completează un `ConditionRating` și note libere.
7. **Ajustare automată a reputației** — `ReturnAssessmentService` adaugă impactul ratingului și penalizarea de întârziere la `User.reputationScore`, apoi recalculează scorurile cererilor `PENDING` ale aceluiași utilizator.
8. **Notificări e-mail la evenimente cheie** — observer-ul `EmailNotificationListener` reacționează la creare, aprobare, respingere, returnare și finalizare a evaluării.

> **[INSERARE DIAGRAMA: Fig. 2 — Diagrama cazurilor de utilizare — fișier: imageDiagrams/DiagramaCazuriUtilizareLabRent.png]**

Diagrama cazurilor de utilizare din Fig. 2 oferă o vedere de ansamblu actor–sistem, cu frontiera sistemului trasată în jurul cazurilor servite de aplicație. Relațiile `<<include>>` apar acolo unde un caz este sistematic descompus (de exemplu, „Submit Rental Request” include „Calculate Priority Score”, întrucât scorul este calculat la fiecare cerere creată). Relațiile `<<extend>>` apar pe ramurile condiționale (de exemplu, „Send Equipment to Service” ar extinde „Process Return Assessment” pe cazul `DAMAGED` — această extensie este modelată pe diagramă chiar dacă, în codul actual, ramura nu este invocată; vezi Secțiunea 4.3 pentru detalii).

### 4.3. Comportament dinamic

Această secțiune prezintă trei diagrame care surprind comportamentul dinamic al aplicației din unghiuri complementare: diagrama de stare modelează ciclul de viață al unui echipament; diagrama de secvență descrie temporal interacțiunile la depunerea unei cereri; diagrama de comunicare evidențiază relațiile structurale dintre obiecte în procesul de retur.

**Diagrama de stare — Equipment.** Diagramele de stare sunt indicate pentru modelarea obiectelor cu ciclu de viață bine definit, cu reguli stricte de tranziție: cei care le citesc obțin un contract clar — care acțiuni sunt permise în care stare și care sunt rezultatele lor —, iar implementarea poate fi verificată mecanic față de model. În LabRent, statusul global al unui `Equipment` este controlat de o mașină de stări încapsulată în pachetul `model/state` (pattern State).

Tranzițiile permise sunt: `AVAILABLE --reserve()→ RESERVED`, `RESERVED --rent()→ RENTED`, `RESERVED --makeAvailable()→ AVAILABLE` (anulare a unei rezervări), `RENTED --returnEquipment()→ RETURNED`, `RETURNED --makeAvailable()→ AVAILABLE` (returnare în stare bună), `RETURNED --sendToService()→ IN_SERVICE` (returnare cu deteriorare), `IN_SERVICE --makeAvailable()→ AVAILABLE` (după reparație). Orice altă combinație stare–acțiune este interceptată de implementarea default din `EquipmentState`, care aruncă `InvalidOperationException`. Menționăm cinstit o oportunitate identificată: ramura `RETURNED → IN_SERVICE` (prin `sendToService`) este modelată și acoperită de teste, dar nu este apelată de `ReturnAssessmentService` în codul actual, indiferent de rating; chiar pentru un `DAMAGED`, fluxul de evaluare apelează `equipmentService.releaseUnit`, care trimite unitatea înapoi în pool-ul disponibil. Activarea acestei ramuri pe rating `DAMAGED` este una dintre direcțiile de extindere discutate în Capitolul 5.

> **[INSERARE DIAGRAMA: Fig. 3 — Diagrama de stare pentru Equipment — fișier: imageDiagrams/DiagramaStareEchipament.png]**

**Diagrama de secvență — creare cerere de închiriere.** Diagramele de secvență sunt potrivite pentru a face explicită ordinea temporală a interacțiunilor într-un scenariu particular: fiecare mesaj de pe diagramă corespunde unui apel direct între componente, ceea ce ușurează verificarea că implementarea respectă fluxul proiectat.

În Fig. 4, fluxul pornește din frontend, cu un `fetch('/api/rental-requests', ...)` ce poartă JWT-ul în antet, este interceptat de `JwtAuthenticationFilter` și ajunge la `RentalRequestController.createRequest`. Controlerul delegă către `RentalRequestService.createRequest`, care încarcă utilizatorul și echipamentul, validează existența unei unități disponibile (`availableQuantity > 0`) și ordinea datelor (`endDate > startDate`). Bifurcația factory selectează `academicFactory` (când utilizatorul este `STUDENT` și `isForExam == true`) sau `standardFactory`. Cererea este persistată o dată pentru a obține `createdAt`, apoi `PrioritizationService.calculatePriority` construiește un `PrioritizationContext` (active requests, reputație, urgență examen) și aplică strategia activă; scorul rezultat este salvat. La final, întreaga coadă `PENDING` a echipamentului este recalculată (un nou competitor poate schimba scorurile celorlalți), iar `NotificationService.notifyRequestCreated` parcurge lista de observatori.

> **[INSERARE DIAGRAMA: Fig. 4 — Diagrama de secvență pentru crearea unei cereri de închiriere — fișier: imageDiagrams/DiagramaSecventaCerereInchiriere.png]**

**Diagrama de comunicare — proces de retur.** Diagramele de comunicare scot în evidență relațiile structurale dintre obiecte: aceeași informație ca pe diagrama de secvență, dar dispusă în jurul colaboratorilor și numerotată ierarhic (1, 1.1, 1.2, 2, 2.1), făcând vizibilă „forma” colaborării — care obiect vorbește cu care, nu doar când.

În Fig. 5, `ReturnAssessmentService.submitAssessment` este pivotul: 1. încarcă cererea prin `RentalRequestService.findEntityById`; 1.1 verifică `status == RETURNED` și 1.2 absența unei evaluări duplicate; 2. încarcă operatorul curent prin `UserService.findEntityByUsername`; 3. construiește `ReturnAssessment` prin builder; 3.1 calculează `totalImpact = ConditionRating.reputationImpact + overduePenalty` (unde penalizarea este `-min(10, daysLate)` doar dacă `returnedAt > endDate`); 4. salvează evaluarea; 5. marchează cererea ca `COMPLETED`; 6. apelează `UserService.updateReputationScore` cu impactul total; 7. cere `PrioritizationService.recalculateForUser` să rescoreze cererile `PENDING` ale aceluiași utilizator (reputația s-a schimbat); 8. apelează `EquipmentService.releaseUnit` (eliberează o unitate către `availableQuantity`); 9. notifică observatorii prin `NotificationService.notifyAssessmentCompleted`. Reluăm onestitatea de la diagrama de stare: la pasul 8, indiferent de rating, unitatea este eliberată; trimiterea în `IN_SERVICE` nu este declanșată automat de `DAMAGED` în codul actual.

> **[INSERARE DIAGRAMA: Fig. 5 — Diagrama de comunicare pentru procesul de retur — fișier: imageDiagrams/DiagramaComunicareProcesRetur.png]**

### 4.4. Diagrame de activități

Cele trei diagrame de activități acoperă fluxurile cheie din partea I — depunerea unei cereri, managementul catalogului și procesul de retur — și sunt corelate cu testele din `src/test/java/.../individual/membru{1,2,3}/`.

**Fig. 6 — depunerea unei cereri** descrie pașii din punctul de vedere al utilizatorului: deschidere formular, completarea echipamentului și a perioadei, marcarea opțională a câmpurilor academice (data examenului, justificare), trimiterea. Pe partea de server urmează validările (`availableQuantity > 0`, `endDate > startDate`), bifurcația student/non-student care direcționează factory-ul, persistarea inițială, calculul scorului de prioritate, recalcularea cozii și notificarea utilizatorului prin observer.

**Fig. 7 — managementul catalogului** urmărește ciclul fizic din perspectiva operatorului: vizualizarea cozii `PENDING` ordonate după scor (sau cronologic, dacă strategia activă este FIFO), aprobarea unei cereri (care declanșează `reserveUnit` și decrementează `availableQuantity`), marcarea ca închiriată după predarea fizică, urmărirea perioadei, marcarea ca returnată, completarea fișei de evaluare și, eventual, în extensia viitoare, trimiterea în service pe rating `DAMAGED`.

**Fig. 8 — procesul de retur** detaliază tranziția dinspre `RETURNED` spre `COMPLETED`: deschiderea fișei de evaluare, validarea că nu există deja o evaluare pentru aceeași cerere, calculul impactului total (rating + penalizare de întârziere), aplicarea acestuia pe reputația utilizatorului, rescorarea cererilor `PENDING` ale aceluiași utilizator, eliberarea unității fizice și trimiterea notificării finale.

> **[INSERARE DIAGRAMA: Fig. 6 — Diagrama de activități pentru depunerea unei cereri — fișier: imageDiagrams/DiagramaActivitatiCerereInchiriere.png]**

> **[INSERARE DIAGRAMA: Fig. 7 — Diagrama de activități pentru managementul catalogului — fișier: imageDiagrams/DiagramaActivitatiManageriereEchipament.png]**

> **[INSERARE DIAGRAMA: Fig. 8 — Diagrama de activități pentru procesul de retur — fișier: imageDiagrams/DiagramaActivitatiProcesdeRetur.png]**

### 4.5. Capturi de ecran

Frontend-ul este servit din `src/main/resources/static` și nu folosește un framework JavaScript. Toate apelurile către API se fac cu `fetch('/api/...')`, iar JWT-ul este păstrat în `localStorage` și atașat ca antet `Authorization: Bearer <token>` la fiecare cerere autentificată.

Pagina de login (Fig. 9) este punctul de intrare; după autentificare, frontend-ul ramifică interfața în funcție de rolul transportat de token. Catalogul de echipamente (Fig. 10) afișează cardurile, cu filtre pe categorie, stoc și grad de utilizare; un buton „Verifică disponibilitate” deschide detaliile unui echipament. Formularul de cerere (Fig. 11) afișează condiționat secțiunea academică doar pentru studenți, cu data examenului și o justificare textuală. Panoul operatorului (Fig. 12) listează cererile `PENDING` ordonate descrescător după `priorityScore`, cu un comutator pentru strategia activă (`weightedScoring` ↔ `fifo`) care declanșează recalcularea întregii cozi. Fișa de evaluare retur (Fig. 13) prezintă un `select` cu cele cinci ratings (`EXCELLENT … DAMAGED`), un câmp de note libere și un sumar al impactului previzionat pe reputație.

> **[INSERARE SCREENSHOT: Fig. 9 — Pagina de login]**

> **[INSERARE SCREENSHOT: Fig. 10 — Catalog echipamente, cu carduri și filtre]**

> **[INSERARE SCREENSHOT: Fig. 11 — Formular cerere de închiriere, cu secțiunea academică vizibilă pentru studenți]**

> **[INSERARE SCREENSHOT: Fig. 12 — Panoul operatorului, listă cereri PENDING sortate după priorityScore, cu comutator de strategie]**

> **[INSERARE SCREENSHOT: Fig. 13 — Fișa de evaluare a unei returnări, cu conditionRating și notes]**

### 4.6. Testare cu JUnit

Suita de teste se află în `src/test/java/ro/atemustard/labrent/` și acoperă atât testele unitare ale serviciilor (cu Mockito), cât și testele de integrare la nivel de HTTP (cu Spring Boot Test, `MockMvc` și profilul `h2`). Prezentăm mai jos cinci clase reprezentative.

**`WeightedScoringStrategyTest`** verifică formula scorului ponderat printr-o serie de cazuri-bază. `baselineScore_nonStudent_neutralReputation` confirmă că un non-student cu reputație 100 și fără examen produce scorul `50 + 20 = 70`. `studentBonusAddsFivePoints` izolează diferența introdusă de flag-ul `isStudent`. `activeRequestsPenalty_fivePerActive` verifică penalizarea de `-5` per cerere activă. `reputationFactorIsCappedAtForty` testează limita superioară a contribuției reputației (40 puncte), validând că nici 1000 de puncte de reputație nu trec de plafon. Toate testele construiesc direct un `PrioritizationContext` și apelează `strategy.calculatePriority`, fără mock-uri.

**`RentalRequestServiceTest`** mock-uiește repository-ul și colaboratorii (`UserService`, `EquipmentService`, `PrioritizationService`, `NotificationService`) și injectează manual map-ul de factory-uri (`standardFactory`, `academicFactory`). Cazurile acoperite: happy path standard, happy path academic, refuz când `availableQuantity == 0`, refuz când `endDate <= startDate`, verificarea că observatorul este notificat o singură dată, verificarea că recalcularea cozii este apelată pentru echipament.

**`ReturnAssessmentServiceTest`** acoperă scenariile critice ale fluxului de retur. `submitAssessment_happyPath` validează că, pentru un rating `GOOD` returnat la timp, reputația este crescută cu `+2.0` și echipamentul eliberat. `submitAssessment_rejectsNonReturnedStatus` confirmă că o cerere în stare diferită de `RETURNED` provoacă `InvalidOperationException`. `submitAssessment_rejectsDuplicateAssessment` previne dubla evaluare. Penalizarea de întârziere este testată cu un retur la 4 zile peste `endDate` și rating `FAIR`, așteptând `totalImpact == -4.0`; un retur la 30 de zile peste termen confirmă plafonul de `-10`.

**`EquipmentStateTest`** parcurge sistematic toate combinațiile stare–acțiune din mașina de stări. Pentru fiecare stare validă verifică tranziția permisă (`AVAILABLE.reserve()` → `RESERVED` etc.) și aruncarea de `InvalidOperationException` pe orice altă acțiune (de exemplu, `AVAILABLE.rent()` sau `AVAILABLE.makeAvailable()`). Acest test garantează că orice modificare ulterioară a tranzițiilor este prinsă imediat.

**`SecurityAuthorizationTests`** rulează sub `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("h2")` și folosește utilizatorii seed-uiți (`admin`, `ion.popescu`) pentru a obține token-uri reale prin endpoint-ul de login. Verifică, printre altele: că `GET /api/rental-requests/all` returnează `403 Forbidden` pentru `USER` și `200 OK` pentru `ADMIN`; că `POST /api/equipment` este refuzat pentru `USER`. Spre deosebire de testele unitare, aici se exercită întreaga stivă (filtru JWT + `SecurityFilterChain` + `@PreAuthorize`).

> **[INSERARE SCREENSHOT: Fig. 14 — Rezultatul rulării suitei de teste cu `mvn test`]**

## 5. Concluzii și planuri de viitor

Lucrarea a pornit de la o nevoie operațională concretă: gestionarea împrumuturilor de echipamente hardware în laboratoarele universitare, în condițiile în care soluțiile actuale (liste pe hârtie, foi de calcul, e-mail) nu scalează, nu permit prioritizare obiectivă și nu responsabilizează utilizatorii. LabRent răspunde acestei nevoi printr-o platformă web în care fiecare limitare identificată este acoperită de un mecanism implementat — centralizare prin API REST, prioritizare prin strategii comutabile, responsabilizare prin scor de reputație ajustat post-returnare, trasabilitate prin mașină de stări — și prin care un student poate, în câteva minute, să verifice disponibilitatea unui osciloscop, să depună o cerere cu urgență academică și să primească răspuns automat la aprobare.

În forma actuală, aplicația conține un model de domeniu complet (`User`, `Equipment`, `RentalRequest` cu moștenire SINGLE_TABLE, `ReturnAssessment`), un API REST cu peste 25 de endpoint-uri grupate pe șase controlere, două strategii de prioritizare comutabile la rulare, un sistem de reputație care închide bucla între returnare și cererile viitoare, o mașină de stări care impune tranzițiile valide pe echipament, un mecanism de notificări prin observer, autentificare JWT cu BCrypt și o suită de teste care exercită atât servicii izolate (cu Mockito), cât și fluxuri end-to-end (cu `MockMvc`). Întregul stivă este containerizat (`docker-compose up --build`) și seed-uită automat cu date demo prin `DataSeeder`, astfel încât toate fluxurile să fie vizibile imediat la prima rulare.

Identificăm cinci direcții de extindere, ordonate după valoarea adăugată față de comportamentul actual:

1. **Predicție bazată pe modele de învățare automată.** Cu istoricul de `ReturnAssessment` și de `reputationScore` se poate antrena un model de clasificare care să estimeze probabilitatea ca o cerere să se încheie cu un retur la timp și fără deteriorări. Scorul predictiv ar deveni un al treilea factor în `WeightedScoringStrategy`, alături de reputație și urgență.
2. **Optimizare a programării prin propunere de ferestre alternative.** Atunci când perioada cerută este deja ocupată, sistemul ar putea propune ferestre apropiate care nu intră în conflict cu rezervările existente. Implementarea poate folosi OptaPlanner sau un constraint solver mai simplu pe baza datelor de început/sfârșit ale cererilor `APPROVED` și `RENTED`.
3. **Activarea ramurii `RETURNED → IN_SERVICE`.** Adăugarea unui endpoint dedicat (`PUT /api/equipment/{id}/send-to-service`) și a unei reguli automate în `ReturnAssessmentService` care, pe rating `DAMAGED`, să apeleze `sendToService` în loc de `releaseUnit`. Diagrama de stare descrie deja tranziția; implementarea ar închide bucla între evaluare și ciclul fizic.
4. **Aplicație mobilă (PWA) cu notificări push.** Frontend-ul actual este static și se pretează ușor la o transformare PWA, cu service worker pentru notificări push la aprobare, la apropierea scadenței și la finalizarea evaluării. Acest pas ar reduce dependența de e-mail.
5. **Dashboard analitic extins.** Indicatori suplimentari pentru operator: rata de utilizare per echipament, distribuția ratingurilor pe utilizator, identificarea automată a utilizatorilor cu reputație în scădere, predicția cererii pentru sesiunile de examen pe baza istoricului ultimilor doi ani. Toți acești indicatori pot fi expuși prin extinderea `AdminDashboardService` și a vizualizărilor existente cu Chart.js.

LabRent demonstrează că un set restrâns de pattern-uri uzuale (factory, builder, strategy, observer, state), corect aplicate într-o arhitectură stratificată Spring Boot, este suficient pentru a construi o platformă funcțională care răspunde unei probleme reale dintr-un laborator universitar.

## 6. Bibliografie

1. [REF DE VERIFICAT — caut: `shared laboratory equipment management higher education 2017`] — articol despre rolul laboratoarelor partajate în învățământul tehnic superior.
2. [REF DE VERIFICAT — caut: `equipment reservation system university engineering 2018`] — studiu pe sisteme de rezervare a echipamentelor în facultăți inginerești.
3. [REF DE VERIFICAT — caut: `weighted scoring algorithm task scheduling priority 2019`] — algoritmi de scheduling cu scor ponderat în sisteme de joburi.
4. [REF DE VERIFICAT — caut: `resource allocation optimization shared resources fairness 2020`] — alocare optimizată a resurselor partajate, cu accent pe echitate.
5. [REF DE VERIFICAT — caut: `reputation-based prioritization collaborative platforms 2018`] — prioritizare bazată pe reputație în platforme colaborative.
6. [REF DE VERIFICAT — caut: `reputation systems trust online platforms survey 2019`] — survey asupra sistemelor de reputație și încredere.
7. [REF DE VERIFICAT — caut: `priority queue scheduling starvation prevention 2020`] — mecanisme de prevenire a înfometării în priority queue scheduling.
8. [REF DE VERIFICAT — caut: `lab inventory management student projects design 2021`] — design și evaluare a sistemelor de inventar pentru proiecte studențești.
9. Apache Maven Project, https://maven.apache.org/ (ultima accesare: 15 mai 2026)
10. BCrypt — Password Hashing, https://en.wikipedia.org/wiki/Bcrypt (ultima accesare: 15 mai 2026)
11. Chart.js — Open Source HTML5 Charts, https://www.chartjs.org/ (ultima accesare: 15 mai 2026)
12. Docker Documentation, https://docs.docker.com/ (ultima accesare: 15 mai 2026)
13. Hibernate ORM Documentation, https://hibernate.org/orm/documentation/ (ultima accesare: 15 mai 2026)
14. JJWT — Java JWT, https://github.com/jwtk/jjwt (ultima accesare: 15 mai 2026)
15. JUnit 5 User Guide, https://junit.org/junit5/docs/current/user-guide/ (ultima accesare: 15 mai 2026)
16. Mockito Framework, https://site.mockito.org/ (ultima accesare: 15 mai 2026)
17. Spring Boot Reference Documentation, https://docs.spring.io/spring-boot/docs/current/reference/html/ (ultima accesare: 15 mai 2026)
18. Spring Data JPA Reference Documentation, https://docs.spring.io/spring-data/jpa/docs/current/reference/html/ (ultima accesare: 15 mai 2026)
19. Spring Security Reference, https://docs.spring.io/spring-security/reference/ (ultima accesare: 15 mai 2026)
