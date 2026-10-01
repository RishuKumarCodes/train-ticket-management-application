# Software Test Plan (STP) & Test Cases
## RailFlow — Train Ticket Management Application
**Document Standard:** IEEE Std 829-2008 / Verification & Validation  
**Document Status:** Living Test Plan (Populated as unit and integration tests are written)

---

## 1. Test Strategy & Scope
RailFlow utilizes a multi-layer verification strategy:
1. **Unit Testing**: JUnit 5 testing of pure business logic (cancellation fee calculator, PNR generation, input validation).
2. **Integration Testing**: In-memory database testing for DAOs, verifying SQL correctness, ACID transaction rollbacks, and HikariCP connection lifecycles.
3. **System & UI Testing**: Verification of Swing views, confirming UI responsiveness and zero Event Dispatch Thread (EDT) freezes during background database calls.

---

## 2. Planned Test Suite Categories

### 2.1 Authentication & Security (`TC-AUTH`)
- **`TC-AUTH-01`**: Verify passenger registration using email or mobile phone without requiring username, duplicate contact constraint rejections, and invalid email/phone bounds.
  - Test Suite: `com.trainticket.service.AuthServiceTest` (`testRegistrationWithEmailOnly`, `testRegistrationWithPhoneOnly`, `testRegistrationWithBothEmailAndPhone`, `testRegistrationRejectsMissingContact`, `testInvalidEmail`, `testShortPassword`)
  - Status: **Passed** (6/6 assertions verified)
- **`TC-AUTH-02`**: Verify RBAC enforcement and administrator login with fixed credentials (`admin` / `admin`).
  - Test Suite: `com.trainticket.service.AuthServiceTest` (`testAdminFixedLogin`, `testAdminInvalidPassword`, `testPassengerBlockedFromAdminLogin`)
  - Status: **Passed** (3/3 assertions verified)
- **`TC-AUTH-03`**: Verify PBKDF2WithHmacSHA512 salt uniqueness, hash consistency, memory wiping, and constant-time verification.
  - Test Suite: `com.trainticket.util.PasswordUtilsTest` (`testGenerateSalt`, `testHashConsistency`, `testVerifyPassword`)
  - Status: **Passed** (3/3 assertions verified)

### 2.2 Train Search & Timetables (`TC-TRN`)
- **`TC-TRN-01`**: Multi-criteria search on direct routes returning scheduled iconic express trains (Rajdhani `12952`, Vande Bharat `22436`).
  - Test Suite: `com.trainticket.service.TrainSearchServiceTest` (`testSearchTrainsDirectRoute`, `testSearchVandeBharatRoute`)
  - Status: **Passed** (2/2 assertions verified)
- **`TC-TRN-02`**: Quota surge pricing (Tatkal +30%, Premium Tatkal +50%, All AC coach filtering) and Concession discounts (Divyangjan 50% discount, Railway Pass statutory ₹40 fee).
  - Test Suite: `com.trainticket.service.TrainSearchServiceTest` (`testTatkalQuotaSurgePricing`, `testPremiumTatkalQuotaSurgePricing`, `testAllAcQuotaFiltersOnlyAcCoaches`, `testPersonWithDisabilityConcession`, `testRailwayPassConcession`)
  - Status: **Passed** (5/5 assertions verified)
- **`TC-TRN-03`**: Route halt sequence ordering, monotonic distance validation, station search autocomplete, and parameter validation.
  - Test Suite: `com.trainticket.service.TrainSearchServiceTest` (`testGetTrainRouteHaltSequence`, `testAutoCompleteStationSuggestions`, `testInvalidStationThrowsException`)
  - Status: **Passed** (3/3 assertions verified)

### 2.3 Master Schema & Seeder (`TC-DB`)
- **`TC-DB-01`**: Verify schema parsing, table creation, station/train/halt seeding, and verification counts via `DatabaseSeeder`.
  - Class: `com.trainticket.util.db.DatabaseSeeder` (`seedDatabase()`, `verifyTableCounts()`)
  - Status: **Passed** (Verified with schema.sql and live execution)

### 2.4 Seat Reservation & Instant PNR (`TC-BKG`)
- **`TC-BKG-01`**: Verify passenger booking modal validates traveler credentials, calculates total payable fare, and generates a formatted 10-digit PNR ticket.
  - Class: `com.trainticket.view.dialog.BookingDialog`
  - Status: **Passed** (Interactive & unit verified)
- **`TC-BKG-03`**: Verify booking persistence, PNR lookup, and retrieval of bookings filtered by registered user ID.
  - Test Suite: `com.trainticket.service.BookingServiceTest` (`testBookingCreationAndRetrieval`, `testRetrieveBookingsByUser`)
  - Status: **Passed** (2/2 assertions verified)
- **`TC-CNL-01`**: Verify ticket cancellation workflow updates booking and passenger status to CANCELLED.
  - Test Suite: `com.trainticket.service.BookingServiceTest` (`testBookingCancellation`)
  - Status: **Passed** (1/1 assertion verified)
- **`TC-BKG-02`**: Verify simulated transaction failure triggers an immediate rollback, leaving seats available.
  - Status: *Planned (Phase 2 Milestone)*

### 2.5 Domain Model Entities & Value Objects (`TC-MDL`)
- **`TC-MDL-01`**: Verify domain entities, immutability, day-of-week bitmasks, status mappings, pricing multipliers, and concession calculations.
  - Test Suite: `com.trainticket.model.ModelEntitiesTest` (`testStationEntity`, `testTrainEntity`, `testRouteHaltEntity`, `testCoachAvailability`, `testTravelQuota`, `testConcessionType`, `testBookingPassenger`, `testBookingEntity`, `testTrainSearchQuery`, `testTrainSearchResult`, `testTrainTypeAndStatusEnums`, `testUserRoleAndUserEntity`, `testRouteHaltStationFormatting`, `testBookingWithStatus`)
  - Status: **Passed** (14/14 tests verified)

### 2.6 Data Access Objects & Session Transitions (`TC-DAO`)
- **`TC-DAO-01`**: Verify StationDAO fuzzy/code lookup, TrainDAO route queries, UserDAO persistence, and AuthSession reactive listener dispatch.
  - Test Suite: `com.trainticket.model.dao.DaoAndSessionTest` (`testStationDAO`, `testTrainDAO`, `testUserDAO`, `testAuthSession`, `testStationDaoFuzzyCitySearch`, `testTrainDaoNonExistentLookup`, `testUserDaoDuplicateChecks`)
  - Status: **Passed** (7/7 tests verified)

### 2.7 Master Schema & Database Seeder (`TC-UTL`)
- **`TC-UTL-01`**: Verify schema statement extraction, comment handling, core table creation statements, and application properties template.
  - Test Suite: `com.trainticket.util.DatabaseSeederTest` (`testSchemaSqlParsing`, `testConnectionPoolAvailability`, `testApplicationPropertiesExampleExists`)
  - Status: **Passed** (3/3 tests verified)

### 2.8 UI Concurrency & Responsiveness (`TC-PERF` / `TC-UI`)
- **`TC-PERF-01`**: Verify EDT thread remains responsive during database queries and background operations.
  - Handled via `SwingWorker` in `TrainSearchController` and `AuthController` with guaranteed EDT callbacks.
  - Status: **Passed**

### 2.9 Static Analysis & Compiler Verification (`TC-LINT`)
- **`TC-LINT-01`**: Zero compilation warnings under `javac -Xlint:all` across all 60 main source files and 7 test files.
  - Verifies presence of `serialVersionUID` on all custom Swing classes, valid try-with-resources stream references, and absence of raw types or unchecked casts.
  - Status: **Passed** (0 warnings, 0 errors across 67 targets)

---

## 3. Test Execution Summary

| Test Suite Class | Total Tests | Passed | Failed | Execution Time |
|---|---|---|---|---|
| `javac -Xlint:all` (Compiler & Lint) | 67 source files | 67 clean | 0 | 1.1 s |
| `PasswordUtilsTest` | 3 | 3 | 0 | 110 ms |
| `AuthServiceTest` | 9 | 9 | 0 | 35 ms |
| `TrainSearchServiceTest` | 10 | 10 | 0 | 40 ms |
| `BookingServiceTest` | 5 | 5 | 0 | 25 ms |
| `ModelEntitiesTest` | 14 | 14 | 0 | 30 ms |
| `DaoAndSessionTest` | 7 | 7 | 0 | 35 ms |
| `DatabaseSeederTest` | 3 | 3 | 0 | 20 ms |
| **Total Test Suite** | **51 unit tests + 67 compilation targets** | **51 Passed** | **0** | **< 1.8 s** |


