# Contributing to RailFlow

Thank you for contributing to **RailFlow**! Whether you are a team member, a peer student, an open-source contributor, or an AI pair programmer, this guide defines our standards for engineering, file structure, and workflow.

---

## 🌟 The Prime Directives

1. **Strict MVC Architecture**: Zero business or SQL logic in Views; zero Swing/AWT imports in Models.
2. **24px Pill Geometry & Minimalist UI**: Buttons must be full rounded pills (`arc: 999` or `height / 2`) or have a minimum `24px` border radius. Zero visual clutter and no redundant explanatory text.
3. **The Swing EDT Invariant**: Never block the Event Dispatch Thread with database calls, file I/O, or heavy calculations. Always use `SwingWorker` or asynchronous executors.
4. **Mandatory Documentation Synchronization (Docs-as-Code)**: **No code change is complete without updating the corresponding documentation in `docs/` within the exact same pull request or commit.**

---

## 📂 Source Code Layout (`src/`)

When contributing, ensure code and resources are placed in their proper locations within `src/`:

```text
src/
├── main/
│   ├── java/com/trainticket/
│   │   ├── Main.java                  # Main application entrypoint: initializes FlatLaf dark theme & launches MainFrame on EDT
│   │   ├── view/
│   │   │   ├── MainFrame.java         # Root desktop window (1280x820), top navigation header, OS taskbar/dock icon integration
│   │   │   ├── component/             # Reusable UI components (VideoBackgroundPanel, home/ subcomponents)
│   │   │   └── pages/
│   │   │       └── HomeView.java      # Initial landing page with Hero, Featured Destinations & quick shortcuts
│   │   ├── model/                     # Domain entities, DTOs, and JDBC DAOs (added as database models are built)
│   │   ├── controller/                # User action listeners and asynchronous background workers (SwingWorker)
│   │   └── util/
│   │       └── AssetManager.java      # Classpath media asset loader with in-memory thread-safe image caching
│   │
│   └── resources/
│       ├── application.properties.example # Database credentials, connection pool settings, and window size defaults
│       ├── logback.xml                # Structured console logging configuration
│       ├── assets/
│       │   ├── icons/
│       │   │   └── icon.png           # Official high-resolution application icon (512x512)
│       │   ├── videos/hero.mp4        # Journey video loop asset
│       │   ├── audio/hero.mp3         # Ambient audio asset
│       │   ├── fonts/                 # Custom typefaces
│       │   └── images/                # Background graphics and train textures
│       │
│       └── db/
│           └── schema.sql             # SQL database initialization script
│
└── test/java/com/trainticket/         # Automated JUnit 5 unit and integration tests
```

*Note: Documentation files live in **`docs/`** and AI engineering guidelines live in **`.agents/`**.*

---

## 🛠️ How to Setup, Run & Verify Locally

### 1. Initial Setup
```bash
# 1. Clone repository
git clone https://github.com/RishuKumarCodes/train-ticket-management-application.git
cd train-ticket-management-application

# 2. Configure local credentials
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 3. Initialize MySQL database
mysql -u root -p < src/main/resources/db/schema.sql
```

### 2. Running the Application
```bash
# Launch via Maven
mvn clean compile exec:java

# Or run directly in your IDE by opening Main.java and clicking Run ▶
```

### 3. Pre-Commit Quality Checks
Before submitting a pull request or pushing commits, run:
```bash
# Compile and run all unit/integration tests
mvn test

# Verify package assembly
mvn package
```

---

## 📋 Git Workflow & Conventional Commits

We follow a structured branch naming and pull request model:

### Branch Naming
- `feature/<description>`: New functional capabilities (e.g. `feature/train-search`, `feature/coach-seat-grid`).
- `fix/<description>`: Bug and defect fixes (e.g. `fix/edt-concurrency-leak`).
- `docs/<description>`: Documentation updates (e.g. `docs/rtm-matrix-update`).

### Conventional Commit Format
```text
feat(booking): implement atomic seat reservation with HikariCP
fix(view): adjust button padding and hover glow on HomeView
docs(srs): add REQ-AUTH-03 for two-factor verification
test(dao): add rollback unit test for seat reservation collision
```

---

## 📑 The Co-Evolution Matrix (Mandatory Docs Sync)

Before your Pull Request can be merged, verify that all applicable documentation has been updated:

| If You Modify... | You MUST Update... |
|---|---|
| Database Schema (`src/main/resources/db/schema.sql`) | [`docs/02-system-design/database-design.md`](docs/02-system-design/database-design.md) |
| Features or Requirements | [`docs/01-feasibility-and-requirements/srs.md`](docs/01-feasibility-and-requirements/srs.md) & [`docs/04-testing/requirements-traceability-matrix.md`](docs/04-testing/requirements-traceability-matrix.md) |
| UI Components or Styling | [`docs/02-system-design/ui-ux-design-system.md`](docs/02-system-design/ui-ux-design-system.md) |
| Package Structure or Controllers | [`docs/03-implementation/module-specifications.md`](docs/03-implementation/module-specifications.md) |
| Test Suites | [`docs/04-testing/test-plan.md`](docs/04-testing/test-plan.md) & RTM status |

---

## 🤝 Need Help?
Check out our [Documentation Hub](docs/README.md) or open an issue on the repository.
