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




