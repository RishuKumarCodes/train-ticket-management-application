# 🚅 RailFlow — Train Ticket Management Desktop Application

[![Java](https://img.shields.io/badge/Java-17%20%2F%2021%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![GUI](https://img.shields.io/badge/GUI-Swing%20%2B%20FlatLaf-blue.svg?style=flat-square)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%20%2B%20HikariCP-informational.svg?style=flat-square)](src/main/resources/db/schema.sql)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)

**RailFlow** is a modern desktop railway reservation and fleet management system built with **Java**, **Swing**, **JDBC**, and **MySQL**. Engineered with a clean **Model-View-Controller (MVC)** architecture, RailFlow provides passengers with an intuitive ticket booking experience and empowers railway administrators with complete operational control over train rosters, schedules, routes, and seat occupancy.

---

## ✨ Application Features

- **Train Search & Discovery**: Search by origin and destination stations with instant autocompletion, real-time schedule timetables, intermediate halts, and fare comparisons across coach tiers (1A, 2A, 3A, Sleeper, Chair Car).
- **Interactive Coach & Seat Map**: Visual coach schematics displaying Lower, Middle, Upper, Side Berths, and Window seats with real-time seat locking to prevent double bookings.
- **Reservation & PNR Tracking**: Multi-passenger booking transactions, automated unique 10-digit PNR generation, and printable e-ticket summaries.
- **Cancellations & Automated Refunds**: Full or partial ticket cancellation with automated tiered refund calculations based on departure timetable rules.
- **Admin Fleet & Schedule Operations**: Full-page Station Master operations center with live digital clock, fleet rosters, route halts, booking manifests, and real-time database telemetry.
- **Modern Desktop Experience**: FlatLaf Universal Light theme, 50px borderless cards (`arc: 100`), full rounded pill buttons (`arc: 999`), monumental Bebas Neue display typography, zero header clutter, and 60 FPS spring animations.
- **Modular Component Architecture**: High cohesion, decoupled MVC design (`AppHeaderPanel`, `TrainResultCard`, `CoachClassPillButton`, `BookingTicketCard`, `AdminSidebar`, `AdminTopBar`, `AdminStatCard`).
- **Comprehensive Verification**: 51 passing unit tests with lightweight standalone runner and zero compiler warnings under `javac -Xlint:all`.

---

## 📂 Source Code Structure (`src/`)

```text
train-ticket-management-application/
├── docs/                                  # Living Waterfall documentation hub (Phases 1 to 6)
├── .agents/                               # AI engineering rules & Prime Directive
├── pom.xml                                # Maven build descriptor and dependencies
├── CONTRIBUTING.md                        # Contribution guidelines & Git workflow
├── AGENTS.md                              # AI instructions & documentation sync rules
├── README.md                              # Project landing page (this file)
│
└── src/
    ├── main/
    │   ├── java/com/trainticket/
    │   │   ├── Main.java                  # Application entry point: initializes FlatLaf Universal Light theme & launches MainFrame on EDT
    │   │   ├── view/
    │   │   │   ├── MainFrame.java         # Root desktop window (1280x820), 4-page router, and background video lifecycle
    │   │   │   ├── component/
    │   │   │   │   ├── navigation/        # AppHeaderPanel (brand logo, CoreAudio sound menu, 3-tab navigation capsule, profile button)
    │   │   │   │   ├── train/             # Reusable TrainResultCard & CoachClassPillButton
    │   │   │   │   ├── card/              # BookingTicketCard (confirmed booking card with berth details and cancellation action)
    │   │   │   │   ├── selector/          # ModernSmoothDropdown & ModernDatePicker
    │   │   │   │   ├── home/              # HeroSection, SearchCapsulePanel, FeaturedDestinationsSection
    │   │   │   │   └── VideoBackgroundPanel.java # JavaFX MediaPlayer looping hero.mp4
    │   │   │   ├── admin/                 # AdminDashboardView, AdminSidebar, AdminTopBar, AdminStatCard
    │   │   │   ├── pages/                 # HomeView, TrainsPageView, MyBookingsPageView
    │   │   │   └── dialog/                # ModernModalDialog, AuthDialog, AdminLoginDialog, BookingDialog, TrainRouteTimetableDialog
    │   │   ├── model/                     # Domain entities, DTOs, and JDBC DAOs with thread-safe in-memory dev fallbacks
    │   │   ├── controller/                # AuthController & TrainSearchController (asynchronous SwingWorker tasks)
    │   │   └── util/                      # AssetManager, AudioManager, PasswordUtils, DatabaseConnectionPool, DatabaseSeeder
    │   │
    │   └── resources/
    │       ├── application.properties.example # DB credentials (URL, user, password), connection pool, and window size defaults
    │       ├── logback.xml                # Structured console logging configuration
    │       ├── assets/
    │       │   ├── icons/
    │       │   │   ├── icon.png           # Official macOS HIG squircle application icon (512x512 superellipse)
    │       │   │   ├── icon-wide.png      # High-resolution 5:1 wide brand header banner (2000x400)
    │       │   │   ├── speaker-on.svg     # Vector icon for active sound playback
    │       │   │   └── speaker-off.svg    # Vector icon for muted sound state
    │       │   ├── videos/
    │       │   │   └── hero.mp4           # 1080p ambient journey video loop asset
    │       │   ├── audio/
    │       │   │   └── hero.mp3           # Ambient journey stereo audio track loop
    │       │   ├── fonts/                 # Custom typefaces
    │       │   └── images/                # Background graphics and train textures
    │       │
    │       └── db/
    │           └── schema.sql             # SQL database initialization script
    │
    └── test/java/com/trainticket/         # Automated JUnit 5 unit and integration tests
```

---

## 🚀 How to Setup and Run

### 1. Prerequisites
- **Java Development Kit (JDK)**: OpenJDK 17 LTS or 21 LTS installed (`java -version`).
- **Apache Maven**: Version 3.8 or higher (`mvn -version`).
- **MySQL Server**: MySQL 8.0+ running locally or on a remote server.

---

### 2. Setup Database Credentials
1. Create your local properties file from the example template:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Open `src/main/resources/application.properties` in your editor and update your database credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/train_ticket_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.username=your_mysql_username
   db.password=your_mysql_password
   ```

---

### 3. Initialize & Seed Database
You can initialize the database tables, stations, routes, and admin user using any of the following methods:

#### Method 1: Using the Automated Java Seeder (Recommended)
```bash
# Via Maven
mvn exec:java -Dexec.args="seed"

# Or via RailFlow CLI script
./RailFlow.command seed

# Or double-click SeedDatabase.command in macOS Finder
```

#### Method 2: Direct MySQL CLI Import
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```
*(Note: If MySQL is not running locally, RailFlow automatically falls back to an in-memory thread-safe dataset for instant zero-config testing).*


---

### 4. Run the Application

You can launch RailFlow using any of the following methods:

#### Method A: Live-Reload Dev Mode (Recommended for Development)
Automatically watches `src/main` for any code or asset changes, recompiles incrementally, and restarts the application automatically on save:
```bash
./dev.sh
```

#### Method B: One-Click macOS Desktop Launcher
You can double-click **`RailFlow.command`** directly in macOS Finder to launch the app without opening terminal.

#### Method C: Standard Terminal Launch
```bash
mvn exec:java
```

#### Method D: In-App Hot-Reload Shortcut
While RailFlow is running, press **`⌘ + R`** (or **`F5`** / **`Ctrl + R`**) inside the application window to instantly hot-reload and repaint the active view without restarting the JVM!

#### Method E: Directly in Your IDE (VS Code or IntelliJ IDEA)
1. Open the project folder in your IDE.
2. Navigate to [`src/main/java/com/trainticket/Main.java`](src/main/java/com/trainticket/Main.java).
3. Click the **Run ▶** button above `public static void main(String[] args)`.

#### Method F: Build Standalone Executable (.JAR)
```bash
# Package into a single executable JAR with all dependencies bundled
mvn clean package

# Run the compiled JAR
java -jar target/train-ticket-management-1.0.0-SNAPSHOT.jar
```

---

## 📚 Project Documentation & Governance

- **[`docs/`](docs/)**: Full living documentation hub organized by Waterfall SDLC phases (SRS, System Architecture, Database Design, Test Plan & RTM, User Manual, and University Thesis template).
- **[`CONTRIBUTING.md`](CONTRIBUTING.md)**: Team Git workflow, Conventional Commit guidelines, and Docs-as-Code synchronization rules.
- **[`AGENTS.md`](AGENTS.md)**: AI engineering guidelines, EDT thread safety invariants, and documentation co-evolution directive.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
