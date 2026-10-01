# Requirements Traceability Matrix (RTM)
## RailFlow — Train Ticket Management Application
**Document Standard:** NASA SWE-052 / IEEE Bidirectional Traceability  
**Document Status:** Living Traceability Matrix (Updated as modules are implemented)

---

## 1. Overview
The Requirements Traceability Matrix (RTM) establishes bidirectional traceability between user requirements defined in the [SRS Document](../01-feasibility-and-requirements/srs.md), the system implementation packages, and the verification test cases in the [Test Plan](test-plan.md).

As each feature and test is implemented, its verification row will be populated here.

---

## 2. Traceability Matrix

| Requirement ID | Requirement Summary | Implementation Package / Class | Test Case ID | Test Status |
|---|---|---|---|---|
| **REQ-AUTH-01** | User registration & validation | `com.trainticket.model.service.AuthService`<br>`com.trainticket.model.dao.UserDAO`<br>`com.trainticket.view.dialog.AuthDialog` | `TC-AUTH-01` | **Passed** |
| **REQ-AUTH-02** | User authentication & role access | `com.trainticket.model.service.AuthService`<br>`com.trainticket.controller.AuthController`<br>`com.trainticket.view.dialog.AdminLoginDialog`<br>`com.trainticket.view.admin.AdminDashboardFrame` | `TC-AUTH-02` | **Passed** |
| **REQ-TRN-01** | Multi-criteria train search interface | `com.trainticket.model.service.TrainSearchService`<br>`com.trainticket.model.dao.StationDAO`<br>`com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.component.home.SearchCapsulePanel`<br>`com.trainticket.view.component.selector.ModernDatePicker`<br>`com.trainticket.view.component.selector.ModernSmoothDropdown` | `TC-TRN-01` | **Passed** |
| **REQ-TRN-02** | Schedules, stops & coach fare stream | `com.trainticket.view.pages.TrainSearchResultsView`<br>`com.trainticket.controller.TrainSearchController` | `TC-TRN-02` | **Passed** |
| **REQ-TRN-03** | Station-wise timetable & transit modal | `com.trainticket.view.dialog.TrainRouteTimetableDialog` | `TC-TRN-03` | **Passed** |
| **REQ-TRN-04** | Instant booking & PNR ticket issuance | `com.trainticket.view.dialog.BookingDialog` | `TC-BKG-01` | **Passed** |
| **REQ-DB-02** | Master schema & database seeder | `com.trainticket.util.db.DatabaseSeeder`<br>`RailFlow.command`<br>`SeedDatabase.command` | `TC-DB-01` | **Passed** |
| **REQ-NAV-01** | Persistent 3-tab passenger navigation | `com.trainticket.view.MainFrame` | `TC-NAV-01` | **Passed** |
| **REQ-TRN-05** | Dedicated Trains discovery page with pre-filled header | `com.trainticket.view.pages.TrainsPageView`<br>`com.trainticket.view.component.home.SearchCapsulePanel` | `TC-TRN-05` | **Passed** |
| **REQ-BKG-03** | Passenger booking history & PNR status | `com.trainticket.model.service.BookingService`<br>`com.trainticket.model.dao.BookingDAO`<br>`com.trainticket.view.pages.MyBookingsPageView` | `TC-BKG-03` | **Passed** |
| **REQ-ADM-03** | Full-page embedded Station Master Command Center | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.MainFrame` | `TC-ADM-03` | **Passed** |
| **REQ-UI-01** | Animated dropdown selector with 60 FPS spring physics | `com.trainticket.view.component.selector.ModernSmoothDropdown` | `TC-UI-01` | **Passed** |
| **REQ-UI-02** | Minimalist calendar date picker with shortcut presets | `com.trainticket.view.component.selector.ModernDatePicker` | `TC-UI-02` | **Passed** |
| **REQ-UI-03** | Interactive station autocomplete & capsule width stabilization | `com.trainticket.view.component.selector.StationAutocompleteDropdown`<br>`com.trainticket.view.component.home.SearchCapsulePanel` | `TC-UI-03` | **Passed** |
| **REQ-AUD-01** | CoreAudio native device tracking & ambient audio stream | `com.trainticket.util.AudioManager` | `TC-AUD-01` | **Passed** |
| **REQ-ARCH-01** | Modular view decomposition & zero code duplication | `com.trainticket.view.component.train.*`<br>`com.trainticket.view.component.card.*`<br>`com.trainticket.view.component.navigation.*`<br>`com.trainticket.view.admin.*` | `TC-ARCH-01` | **Passed** |
| **REQ-SEAT-01** | Interactive coach seat map | *(Phase 2 Milestone)* | `TC-SEAT-01` | Planned |
| **REQ-BKG-02** | Atomic reservation transaction | *(Phase 2 Milestone)* | `TC-BKG-02` | Planned |
| **REQ-CNL-01** | Ticket cancellation & refund logic | `com.trainticket.model.service.BookingService` | `TC-CNL-01` | **Passed** |
| **REQ-ADM-01** | Admin train & route configuration | *(Phase 2 Milestone)* | `TC-ADM-01` | Planned |
| **NFR-PERF-01** | Low-latency DB queries via HikariCP | `com.trainticket.util.db.DatabaseConnectionPool` | `TC-PERF-01` | **Passed** |
| **NFR-REL-01** | Zero EDT freezing during background I/O | `com.trainticket.controller.TrainSearchController`<br>`com.trainticket.controller.AuthController` | `TC-REL-01` | **Passed** |
| **NFR-CODE-01** | Zero compiler warnings under `javac -Xlint:all` & serialization integrity | All classes in `com.trainticket.*` (67 source targets) | `TC-LINT-01` | **Passed** |
| **REQ-NAV-02** | Adaptive nav capsule: dark glass on Home, frosted white on light-canvas pages | `com.trainticket.view.component.navigation.AppHeaderPanel` | `TC-NAV-02` | **Passed** |
| **REQ-AUD-02** | Music button home-only visibility & auto-stop on navigation away | `com.trainticket.view.component.navigation.AppHeaderPanel`<br>`com.trainticket.view.MainFrame` | `TC-AUD-02` | **Passed** |
| **REQ-UI-04** | Standalone Plan My Trip page with own header and destinations grid | `com.trainticket.view.pages.PlanMyTripView`<br>`com.trainticket.view.MainFrame` | `TC-UI-04` | **Passed** |

