# LabRent

LabRent is a full-stack laboratory equipment rental system built with Spring Boot, Spring Security, JWT authentication, PostgreSQL, and a lightweight vanilla JavaScript frontend.

The application models a real laboratory workflow: users browse equipment, submit rental requests, wait in a prioritized queue, receive approvals from operators, return equipment, and receive reputation updates after an operator assessment.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Domain Model](#domain-model)
- [Design Patterns](#design-patterns)
- [Business Workflows](#business-workflows)
- [Prioritization Model](#prioritization-model)
- [Security Model](#security-model)
- [API Reference](#api-reference)
- [Frontend](#frontend)
- [Demo Data](#demo-data)
- [Running the Project](#running-the-project)
- [Configuration](#configuration)
- [Testing](#testing)
- [Manual Smoke Test](#manual-smoke-test)
- [UML and Project Documentation](#uml-and-project-documentation)
- [Troubleshooting](#troubleshooting)

## Features

### User Features

- Register and log in with JWT-based authentication.
- Browse the equipment catalog.
- Filter and sort equipment by name, category, stock, and utilization.
- Submit standard rental requests.
- Submit academic rental requests as a student, with exam date and justification.
- Track personal requests by status.
- View pending queue positions.
- View active, overdue, returned, and completed rentals.
- View reputation and return assessment history.

### Admin / Operator Features

- View an operational dashboard with KPIs, charts, and recent activity.
- Manage rental requests through the full lifecycle:
  - approve
  - reject
  - mark as rented
  - mark as returned
  - complete return assessment
- View prioritized queues per equipment item.
- Switch queue ordering between weighted scoring and FIFO.
- Manage the equipment catalog.
- Manage users.
- Submit return assessments and apply reputation changes.
- Run a demo simulation that periodically creates random rental requests.

## Technology Stack

### Backend

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.3 |
| Web layer | Spring MVC / REST controllers |
| Persistence | Spring Data JPA, Hibernate |
| Security | Spring Security, JWT |
| Validation | Jakarta Bean Validation |
| Database | PostgreSQL 16 |
| Test database | H2 |
| Testing | JUnit 5, Mockito, Spring Security Test |
| Build | Maven |

### Frontend

| Area | Technology |
| --- | --- |
| UI | Static HTML |
| Styling | Custom CSS |
| Logic | Vanilla JavaScript |
| Charts | Chart.js |
| Auth storage | JWT and current user saved in `localStorage` |

### Runtime

| Component | Description |
| --- | --- |
| `app` | Spring Boot application exposed on port `8080` |
| `db` | PostgreSQL 16 exposed on port `5432` |

## Architecture

LabRent follows a classic layered architecture:

```text
Frontend -> REST Controllers -> Services -> Repositories -> JPA Entities -> Database
```

The important responsibility boundaries are:

- `controller`: HTTP API endpoints, request validation, role restrictions.
- `service`: business rules, workflow orchestration, state changes, prioritization, notifications.
- `repository`: Spring Data JPA access methods.
- `model`: domain entities, enums, inheritance, and state machine classes.
- `dto`: objects exposed through the API and consumed by the frontend.
- `config`: Spring Security, JWT filter, application bootstrap, demo data seeding.
- `exception`: centralized JSON error handling.

The frontend is served from `src/main/resources/static` by the same Spring Boot application.

## Project Structure

```text
.
|-- Dockerfile
|-- docker-compose.yml
|-- pom.xml
|-- README.md
|-- DIAGRAME.txt
|-- barem.txt
|-- mermaidDiagrams/
|   |-- DiagramaComunicareProcesRetur.mmd
|   |-- DiagramaStareEchipament.mmd
|   `-- SecventaCerereInchiriere.mmd
|-- requiredDiagrams/
|   |-- DiagramaActivitatiCerereInchiriere.vsdx
|   |-- DiagramaActivitatiManageriereEchipament.vsdx
|   |-- DiagramaActivitatiProcesdeRetur.vsdx
|   |-- DiagramaCazuriUtilizareLabRent.vsdx
|   `-- DiagramaClaseLabRent.vsdx
`-- src/
    |-- main/
    |   |-- java/ro/atemustard/labrent/
    |   |   |-- config/
    |   |   |-- controller/
    |   |   |-- dto/
    |   |   |-- exception/
    |   |   |-- model/
    |   |   |-- repository/
    |   |   |-- service/
    |   |   `-- util/
    |   `-- resources/
    |       |-- application.properties
    |       |-- application-docker.properties
    |       |-- application-h2.properties
    |       `-- static/
    `-- test/
        |-- java/ro/atemustard/labrent/
        `-- resources/
```

## Domain Model

### User

`User` represents an application account.

Important fields:

- `username`
- `email`
- `password`
- `role`: `USER` or `ADMIN`
- `userType`: `STUDENT` or `NON_STUDENT`
- `reputationScore`: starts at `100.0`, then changes after return assessments

The reputation score is used by the weighted prioritization strategy.

### Equipment

`Equipment` represents an equipment type with pooled stock, not one physical unit.

Example:

```text
Tektronix TBS2000 Oscilloscope
totalQuantity = 2
availableQuantity = 1
```

Important fields:

- `name`
- `description`
- `category`
- `status`
- `totalQuantity`
- `availableQuantity`

Possible statuses:

- `AVAILABLE`
- `RESERVED`
- `RENTED`
- `RETURNED`
- `IN_SERVICE`

### RentalRequest

`RentalRequest` is the abstract base class for rental requests.

Concrete subclasses:

- `StandardRentalRequest`
- `AcademicRentalRequest`

Persistence uses JPA single-table inheritance:

```java
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "request_type")
```

Important fields:

- `user`
- `equipment`
- `startDate`
- `endDate`
- `status`
- `projectDescription`
- `priorityScore`
- `createdAt`
- `returnedAt`
- `returnAssessment`

Possible request statuses:

- `PENDING`
- `APPROVED`
- `REJECTED`
- `RENTED`
- `RETURNED`
- `COMPLETED`

### AcademicRentalRequest

`AcademicRentalRequest` extends `RentalRequest` and adds:

- `examDate`
- `justification`

This subtype is created when a student submits a request marked as exam-related.

### ReturnAssessment

`ReturnAssessment` stores the operator's evaluation after a rental is returned.

Important fields:

- `rentalRequest`
- `operator`
- `conditionRating`
- `notes`
- `reputationImpact`
- `assessedAt`

Condition ratings:

| Rating | Base Reputation Impact |
| --- | ---: |
| `EXCELLENT` | `+5` |
| `GOOD` | `+2` |
| `FAIR` | `0` |
| `POOR` | `-5` |
| `DAMAGED` | `-15` |

Late returns add an extra negative penalty of `-1` point per overdue day, capped at `-10`.

## Design Patterns

### Builder

Used by:

- `RentalRequest`
- `ReturnAssessment`

`RentalRequest.builder()` creates the correct concrete subclass:

- `AcademicRentalRequest` when `isForExam = true`
- `StandardRentalRequest` otherwise

### Factory

Package:

```text
src/main/java/ro/atemustard/labrent/service/factory
```

Factory classes:

- `RentalRequestFactory`
- `StandardRentalRequestFactory`
- `AcademicRentalRequestFactory`

`RentalRequestService` receives the available factories and selects the correct one based on the user type and request data.

### Strategy

Package:

```text
src/main/java/ro/atemustard/labrent/service/prioritization
```

Strategy classes:

- `PrioritizationStrategy`
- `WeightedScoringStrategy`
- `FIFOStrategy`
- `PrioritizationContext`

Admins can switch the active prioritization strategy at runtime.

### State

Package:

```text
src/main/java/ro/atemustard/labrent/model/state
```

State classes:

- `EquipmentState`
- `AvailableState`
- `ReservedState`
- `RentedState`
- `ReturnedState`
- `InServiceState`
- `EquipmentStateFactory`

The state model defines valid equipment status transitions:

```text
AVAILABLE  -> RESERVED
RESERVED   -> RENTED
RESERVED   -> AVAILABLE
RENTED     -> RETURNED
RETURNED   -> AVAILABLE
RETURNED   -> IN_SERVICE
IN_SERVICE -> AVAILABLE
```

Invalid transitions throw `InvalidOperationException`.

### Observer

Package:

```text
src/main/java/ro/atemustard/labrent/service/observer
```

Observer classes:

- `RentalEventListener`
- `EmailNotificationListener`
- `NotificationService`

The notification service emits events when:

- a request is created
- a request is approved
- a request is rejected
- equipment is returned
- an assessment is completed

## Business Workflows

### Submit Rental Request

Endpoint:

```http
POST /api/rental-requests
```

Implementation path:

```text
RentalRequestController.createRequest
-> RentalRequestService.createRequest
-> RentalRequestFactory
-> PrioritizationService
-> NotificationService
```

Flow:

1. The current user is loaded from the JWT principal.
2. The selected equipment is loaded.
3. The service checks that `availableQuantity > 0`.
4. The service validates that `endDate` is after `startDate`.
5. The correct request subtype is created:
   - student plus exam request -> `AcademicRentalRequest`
   - otherwise -> `StandardRentalRequest`
6. The request is saved to obtain `createdAt`.
7. The priority score is calculated.
8. Existing pending requests for the same equipment are recalculated.
9. Notification listeners are called.

### Approve Request

Endpoint:

```http
PUT /api/rental-requests/{id}/approve
```

Rules:

- Request must be `PENDING`.
- One equipment unit is reserved.
- `availableQuantity` decreases.
- Request status becomes `APPROVED`.

### Mark as Rented

Endpoint:

```http
PUT /api/rental-requests/{id}/rent
```

Rules:

- Request must be `APPROVED`.
- Request status becomes `RENTED`.

### Mark as Returned

Endpoint:

```http
PUT /api/rental-requests/{id}/return
```

Rules:

- Request must be `RENTED`.
- Request status becomes `RETURNED`.
- `returnedAt` is set to the current date.
- The equipment unit is not released yet. Release happens after the return assessment.

### Submit Return Assessment

Endpoint:

```http
POST /api/return-assessments
```

Implementation path:

```text
ReturnAssessmentController.submitAssessment
-> ReturnAssessmentService.submitAssessment
-> UserService.updateReputationScore
-> EquipmentService.releaseUnit
-> PrioritizationService.recalculateForUser
-> NotificationService
```

Flow:

1. The rental request is loaded.
2. The request must have status `RETURNED`.
3. Duplicate assessments are rejected.
4. The operator is loaded from the JWT principal.
5. The assessment is created.
6. Reputation impact is calculated from condition rating plus overdue penalty.
7. The request becomes `COMPLETED`.
8. The user's reputation is updated.
9. The user's pending requests are rescored.
10. The equipment unit is released.
11. Notification listeners are called.

## Prioritization Model

The active prioritization strategy is configured with:

```properties
app.prioritization.strategy=weightedScoring
```

Available strategies:

- `weightedScoring`
- `fifo`

### Weighted Scoring

`WeightedScoringStrategy` starts from a stable base score and adjusts it:

| Factor | Rule |
| --- | --- |
| Base | `50` points |
| Reputation | `min(reputationScore / 100 * 20, 40)` |
| Active requests | `-5` points per active request |
| Student bonus | `+5` points |
| Exam urgency | up to `+30` points as exam date approaches |

Queues are ordered by:

```text
priorityScore DESC, createdAt ASC
```

### FIFO

`FIFOStrategy` keeps the same informational priority score but changes the queue ordering to:

```text
createdAt ASC
```

This makes it easy to compare strict chronological ordering with merit-based ordering from the admin UI.

## Security Model

Authentication is stateless and JWT-based.

Rules configured in `SecurityConfig`:

- `POST /api/auth/register` and `POST /api/auth/login` are public.
- `/api/admin/**` requires `ADMIN`.
- Other `/api/**` endpoints require authentication.
- Static frontend assets are public.
- CSRF is disabled because the API uses stateless JWT authentication.
- H2 console is allowed for the H2 profile.

Method-level restrictions are also applied with `@PreAuthorize`, especially for admin-only operations.

## API Reference

All endpoints below are relative to:

```text
http://localhost:8080
```

Authenticated requests must include:

```http
Authorization: Bearer <jwt>
```

### Authentication

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create a user account |
| `POST` | `/api/auth/login` | Public | Authenticate and return JWT |

Register example:

```json
{
  "username": "student.demo",
  "email": "student.demo@example.com",
  "password": "parola123",
  "userType": "STUDENT"
}
```

Login example:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Users

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | Authenticated | Get current user |
| `GET` | `/api/users/{id}` | Admin | Get user by ID |
| `GET` | `/api/users` | Admin | List all users |

### Equipment

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/equipment` | Authenticated | List all equipment |
| `GET` | `/api/equipment/{id}` | Authenticated | Get equipment details |
| `GET` | `/api/equipment/available` | Authenticated | List available equipment |
| `GET` | `/api/equipment/category/{category}` | Authenticated | List equipment by category |
| `POST` | `/api/equipment` | Admin | Create equipment |
| `PUT` | `/api/equipment/{id}` | Admin | Update equipment |
| `DELETE` | `/api/equipment/{id}` | Admin | Delete equipment |

Create or update equipment example:

```json
{
  "name": "Rigol DS1054Z Oscilloscope",
  "description": "Digital oscilloscope, 4 channels, 50MHz",
  "category": "Oscilloscope",
  "totalQuantity": 5
}
```

### Rental Requests

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/rental-requests` | User | Create rental request |
| `GET` | `/api/rental-requests/my` | Authenticated | List current user's requests |
| `GET` | `/api/rental-requests/my-queue-positions` | Authenticated | Get simple pending queue positions |
| `GET` | `/api/rental-requests/my-queue-details` | Authenticated | Get pending queue positions plus queue totals |
| `GET` | `/api/rental-requests/pending` | Admin | List pending requests |
| `GET` | `/api/rental-requests/all` | Admin | List all requests |
| `GET` | `/api/rental-requests/prioritized/{equipmentId}` | Admin | Get prioritized queue for one equipment item |
| `PUT` | `/api/rental-requests/{id}/approve` | Admin | Approve pending request |
| `PUT` | `/api/rental-requests/{id}/reject` | Admin | Reject pending request |
| `PUT` | `/api/rental-requests/{id}/rent` | Admin | Mark approved request as rented |
| `PUT` | `/api/rental-requests/{id}/return` | Admin | Mark rented request as returned |

Create rental request example:

```json
{
  "equipmentId": 1,
  "startDate": "2026-05-15",
  "endDate": "2026-05-22",
  "projectDescription": "Signal processing lab project",
  "isForExam": true,
  "examDate": "2026-05-18",
  "justification": "Required for the final practical exam"
}
```

### Return Assessments

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/return-assessments` | Admin | Submit return assessment |
| `GET` | `/api/return-assessments/request/{requestId}` | Admin | Get assessment by request |
| `GET` | `/api/return-assessments/user/{userId}` | Owner or Admin | Get user's assessment history |

Submit assessment example:

```json
{
  "rentalRequestId": 12,
  "conditionRating": "GOOD",
  "notes": "Returned on time with only minor cosmetic wear."
}
```

### Admin

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/admin/dashboard-stats` | Admin | Dashboard KPIs and chart data |
| `GET` | `/api/admin/activity-feed?limit=20` | Admin | Recent activity feed |
| `GET` | `/api/admin/prioritization-strategy` | Admin | Get active strategy |
| `PUT` | `/api/admin/prioritization-strategy` | Admin | Switch active strategy |
| `POST` | `/api/admin/simulation/random-request` | Admin | Create one random demo request |

Switch strategy example:

```json
{
  "strategy": "fifo"
}
```

or:

```json
{
  "strategy": "weightedScoring"
}
```

### Error Responses

Errors are normalized by `GlobalExceptionHandler` through `ErrorResponseDTO`.

Example validation error:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input data",
  "timestamp": "2026-05-10T12:00:00",
  "validationErrors": {
    "password": "Password must be at least 6 characters"
  }
}
```

## Frontend

The frontend lives in:

```text
src/main/resources/static/index.html
src/main/resources/static/css/style.css
src/main/resources/static/js/app.js
```

It is intentionally framework-free.

Main UI sections:

- Authentication screen
- User dashboard
- Admin dashboard
- Equipment catalog
- My Requests
- New Request
- Manage Requests
- Users
- Assessments

Frontend responsibilities:

- Stores JWT and current user in `localStorage`.
- Adds `Authorization: Bearer <token>` to API calls.
- Renders equipment cards, request cards, tables, modals, badges, and filters.
- Provides advanced filtering for equipment, user requests, admin request management, users, and assessments.
- Displays queue position information.
- Renders Chart.js dashboard visualizations.
- Supports admin simulation controls.

## Demo Data

`DataSeeder` creates demo data on the first application start, but only when the user table is empty.

Seeded data includes:

- 2 admin/operator accounts.
- Multiple student and non-student accounts.
- 10 equipment types.
- Pending queues with competing requests.
- Approved and active rentals.
- Overdue rentals.
- Returned rentals awaiting assessment.
- Completed rentals with assessment history.
- Rejected requests.

Useful accounts:

| Username | Password | Role | Notes |
| --- | --- | --- | --- |
| `admin` | `admin123` | `ADMIN` | Primary operator |
| `operator2` | `admin123` | `ADMIN` | Secondary operator |
| `ion.popescu` | `parola123` | `USER` | Student baseline account |
| `diana.stoica` | `parola123` | `USER` | Student with exam-related priority examples |
| `alex.marinescu` | `parola123` | `USER` | Low reputation, overdue and damaged-history examples |
| `radu.georgescu` | `parola123` | `USER` | High reputation student |
| `andrei.dumitrescu` | `parola123` | `USER` | Non-student external collaborator |

## Running the Project

### Option 1: Docker Compose

Requirements:

- Docker
- Docker Compose

Start the application and database:

```powershell
docker compose up --build
```

Start in the background:

```powershell
docker compose up --build -d
```

Stop containers:

```powershell
docker compose down
```

Reset the PostgreSQL volume and reseed demo data:

```powershell
docker compose down -v
docker compose up --build
```

Application URL:

```text
http://localhost:8080/
```

PostgreSQL connection:

```text
host: localhost
port: 5432
database: labrent
username: postgres
password: postgres
```

### Option 2: Local PostgreSQL

Requirements:

- Java 17
- Maven
- PostgreSQL running locally

Create a database named:

```text
labrent
```

Run:

```powershell
mvn spring-boot:run
```

The default local configuration expects:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/labrent
spring.datasource.username=postgres
spring.datasource.password=postgres
```

You can override credentials with:

```powershell
$env:DB_USERNAME="your_user"
$env:DB_PASSWORD="your_password"
mvn spring-boot:run
```

### Option 3: H2 Profile

Use this when you want to run without PostgreSQL:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 console:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:labrent
```

## Configuration

Main configuration files:

| File | Purpose |
| --- | --- |
| `src/main/resources/application.properties` | Default application settings |
| `src/main/resources/application-docker.properties` | Docker database override |
| `src/main/resources/application-h2.properties` | H2 in-memory profile |

Important properties:

```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update
app.jwt.secret=${JWT_SECRET:labrent-dev-secret-key-change-in-production-min-256-bits-long!!}
app.jwt.expiration-ms=86400000
app.prioritization.strategy=weightedScoring
```

Mail is configured through Spring Mail. If SMTP credentials are not supplied, the notification listener still logs events for development/demo purposes.

Optional environment variables:

| Variable | Description |
| --- | --- |
| `DB_USERNAME` | Local PostgreSQL username |
| `DB_PASSWORD` | Local PostgreSQL password |
| `JWT_SECRET` | JWT signing secret |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |

## Testing

Run all tests:

```powershell
mvn test
```

The test suite covers:

- Application context startup.
- DTO mapping.
- JWT token generation and validation.
- Security authorization rules.
- Repository queries with H2.
- Equipment CRUD and stock transitions.
- Rental request creation and lifecycle transitions.
- Return assessment rules and reputation updates.
- Prioritization strategy behavior.
- Factory selection for standard and academic requests.
- Observer notification dispatch.
- UML-driven individual activity scenarios.

Important test groups:

| Test Area | Representative Files |
| --- | --- |
| Rental request scenario | `SubmitRentalRequestActivityTest`, `RentalRequestServiceTest` |
| Equipment lifecycle | `EquipmentLifecycleActivityTest`, `EquipmentStateTest`, `EquipmentServiceTest` |
| Return assessment | `ReturnAssessmentActivityTest`, `ReturnAssessmentServiceTest` |
| Prioritization | `WeightedScoringStrategyTest`, `FIFOStrategyTest`, `PrioritizationServiceTest` |
| Security | `SecurityAuthorizationTests`, `JwtTokenProviderTest` |
| Persistence | `UserRepositoryTest`, `EquipmentRepositoryTest`, `RentalRequestRepositoryTest` |

If Maven is not installed locally, tests can also be run through a temporary Docker build:

```powershell
@'
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn test -B
'@ | docker build -f - .
```

## Manual Smoke Test

1. Start the app with Docker:

   ```powershell
   docker compose up --build
   ```

2. Open:

   ```text
   http://localhost:8080/
   ```

3. Log in as:

   ```text
   admin / admin123
   ```

4. Check the admin dashboard KPIs and charts.
5. Open Equipment and inspect the priority queue for a scarce item such as `Tektronix TBS2000 Oscilloscope`.
6. Switch prioritization strategy from `weightedScoring` to `fifo`.
7. Reopen the same equipment and verify that queue ordering changes.
8. Open Manage Requests.
9. Approve a pending request.
10. Mark it as rented.
11. Mark it as returned.
12. Open Assessments and submit a return assessment.
13. Verify that the request becomes `COMPLETED`.
14. Verify that the user reputation changes.
15. Log in as a normal user and check My Requests and queue positions.

## UML and Project Documentation

The repository contains UML-related artifacts used for the ISP project.

### Mermaid Diagrams

| File | Description |
| --- | --- |
| `mermaidDiagrams/SecventaCerereInchiriere.mmd` | Submit rental request sequence |
| `mermaidDiagrams/DiagramaStareEchipament.mmd` | Equipment lifecycle state diagram |
| `mermaidDiagrams/DiagramaComunicareProcesRetur.mmd` | Return assessment communication diagram |

### Visual Diagram Files

| File | Description |
| --- | --- |
| `requiredDiagrams/DiagramaClaseLabRent.vsdx` | Class diagram |
| `requiredDiagrams/DiagramaCazuriUtilizareLabRent.vsdx` | Use case diagram |
| `requiredDiagrams/DiagramaActivitatiCerereInchiriere.vsdx` | Rental request activity diagram |
| `requiredDiagrams/DiagramaActivitatiManageriereEchipament.vsdx` | Equipment management activity diagram |
| `requiredDiagrams/DiagramaActivitatiProcesdeRetur.vsdx` | Return process activity diagram |

### Text Specifications

| File | Description |
| --- | --- |
| `DIAGRAME.txt` | Detailed UML diagram specification |
| `barem.txt` | Original grading requirements |

## Troubleshooting

### `request_type` Column Missing

`RentalRequest` uses single-table inheritance with a discriminator column named `request_type`.

If an older PostgreSQL volume was created before this field existed, you may see an error similar to:

```text
column rr1_0.request_type does not exist
```

Reset the database volume:

```powershell
docker compose down -v
docker compose up --build
```

### Demo Data Does Not Reappear

`DataSeeder` runs only when the `users` table is empty.

To force reseeding in Docker:

```powershell
docker compose down -v
docker compose up --build
```

### Port Already in Use

If port `8080` or `5432` is already used, update `docker-compose.yml` port mappings or stop the process using that port.

### JWT Issues

If tokens become invalid after changing `JWT_SECRET`, log out from the frontend or clear browser local storage:

```text
jwt_token
current_user
```

### H2 Console Login

Use:

```text
JDBC URL: jdbc:h2:mem:labrent
User: sa
Password:
```

## Development Notes

- Keep business rules in services, not controllers.
- Use DTOs at the API boundary instead of exposing JPA entities.
- Prefer the state classes for equipment status transitions.
- Recalculate priority when reputation, competition, or the active strategy changes.
- Update tests when changing request lifecycle, reputation rules, or prioritization formulas.
- Be careful with the seeded demo data because it is intentionally designed to exercise dashboards, overdue rentals, queue ordering, and assessment flows.
