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
| [`Main.java`](../../src/main/java/com/trainticket/Main.java) | `com.trainticket` | Application bootstrap, FlatLaf Dark theme initialization, macOS properties, and EDT dispatch. |
| [`MainFrame.java`](../../src/main/java/com/trainticket/view/MainFrame.java) | `com.trainticket.view` | Primary `JFrame` window with `JLayeredPane` root, OS Taskbar / macOS Dock icon binding (`icon.png` squircle), `icon-wide.png` header branding, and background video lifecycle integration. |
| [`VideoBackgroundPanel.java`](../../src/main/java/com/trainticket/view/component/VideoBackgroundPanel.java) | `com.trainticket.view.component` | High-performance JavaFX `MediaPlayer` embedded inside Swing (`JFXPanel`); renders looping `hero.mp4` with dynamic cover-scaling, dark tint overlay, and window lifecycle pause/resume. |
| [`HomeView.java`](../../src/main/java/com/trainticket/view/pages/HomeView.java) | `com.trainticket.view.pages` | Initial landing page featuring 24px glassmorphic train search card, `FlatLineBorder` subtle outline, pill inputs, and quick action shortcuts. |
| [`AssetManager.java`](../../src/main/java/com/trainticket/util/AssetManager.java) | `com.trainticket.util` | Classpath resource media loader with in-memory caching for images, squircles, and vector icons. |
| [`AudioManager.java`](../../src/main/java/com/trainticket/util/AudioManager.java) | `com.trainticket.util` | High-fidelity ambient audio engine using Java Sound (`javax.sound.sampled`) and native macOS CoreAudio device tracking; routes sound to the exact active OS output source, auto-migrates live streams when OS devices change, and supports hardware output switching. |
| [`DatabaseConnectionPool.java`](../../src/main/java/com/trainticket/util/db/DatabaseConnectionPool.java) | `com.trainticket.util.db` | High-performance HikariCP connection pool; loads credentials and handles SQL connection lifecycles. |


