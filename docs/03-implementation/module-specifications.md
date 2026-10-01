# Module Specifications & Architecture Breakdown
## RailFlow — Train Ticket Management Application
**Document Status:** Living Technical Specification (Updated as classes are implemented)

---

## 1. High-Level Package Architecture

The project follows a standard 3-tier MVC directory structure:

```
com.trainticket/
├── config/                     # Configuration loaders (DB connection settings, app constants)
├── model/                      # Business domain, entities, DTOs, and JDBC DAOs
├── view/                       # Passive Swing views, panels, dialogs, and themes
├── controller/                 # Action listeners and EDT asynchronous dispatchers
└── util/                       # Shared utilities (DB connection pool, animation helpers, asset loader)
```

---

## 2. Layer Contracts & Responsibilities

1. **Model Layer (`com.trainticket.model`)**:
   - Entities represent database rows.
   - DAOs own all SQL queries and `PreparedStatement` executions.
   - Services coordinate multi-DAO atomic transactions.
   - **Rule**: Never import Swing or AWT classes.

2. **View Layer (`com.trainticket.view`)**:
   - Renders state and registers listener callbacks.
   - **Rule**: Never execute SQL queries or long-running computations.

3. **Controller Layer (`com.trainticket.controller`)**:
   - Handles user interactions from Views.
   - Delegates business tasks to Model/Services using asynchronous workers (`SwingWorker`).
   - Updates View state exclusively on the Swing Event Dispatch Thread (EDT).

---

## 3. Implemented Modules Log

| Class / Component | Package | Role & Responsibility |
|---|---|---|
| [`Main.java`](../../src/main/java/com/trainticket/Main.java) | `com.trainticket` | Application bootstrap, FlatLaf Universal Light theme initialization, macOS properties, and EDT dispatch. |
| [`MainFrame.java`](../../src/main/java/com/trainticket/view/MainFrame.java) | `com.trainticket.view` | Primary `JFrame` window with `JLayeredPane` root, OS Taskbar / macOS Dock icon binding (`icon.png` squircle), `icon-wide.png` header branding, and background video lifecycle integration. |
| [`VideoBackgroundPanel.java`](../../src/main/java/com/trainticket/view/component/VideoBackgroundPanel.java) | `com.trainticket.view.component` | High-performance JavaFX `MediaPlayer` embedded inside Swing (`JFXPanel`); renders looping `hero.mp4` with dynamic cover-scaling, dark tint overlay, and window lifecycle pause/resume. |
| [`HomeView.java`](../../src/main/java/com/trainticket/view/pages/HomeView.java) | `com.trainticket.view.pages` | Streamlined page orchestrator hosting `HeroSection` and `FeaturedDestinationsSection` within a transparent `JScrollPane` with smart occlusion culling and smooth programmatic scrolling. |
| [`HeroSection.java`](../../src/main/java/com/trainticket/view/component/home/HeroSection.java) | `com.trainticket.view.component.home` | Hero section coordinating colossal kinetic animated typography (`HeroCenterTitleComponent`) and horizontal glass search capsule (`SearchCapsulePanel`). |
| [`HeroCenterTitleComponent.java`](../../src/main/java/com/trainticket/view/component/home/HeroCenterTitleComponent.java) | `com.trainticket.view.component.home` | Dynamic animated headline rendering "DISCOVER YOUR NEXT" eyebrow highlight and staggered sextic ease character handoff for "ADVENTURE", "EXPERIENCE", "JOURNEY", "MEMORY". |
| [`SearchCapsulePanel.java`](../../src/main/java/com/trainticket/view/component/home/SearchCapsulePanel.java) | `com.trainticket.view.component.home` | Horizontal floating white pill capsule holding From, To, Date, Class dropdown, pastel circular icon avatars, and brand-orange Search CTA button. |
| [`FeaturedDestinationsSection.java`](../../src/main/java/com/trainticket/view/component/home/FeaturedDestinationsSection.java) | `com.trainticket.view.component.home` | Crisp white sheet section with 70px corner radius, ambient drop shadow, watermarked header, and 3-column responsive grid of destination cards. |
| [`FeaturedDestinationsHeader.java`](../../src/main/java/com/trainticket/view/component/home/FeaturedDestinationsHeader.java) | `com.trainticket.view.component.home` | Visual section header rendering subtle Bebas Neue "DESTINATION" watermark behind bold slate "Featured Destinations" foreground title. |
| [`DestinationImageCard.java`](../../src/main/java/com/trainticket/view/component/home/DestinationImageCard.java) | `com.trainticket.view.component.home` | Interactive scenic photo card with 50px rounded corners, per-character staggered title reveal behind image, frosted location row, and rotating arrow action button. |
| [`AssetManager.java`](../../src/main/java/com/trainticket/util/AssetManager.java) | `com.trainticket.util` | Classpath resource media loader with in-memory caching for images, TrueType fonts (Roboto & Bebas Neue), and vector icons. |
| [`AudioManager.java`](../../src/main/java/com/trainticket/util/AudioManager.java) | `com.trainticket.util` | High-fidelity ambient audio engine using Java Sound (`javax.sound.sampled`) and native macOS CoreAudio device tracking; routes sound to the exact active OS output source, auto-migrates live streams when OS devices change, and supports hardware output switching. |
| [`DatabaseConnectionPool.java`](../../src/main/java/com/trainticket/util/db/DatabaseConnectionPool.java) | `com.trainticket.util.db` | High-performance HikariCP connection pool; loads credentials and handles SQL connection lifecycles. |
| [`PasswordUtils.java`](../../src/main/java/com/trainticket/util/PasswordUtils.java) | `com.trainticket.util` | Cryptographic utility providing PBKDF2WithHmacSHA512 password salting, constant-time verification, and memory wiping. |
| [`UserRole.java`](../../src/main/java/com/trainticket/model/UserRole.java) | `com.trainticket.model` | Role-Based Access Control (RBAC) enum: `GUEST`, `PASSENGER`, `ADMIN`. |
| [`User.java`](../../src/main/java/com/trainticket/model/User.java) | `com.trainticket.model` | Immutable domain user entity holding account profile, contact details (email and/or phone), and role (zero UI dependencies). |
| [`UserRecord.java`](../../src/main/java/com/trainticket/model/dao/UserRecord.java) | `com.trainticket.model.dao` | Data transfer record encapsulating domain User alongside cryptographic salt and hash. |
| [`UserDAO.java`](../../src/main/java/com/trainticket/model/dao/UserDAO.java) | `com.trainticket.model.dao` | Pure JDBC persistence using `PreparedStatement` with HikariCP pooling, email/phone lookup, and thread-safe in-memory dev fallback. |
| [`AuthStateListener.java`](../../src/main/java/com/trainticket/model/AuthStateListener.java) | `com.trainticket.model` | Functional callback interface for decoupled reactive authentication state notifications. |
| [`AuthSession.java`](../../src/main/java/com/trainticket/model/AuthSession.java) | `com.trainticket.model` | Thread-safe session singleton managing active authenticated identity (`GUEST`, `PASSENGER`, `ADMIN`). |
| [`AuthService.java`](../../src/main/java/com/trainticket/model/service/AuthService.java) | `com.trainticket.model.service` | Authentication and registration business rules engine with email or mobile phone primary authentication, internal username derivation, password strength bounds, and fixed admin credential verification. |
| [`AuthController.java`](../../src/main/java/com/trainticket/controller/AuthController.java) | `com.trainticket.controller` | Asynchronous worker coordinator decoupling PBKDF2 hashing from Swing EDT with guaranteed EDT callback dispatch. |
| [`ModernModalDialog.java`](../../src/main/java/com/trainticket/view/dialog/ModernModalDialog.java) | `com.trainticket.view.dialog` | Reusable modal dialog base window providing native separate window presence, application modality, macOS traffic lights (close, minimize, status), drag-to-move header, 60 FPS enter/exit spring animations, 50px corner radius, borderless card surface, multi-tier soft shadows, and pill control factories. |
| [`AuthDialog.java`](../../src/main/java/com/trainticket/view/dialog/AuthDialog.java) | `com.trainticket.view.dialog` | Frosted glassmorphic passenger auth modal extending `ModernModalDialog` with segmented pill tabs (Sign In / Create Account via email or phone), full pill inputs/buttons, and "Continue as Guest". |
| [`AdminLoginDialog.java`](../../src/main/java/com/trainticket/view/dialog/AdminLoginDialog.java) | `com.trainticket.view.dialog` | High-security Station Master portal dialog extending `ModernModalDialog` with fixed-credentials check, full pill inputs, and zero sign-up option. |
| [`AdminDashboardFrame.java`](../../src/main/java/com/trainticket/view/admin/AdminDashboardFrame.java) | `com.trainticket.view.admin` | Dedicated station master desktop window styled in Universal Light Theme with operational sidebar, live clock, fleet roster table, and system health status. |
| [`Station.java`](../../src/main/java/com/trainticket/model/Station.java) | `com.trainticket.model` | Immutable domain entity for railway stations (code, name, city, state) with zero UI dependencies. |
| [`Train.java`](../../src/main/java/com/trainticket/model/Train.java) | `com.trainticket.model` | Domain entity for passenger trains holding number, name, type, origin/destination, running days bitmask, halts, and coaches. |
| [`TrainType.java`](../../src/main/java/com/trainticket/model/TrainType.java) | `com.trainticket.model` | Classification enum: `VANDE_BHARAT`, `RAJDHANI`, `SHATABDI`, `SUPERFAST`, `EXPRESS`. |
| [`TrainStatus.java`](../../src/main/java/com/trainticket/model/TrainStatus.java) | `com.trainticket.model` | Operational running status enum: `ON_TIME`, `DEPARTED`, `DELAYED`, `CANCELLED`. |
| [`TravelQuota.java`](../../src/main/java/com/trainticket/model/TravelQuota.java) | `com.trainticket.model` | Travel quota enum with dynamic fare multipliers: `GENERAL` (1.0x), `TATKAL` (1.3x), `PREMIUM_TATKAL` (1.5x), `ALL_AC`. |
| [`ConcessionType.java`](../../src/main/java/com/trainticket/model/ConcessionType.java) | `com.trainticket.model` | Concession discount policy enum: `NONE`, `PERSON_WITH_DISABILITY` (50% discount), `RAILWAY_PASS` (statutory fee ₹40). |
| [`RouteHalt.java`](../../src/main/java/com/trainticket/model/RouteHalt.java) | `com.trainticket.model` | Immutable entity representing scheduled station halts, stop sequence, arrival/departure, halt minutes, km, and day count. |
| [`CoachAvailability.java`](../../src/main/java/com/trainticket/model/CoachAvailability.java) | `com.trainticket.model` | Seating inventory and dynamic fare value object with status helpers (`AVAILABLE`, `RAC`, `WAITLIST`). |
| [`TrainSearchQuery.java`](../../src/main/java/com/trainticket/model/TrainSearchQuery.java) | `com.trainticket.model` | Multi-criteria search query DTO capturing origin, destination, date, quota, concession, and preferred class. |
| [`TrainSearchResult.java`](../../src/main/java/com/trainticket/model/TrainSearchResult.java) | `com.trainticket.model` | Evaluated matching train result DTO with boarding/alighting halts, duration calculations, and coach inventory. |
| [`StationDAO.java`](../../src/main/java/com/trainticket/model/dao/StationDAO.java) | `com.trainticket.model.dao` | Station data access object with HikariCP PreparedStatement queries and 18-station thread-safe in-memory fallback. |
| [`TrainDAO.java`](../../src/main/java/com/trainticket/model/dao/TrainDAO.java) | `com.trainticket.model.dao` | Train route matching DAO with stop-sequence order validation, quota surge & concession pricing, and 7-train repository. |
| [`TrainSearchService.java`](../../src/main/java/com/trainticket/model/service/TrainSearchService.java) | `com.trainticket.model.service` | Domain search service enforcing validation rules, station code verification, route search, and station autocomplete. |
| [`TrainSearchController.java`](../../src/main/java/com/trainticket/controller/TrainSearchController.java) | `com.trainticket.controller` | Background worker coordinator executing train queries via `SwingWorker` with guaranteed EDT callback dispatch. |
| [`TrainSearchResultsView.java`](../../src/main/java/com/trainticket/view/pages/TrainSearchResultsView.java) | `com.trainticket.view.pages` | Universal Light Theme search stream page with Bebas Neue headline, sorting pills, and 50px borderless `TrainResultCard`s. |
| [`TrainRouteTimetableDialog.java`](../../src/main/java/com/trainticket/view/dialog/TrainRouteTimetableDialog.java) | `com.trainticket.view.dialog` | Station-wise timetable modal extending `ModernModalDialog` with vertical journey stepper, transit tracking, and halt table. |
| [`BookingDialog.java`](../../src/main/java/com/trainticket/view/dialog/BookingDialog.java) | `com.trainticket.view.dialog` | Instant passenger booking modal extending `ModernModalDialog` with traveler inputs, fare summary, and PNR ticket generation. |
| [`DatabaseSeeder.java`](../../src/main/java/com/trainticket/util/db/DatabaseSeeder.java) | `com.trainticket.util.db` | Command-line utility and runtime service for executing `schema.sql`, verifying table counts, and seeding master records. |
| [`RailFlow.command`](../../RailFlow.command) | Workspace Root | Interactive macOS double-click launcher & CLI script supporting both app launching and automated `./RailFlow.command seed`. |
| [`ModernSmoothDropdown.java`](../../src/main/java/com/trainticket/view/component/selector/ModernSmoothDropdown.java) | `com.trainticket.view.component.selector` | Generic smooth animated dropdown component featuring transparent `JWindow` floating card, 60 FPS cubic spring drop-down motion, rotating chevron indicator, subtitle badges, and click-outside dismissal. |
| [`ModernDatePicker.java`](../../src/main/java/com/trainticket/view/component/selector/ModernDatePicker.java) | `com.trainticket.view.component.selector` | Minimalist interactive calendar date picker with transparent floating card, Bebas Neue month headers, quick shortcut presets (`Today`, `Tomorrow`, `+7 Days`), day grid with past-date disabling, and circular selection highlights. |
| [`SeedDatabase.command`](../../SeedDatabase.command) | Workspace Root | Dedicated macOS double-click Finder launcher script for one-click train database population. |
| [`BookingPassenger.java`](../../src/main/java/com/trainticket/model/BookingPassenger.java) | `com.trainticket.model` | Value object encapsulating individual traveler names, ages, genders, assigned seat labels (e.g. `B4-23`), berth types, and status. |
| [`Booking.java`](../../src/main/java/com/trainticket/model/Booking.java) | `com.trainticket.model` | Domain entity representing a confirmed train ticket reservation with unique PNR, travel date, quota, fare, timestamps, and passenger list. |
| [`BookingDAO.java`](../../src/main/java/com/trainticket/model/dao/BookingDAO.java) | `com.trainticket.model.dao` | Data access object executing JDBC `PreparedStatement` queries against `bookings` and `booking_passengers` with thread-safe session cache. |
| [`BookingService.java`](../../src/main/java/com/trainticket/model/service/BookingService.java) | `com.trainticket.model.service` | Thread-safe singleton service managing ticket creation, user history retrieval, PNR lookup, and reservation cancellation. |
| [`AdminDashboardView.java`](../../src/main/java/com/trainticket/view/admin/AdminDashboardView.java) | `com.trainticket.view.admin` | Full-page Station Master Command Center embedded inside `MainFrame`, eliminating popup windows, featuring top digital telemetry clock, fleet tables, station halts, manifests, and sign-out action. |
| [`TrainsPageView.java`](../../src/main/java/com/trainticket/view/pages/TrainsPageView.java) | `com.trainticket.view.pages` | Dedicated full-page train discovery view with interactive pre-filled search capsule header, route summary chips, sorting filters, and live train result cards. |
| [`MyBookingsPageView.java`](../../src/main/java/com/trainticket/view/pages/MyBookingsPageView.java) | `com.trainticket.view.pages` | Dedicated passenger booking history page with guest sign-in card prompts, confirmed ticket cards, PNR statuses, passenger manifests, and cancellation workflows. |
| [`BookingServiceTest.java`](../../src/test/java/com/trainticket/service/BookingServiceTest.java) | `com.trainticket.service` | JUnit 5 test suite verifying ticket creation, user booking retrieval, PNR lookup, and cancellation state transitions. |
| [`AppHeaderPanel.java`](../../src/main/java/com/trainticket/view/component/navigation/AppHeaderPanel.java) | `com.trainticket.view.component.navigation` | Decoupled top application navigation header with official wide brand logo image (`icon-wide-white.png` / `icon-wide.png`) featuring contrast adaptation, ambient music toggle with CoreAudio output menu, 3-tab navigation capsule, and reactive authentication profile pill button. |
| [`TrainResultCard.java`](../../src/main/java/com/trainticket/view/component/train/TrainResultCard.java) | `com.trainticket.view.component.train` | Unified 50px borderless train schedule card with Bebas Neue typography, journey duration stepper, coach selection row, route timetable modal trigger, and instant booking action. |
| [`CoachClassPillButton.java`](../../src/main/java/com/trainticket/view/component/train/CoachClassPillButton.java) | `com.trainticket.view.component.train` | Interactive coach class selection pill button rendering seat inventory status, dynamic fare calculation, and animated selection states. |
| [`BookingTicketCard.java`](../../src/main/java/com/trainticket/view/component/card/BookingTicketCard.java) | `com.trainticket.view.component.card` | Dedicated 50px borderless booking card displaying PNR badges, travel stations, scheduled timings, passenger berth list, digital pass view, and cancellation workflow. |
| [`AdminStatCard.java`](../../src/main/java/com/trainticket/view/admin/AdminStatCard.java) | `com.trainticket.view.admin` | Reusable 50px borderless metric card displaying monumental Bebas Neue headers, large bold numeric metric values, and subtle slate color tokens. |
| [`AdminTopBar.java`](../../src/main/java/com/trainticket/view/admin/AdminTopBar.java) | `com.trainticket.view.admin` | Top command bar for Station Master dashboard with 1-second live digital clock, HikariCP database connectivity telemetry badge, and custom-painted sign-out pill button. |
| [`AdminSidebar.java`](../../src/main/java/com/trainticket/view/admin/AdminSidebar.java) | `com.trainticket.view.admin` | Operational left sidebar for the Station Master dashboard featuring monumental Bebas Neue branding, 5 navigation pill toggles, and operator credential display. |
| [`ModelEntitiesTest.java`](../../src/test/java/com/trainticket/model/ModelEntitiesTest.java) | `com.trainticket.model` | Comprehensive unit test suite covering domain entities, enums, day bitmasks, seat inventories, quota surge multipliers, concessions, and booking passengers. |
| [`DaoAndSessionTest.java`](../../src/test/java/com/trainticket/model/dao/DaoAndSessionTest.java) | `com.trainticket.model.dao` | Unit test suite validating StationDAO lookups/autocomplete, TrainDAO route queries, UserDAO persistence, and AuthSession lifecycle transitions. |
| [`DatabaseSeederTest.java`](../../src/test/java/com/trainticket/util/DatabaseSeederTest.java) | `com.trainticket.util` | Unit test suite verifying schema.sql statement parsing, table definitions, seed assertions, and application properties templates. |
---

## 4. Code Quality & Compiler Standards

All Java classes in RailFlow strictly adhere to zero-warning compilation under `javac -Xlint:all`:
- **Swing Serialization**: All 18 custom Swing components, windows, dialogs, and panels declare explicit `private static final long serialVersionUID = 1L;` preventing runtime `InvalidClassException` and eliminating `[serial]` lint warnings.
- **Resource Management**: All file and classpath streams (`getResourceAsStream`) are guarded using Java 9+ try-with-resources with effectively final references (`try (InputStream stream = in)`).
- **Graceful Offline Fallback**: Database connection checks (`DatabaseConnectionPool.isAvailable()`) precede all pool access in `StationDAO`, `TrainDAO`, and `UserDAO`, logging clean informational messages and bypassing network connection attempts when running standalone without MySQL.

