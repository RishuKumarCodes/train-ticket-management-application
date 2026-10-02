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
| **REQ-TRN-03** | Station-wise timetable & transit modal | `com.trainticket.view.dialog.LiveTrainTrackerDialog` | `TC-TRN-03` | **Passed** |
| **REQ-TRN-04** | Instant booking & PNR ticket issuance | `com.trainticket.view.dialog.BookingDialog` | `TC-BKG-01` | **Passed** |
| **REQ-DB-02** | Master schema & database seeder | `com.trainticket.util.db.DatabaseSeeder`<br>`RailFlow.command`<br>`SeedDatabase.command` | `TC-DB-01` | **Passed** |
| **REQ-NAV-01** | Persistent 3-tab passenger navigation | `com.trainticket.view.MainFrame` | `TC-NAV-01` | **Passed** |
| **REQ-TRN-05** | Dedicated Route Search Results page with pre-filled header | `com.trainticket.view.pages.TrainSearchResultsView`<br>`com.trainticket.view.component.home.SearchCapsulePanel` | `TC-TRN-05` | **Passed** |
| **REQ-BKG-03** | Passenger booking history & PNR status | `com.trainticket.model.service.BookingService`<br>`com.trainticket.model.dao.BookingDAO`<br>`com.trainticket.view.pages.MyBookingsPageView` | `TC-BKG-03` | **Passed** |
| **REQ-ADM-03** | Full-page embedded Station Master Command Center | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.MainFrame` | `TC-ADM-03` | **Passed** |
| **REQ-UI-01** | Animated dropdown selector with 60 FPS spring physics | `com.trainticket.view.component.selector.ModernSmoothDropdown` | `TC-UI-01` | **Passed** |
| **REQ-UI-02** | Minimalist calendar date picker with shortcut presets | `com.trainticket.view.component.selector.ModernDatePicker` | `TC-UI-02` | **Passed** |
| **REQ-UI-03** | Interactive station autocomplete & capsule width stabilization | `com.trainticket.view.component.selector.StationAutocompleteDropdown`<br>`com.trainticket.view.component.home.SearchCapsulePanel` | `TC-UI-03` | **Passed** |
| **REQ-AUD-01** | CoreAudio native device tracking & ambient audio stream | `com.trainticket.util.AudioManager` | `TC-AUD-01` | **Passed** |
| **REQ-ARCH-01** | Modular view decomposition & zero code duplication | `com.trainticket.view.component.train.*`<br>`com.trainticket.view.component.card.*`<br>`com.trainticket.view.component.navigation.*`<br>`com.trainticket.view.admin.*` | `TC-ARCH-01` | **Passed** |
| **REQ-SRCH-04** | Swap Route Button (⇄) with animated rotation & auto-search | `com.trainticket.view.component.home.SearchCapsulePanel`<br>`com.trainticket.view.pages.TrainSearchResultsView` | `TC-SRCH-04` | **Passed** |
| **REQ-PNR-01** | Standalone PNR Status Enquiry dialog & passenger cancellation | `com.trainticket.view.dialog.PnrStatusDialog`<br>`com.trainticket.model.service.BookingService`<br>`com.trainticket.view.pages.MyBookingsPageView`<br>`com.trainticket.view.MainFrame` | `TC-PNR-01` | **Passed** |
| **REQ-SEAT-01** | Interactive coach seat map & berth allocation | `com.trainticket.view.component.seat.CoachSeatMapPanel`<br>`com.trainticket.view.component.seat.SeatButton`<br>`com.trainticket.view.dialog.CoachSeatSelectionDialog`<br>`com.trainticket.view.dialog.BookingDialog` | `TC-SEAT-01` | **Passed** |
| **REQ-PAY-01** | Mock Payment Gateway modal (UPI QR, Card, NetBanking, Wallet) | `com.trainticket.view.dialog.PaymentGatewayDialog`<br>`com.trainticket.view.dialog.BookingDialog` | `TC-PAY-01` | **Passed** |
| **REQ-BKG-02** | Atomic reservation transaction & inventory locking | `com.trainticket.model.service.BookingService`<br>`com.trainticket.model.dao.TrainDAO` | `TC-INV-01` | **Passed** |
| **REQ-CNL-01** | Tiered IRCTC Cancellation & Refund Engine | `com.trainticket.model.service.CancellationRefundEngine`<br>`com.trainticket.model.service.BookingService` | `TC-CNL-01` | **Passed** |
| **REQ-CNL-02** | Liquid Cancellation Modal Dialog (`CancelTicketModalDialog`) | `com.trainticket.view.dialog.CancelTicketModalDialog`<br>`com.trainticket.view.dialog.PnrStatusDialog`<br>`com.trainticket.view.component.card.BookingTicketCard` | `TC-CNL-02` | **Passed** |
| **REQ-CNL-03** | Database Persistence & E-Ticket Cancellation State | `com.trainticket.model.dao.BookingDAO`<br>`com.trainticket.view.dialog.ETicketPassDialog` | `TC-CNL-03` | **Passed** |
| **REQ-ADM-01** | Admin train & route configuration | `com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.dialog.AddTrainDialog`<br>`com.trainticket.view.dialog.EditTrainStatusDialog` | `TC-FLEET-01` | **Passed** |
| **REQ-ADM-04** | Station Master Fleet Addition (Commission New Train) | `com.trainticket.view.dialog.AddTrainDialog`<br>`com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-FLEET-01` | **Passed** |
| **REQ-ADM-05** | Live Train Operational Status & Delay Broadcaster | `com.trainticket.view.dialog.EditTrainStatusDialog`<br>`com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.component.train.TrainResultCard` | `TC-FLEET-01` | **Passed** |
| **REQ-ADM-06** | Train Fleet Decommissioning / Deletion | `com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-FLEET-01` | **Passed** |
| **REQ-ADM-07** | Real-Time Dynamic Operational Metrics & Corridor Bar Graphs | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.admin.AdminStatCard` | `TC-FLEET-01` | **Passed** |
| **REQ-ADM-08** | Passenger Manifest & Fleet CSV Export | `com.trainticket.model.service.CsvExportService`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-CSV-01` | **Passed** |
| **REQ-BKG-04** | Real-Time Dynamic Seat Inventory Decrement | `com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.model.service.BookingService` | `TC-INV-01` | **Passed** |
| **REQ-BKG-05** | Real-Time Seat Inventory Replenishment on Cancellation | `com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.model.service.BookingService` | `TC-INV-01` | **Passed** |
| **REQ-PAS-01** | Electronic Reservation Slip (ERS) Digital Ticket Pass | `com.trainticket.view.dialog.ETicketPassDialog`<br>`com.trainticket.view.component.card.BookingTicketCard`<br>`com.trainticket.view.dialog.BookingDialog` | `TC-PASS-01` | **Passed** |
| **REQ-PAS-02** | High-Resolution Pass PNG Export & Native Printing | `com.trainticket.view.dialog.ETicketPassDialog` | `TC-PASS-01` | **Passed** |
| **REQ-USR-03** | Frequent Co-Travelers Master List (1-Click Auto-Fill) | `com.trainticket.model.PassengerMasterRecord`<br>`com.trainticket.model.dao.PassengerMasterDAO`<br>`com.trainticket.view.dialog.BookingDialog` | `TC-PAS-01` | **Passed** |
| **REQ-TRK-01** | Unified Live Status & Route Timetable Tracker | `com.trainticket.view.dialog.LiveTrainTrackerDialog`<br>`com.trainticket.view.component.train.TrainResultCard`<br>`com.trainticket.view.MainFrame` | `TC-PASS-01` | **Passed** |
| **REQ-TRN-06** | Fleet Discovery & Number/Name Search in Trains Tab | `com.trainticket.view.pages.TrainsPageView`<br>`com.trainticket.model.dao.TrainDAO` | `TC-TRN-06` | **Passed** |
| **REQ-USR-04** | Passenger Master List Management Dialog & Booking Auto-Sync | `com.trainticket.view.dialog.SavedPassengersDialog`<br>`com.trainticket.view.pages.MyBookingsPageView`<br>`com.trainticket.view.dialog.BookingDialog` | `TC-USR-04` | **Passed** |
| **NFR-PERF-01** | Low-latency DB queries via HikariCP | `com.trainticket.util.db.DatabaseConnectionPool` | `TC-PERF-01` | **Passed** |
| **NFR-REL-01** | Zero EDT freezing during background I/O | `com.trainticket.controller.TrainSearchController`<br>`com.trainticket.controller.AuthController` | `TC-REL-01` | **Passed** |
| **NFR-CODE-01** | Zero compiler warnings under `javac -Xlint:all` & serialization integrity | All classes in `com.trainticket.*` (74 source targets) | `TC-LINT-01` | **Passed** |
| **REQ-NAV-02** | Adaptive nav capsule: dark glass on Home, frosted white on light-canvas pages | `com.trainticket.view.component.navigation.AppHeaderPanel` | `TC-NAV-02` | **Passed** |
| **REQ-AUD-02** | Music button home-only visibility & auto-stop on navigation away | `com.trainticket.view.component.navigation.AppHeaderPanel`<br>`com.trainticket.view.MainFrame` | `TC-AUD-02` | **Passed** |
| **REQ-UI-04** | Standalone Plan My Trip page with fixed floating back button, single scrolling header, and responsive fluid card grid (2-5 columns) | `com.trainticket.view.pages.PlanMyTripView`<br>`com.trainticket.view.component.home.ResponsiveCardGridLayout`<br>`com.trainticket.view.MainFrame` | `TC-UI-04` | **Passed** |
| **REQ-DST-01** | Interactive Destination Trip Planning Dialog with real-time route validation | `com.trainticket.view.dialog.PlanDestinationTripDialog`<br>`com.trainticket.model.dao.TrainDAO`<br>`com.trainticket.view.component.home.DestinationImageCard` | `TC-DST-01` | **Passed** |
| **REQ-ADM-09** | Administrator Featured Destinations Management (Responsive cards showcase, edit/delete actions, PC image upload) | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.admin.AdminDestinationCard`<br>`com.trainticket.view.dialog.AddDestinationDialog`<br>`com.trainticket.model.dao.FeaturedDestinationDAO` | `TC-ADM-09` | **Passed** |
| **REQ-ADM-10** | Scrollable Analytics Overview Dashboard Workspace (zero rigid headers, fluid JScrollPane) | `com.trainticket.view.admin.AdminDashboardView` | `TC-ADM-10` | **Passed** |
| **REQ-ADM-11** | Vector Curved Area & Line Spline Chart (7-day revenue velocity & traffic trends) | `com.trainticket.view.admin.chart.AnalyticsLineChart`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-ADM-11` | **Passed** |
| **REQ-ADM-12** | Seat Class Demand & Capacity Share Donut Chart | `com.trainticket.view.admin.chart.AnalyticsDonutChart`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-ADM-12` | **Passed** |
| **REQ-ADM-13** | Hourly Network Dispatch Distribution Histogram | `com.trainticket.view.admin.chart.AnalyticsBarChart`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-ADM-13` | **Passed** |
| **REQ-ADM-14** | Elevated KPI Metric Cards with Trend Badges & Ambient Elevation | `com.trainticket.view.admin.AdminStatCard`<br>`com.trainticket.view.admin.AdminDashboardView` | `TC-ADM-14` | **Passed** |
| **REQ-ADM-15** | 100% Real Database Analytics & Dynamic Mathematical Grounding | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.admin.chart.AnalyticsLineChart`<br>`com.trainticket.view.admin.chart.AnalyticsDonutChart`<br>`com.trainticket.view.admin.chart.AnalyticsBarChart` | `TC-ADM-15` | **Passed** |
| **REQ-ADM-16** | Passenger Booking Manifest Inspection & E-Ticket Details View | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.view.dialog.ETicketPassDialog`<br>`com.trainticket.model.service.BookingService` | `TC-PASS-01` | **Passed** |
| **REQ-ADM-17** | System Health & Diagnostics Telemetry Dashboard (Subsystem Matrix, Heap Memory Bar, Controls, Audit Log) | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.util.db.DatabaseConnectionPool` | `TC-ADM-15` | **Passed** |
| **REQ-ADM-18** | User Directory & Account Management (searchable table, ACTIVE/BLOCKED toggle, User Detail drill-down modal with booking history) | `com.trainticket.view.admin.AdminDashboardView`<br>`com.trainticket.model.dao.UserDAO`<br>`com.trainticket.model.dao.BookingDAO`<br>`com.trainticket.view.admin.AdminSidebar` | `TC-ADM-16` | **Implemented** |


