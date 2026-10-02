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
- **`TC-LINT-01`**: Zero compilation warnings under `javac -Xlint:all` across all 74 main source files and 10 test files.
  - Verifies presence of `serialVersionUID` on all custom Swing classes, valid try-with-resources stream references, and absence of raw types or unchecked casts.
  - Status: **Passed** (0 warnings, 0 errors across 84 targets)

### 2.10 Interactive Coach Seat Map & Mock Payment Gateway (`TC-SEAT` / `TC-PAY`)
- **`TC-SEAT-01`**: Verify SeatButton berth abbreviations, state transitions (AVAILABLE, SELECTED, BOOKED), disabled state on BOOKED, and CoachSeatMapPanel capacity constraint enforcement.
  - Test Suite: `com.trainticket.service.SeatAndPaymentTest` (`testSeatButtonCreation`, `testSeatButtonStateTransitions`, `testCoachSeatMapCapacity`, `testSelectedSeatRecord`)
  - Status: **Passed** (4/4 tests verified)
- **`TC-PAY-01`**: Verify PaymentResult record immutability, payment method capture, amount precision, and transaction identifier generation.
  - Test Suite: `com.trainticket.service.SeatAndPaymentTest` (`testPaymentResultRecord`)
  - Status: **Passed** (1/1 test verified)
- **`TC-PNR-01`**: Verify standalone PNR search normalization (hyphens/spaces), retrieval of pre-seeded CNF/RAC/WL bookings, and real-time cancellation.
  - Test Suite: `com.trainticket.service.BookingServiceTest` (`testNormalizedPnrSearch`, `testSamplePnrSeedData`, `testCancelBooking`)
  - Status: **Passed** (3/3 tests verified)

### 2.11 Fleet Operations, Digital Pass, Inventory & CSV Export (`TC-FLEET` / `TC-PASS`)
- **`TC-FLEET-01`**: Verify train commissioning, operational status updates, delay adjustments, and deletion.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testAddTrain`, `testUpdateTrainStatus`, `testDeleteTrain`)
  - Status: **Passed** (3/3 tests verified)
- **`TC-INV-01`**: Verify real-time seat inventory decrement on booking creation and seat replenishment on booking cancellation.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testSeatInventoryDecrementAndReplenish`)
  - Status: **Passed** (1/1 test verified)
- **`TC-PAS-01`**: Verify frequent co-travelers master list retrieval, saving new passenger records, and user isolation.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testPassengerMasterRecordAndDAO`)
  - Status: **Passed** (1/1 test verified)
- **`TC-CSV-01`**: Verify RFC 4180-compliant CSV manifest generation, header formatting, special character escaping, and file output.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testCsvManifestExport`)
  - Status: **Passed** (1/1 test verified)
- **`TC-PASS-01`**: Verify ETicketPassDialog and LiveTrainTrackerDialog headless mode instantiation safety and getter contracts.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testETicketPassDialogHeadlessSafety`)
  - Status: **Passed** (1/1 test verified)
- **`TC-TRN-06`**: Verify search by train number (exact/prefix) and train name substring, plus full journey result creation.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testSearchByTrainNumberOrName`)
  - Status: **Passed** (1/1 test verified)
- **`TC-USR-04`**: Verify SavedPassengersDialog class presence, SearchMode enum contracts, and PassengerMasterDAO add/delete lifecycle.
  - Test Suite: `com.trainticket.service.AdminFleetAndTicketPassTest` (`testSavedPassengersDialogAndSearchMode`)
  - Status: **Passed** (1/1 test verified)

### 2.12 Featured Destinations & Interactive Trip Planning (`TC-DST-01` / `TC-ADM-09`)
- **`TC-DST-01`**: Verify default seed featured destinations in DAO, single-line destination formatting, and TrainDAO `hasRoute` / `hasRouteOnDate` connectivity checking (validating origin-destination pairs like NDLS -> AGC, prohibiting same-station loops, and handling daily trains).
  - Test Suite: `com.trainticket.service.FeaturedDestinationFlowTest` (`testDefaultSeedDestinations`, `testTrainRouteConnectivity`, `testDestinationFormatting`)
  - Status: **Passed** (3/3 tests verified)
- **`TC-ADM-09`**: Verify administrator destination lifecycle (adding new featured destinations, auto-increment IDs, reactive change listener firing, and subsequent removal).
  - Test Suite: `com.trainticket.service.FeaturedDestinationFlowTest` (`testAddAndRemoveDestinationLifecycle`)
  - Status: **Passed** (1/1 test verified)

### 2.13 Admin Analytics Overview & Vector Charts (`TC-ADM-10` to `TC-ADM-15`)
- **`TC-ADM-10` / `TC-ADM-13`**: Verify AdminDashboardView headless instantiation, `CardLayout` section switching, live metrics refresh, and stopLiveClock timer termination.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testAdminDashboardViewLifecycle`)
  - Status: **Passed** (1/1 test verified)
- **`TC-ADM-11`**: Verify AnalyticsLineChart vector rendering, cubic spline curve calculation, gradient fills, and data point updates.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testAnalyticsLineChart`)
  - Status: **Passed** (1/1 test verified)
- **`TC-ADM-12`**: Verify AnalyticsDonutChart multi-segment ring rendering, slice separator gaps, central callout typography, and legend alignment.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testAnalyticsDonutChart`)
  - Status: **Passed** (1/1 test verified)
- **`TC-ADM-13`**: Verify AnalyticsBarChart 24-hour departure histogram, capsule bar rendering, and peak rush hour highlighting.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testAnalyticsBarChart`)
  - Status: **Passed** (1/1 test verified)
- **`TC-ADM-14`**: Verify AdminStatCard 50px corner radius, multi-tier ambient shadow painting, value formatting, and trend subtext badge pills.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testAdminStatCard`)
  - Status: **Passed** (1/1 test verified)
- **`TC-ADM-15`**: Verify 100% real data mathematical integrity, dynamic DayOfWeek revenue and passenger trajectory plotting, and raw seat capacity to percentage legend calculations.
  - Test Suite: `com.trainticket.view.AdminAnalyticsOverviewTest` (`testRealDataIntegrity`)
  - Status: **Passed** (1/1 test verified)

---

## 3. Test Execution Summary

| Test Suite Class | Total Tests | Passed | Failed | Execution Time |
|---|---|---|---|---|
| `javac -Xlint:all` (Compiler & Lint) | 87 source files | 87 clean | 0 | 2.2 s |
| `PasswordUtilsTest` | 3 | 3 | 0 | 110 ms |
| `AuthServiceTest` | 9 | 9 | 0 | 35 ms |
| `TrainSearchServiceTest` | 10 | 10 | 0 | 45 ms |
| `BookingServiceTest` | 7 | 7 | 0 | 25 ms |
| `SeatAndPaymentTest` | 5 | 5 | 0 | 30 ms |
| `ModelEntitiesTest` | 14 | 14 | 0 | 30 ms |
| `DaoAndSessionTest` | 7 | 7 | 0 | 35 ms |
| `DatabaseSeederTest` | 3 | 3 | 0 | 20 ms |
| `AdminFleetAndTicketPassTest` | 9 | 9 | 0 | 35 ms |
| `ResponsiveCardGridLayoutTest` | 4 | 4 | 0 | 45 ms |
| `FeaturedDestinationFlowTest` | 4 | 4 | 0 | 20 ms |
| `CancellationRefundTest` | 4 | 4 | 0 | 25 ms |
| `AdminAnalyticsOverviewTest` | 6 | 6 | 0 | 45 ms |
| **Total Test Suite** | **86 unit tests + 87 compilation targets** | **86 Passed** | **0** | **< 2.8 s** |




