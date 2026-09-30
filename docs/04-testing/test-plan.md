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
- **`TC-TRN-01`**: Verify search between valid stations returns scheduled services and fares.
- **`TC-TRN-02`**: Verify search on routes with zero scheduled runs returns a graceful empty state.

### 2.3 Seat Reservation & ACID Integrity (`TC-BKG`)
- **`TC-BKG-01`**: Verify successful booking marks seat as reserved and issues a unique 10-digit PNR.
- **`TC-BKG-02`**: Verify simulated transaction failure triggers an immediate rollback, leaving seats available.

### 2.4 UI Concurrency & Responsiveness (`TC-PERF` / `TC-UI`)
- **`TC-PERF-01`**: Verify EDT thread remains responsive during database queries and background operations.

*(Individual test methods and execution results will be recorded here alongside their code implementations.)*
