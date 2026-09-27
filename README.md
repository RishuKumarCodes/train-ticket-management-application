# 🚅 RailFlow — Train Ticket Management Desktop Application

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![GUI](https://img.shields.io/badge/GUI-Swing%20%2B%20FlatLaf-blue.svg?style=flat-square)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%20%2B%20HikariCP-informational.svg?style=flat-square)](docs/02-system-design/database-design.md)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)

**RailFlow** is a comprehensive, production-grade desktop railway reservation and fleet management system built with **Java 21**, **Swing**, **JDBC**, and **MySQL**. Engineered with a clean **Model-View-Controller (MVC)** architecture, RailFlow provides passengers with an intuitive ticket booking experience and empowers railway administrators with complete operational control over trains, schedules, routes, and revenue analytics.

---

## ✨ Core Features & Modules

### 1. 🔍 Train Discovery & Route Search
- **Multi-Criteria Search**: Search trains by source station, destination, and travel date with instant station code autocompletion.
- **Live Schedule & Intermediate Halts**: View complete route timetables, stop sequences, arrival/departure timings, and platform distance calculations.
- **Coach Class & Fare Breakdown**: Compare real-time pricing and seat quotas across classes (**1A, 2A, 3A, Sleeper, Chair Car**).
- **Seat Availability Indicators**: Instant visibility into status: Available, RAC (Reservation Against Cancellation), and Waitlist (WL).

### 2. 💺 Interactive Coach Map & Berth Selection
- **Visual Coach Layout**: Interactive graphical coach schematics reflecting standard railway seating plans.
- **Berth Preference Selection**: Choose preferred berths (Lower, Middle, Upper, Side-Lower, Side-Upper, Window, Aisle).
- **Real-Time Seat Locking**: Visual status updates preventing duplicate seat allocations during concurrent bookings.

### 3. 🎫 Reservation & PNR Management
- **Multi-Passenger Bookings**: Reserve tickets for multiple passengers with individual seat allocations under a single transaction.
- **10-Digit PNR Generation**: Instant generation of unique Passenger Name Record (PNR) numbers for tracking.
- **E-Ticket Generation**: Clean, printable booking summaries with passenger itineraries, coach/seat labels, and fare breakdown.
- **Live PNR Inquiry**: Check reservation confirmation status, journey details, and seat allocations anytime.

### 4. 🔄 Cancellation & Automated Refunds
- **Full & Partial Cancellation**: Cancel tickets for all or selected passengers in a booking.
- **Automated Refund Calculation**: Tiered refund deduction rules based on time of cancellation prior to scheduled departure.
- **Status Synchronization**: Cancelled seats immediately return to the inventory pool or promote waitlisted passengers.

### 5. 🛠️ Administrative & Fleet Operations
- **Train Roster Management**: Add, update, and manage train metadata, coach compositions, and total capacities.
- **Route & Timetable Configuration**: Define stations, intermediate stops, distance matrices, and scheduled run frequencies.
- **Occupancy & Delay Monitoring**: Update real-time train statuses (`ON_TIME`, `DELAYED`, `CANCELLED`) with delay notices.
- **Sales & Revenue Analytics**: View daily booking volumes, coach occupancy percentages, and earnings reports.

### 6. 💻 Modern Desktop User Experience
- **Fluid & Responsive UI**: Clean dark theme with smooth micro-interactions, intuitive icon navigation, and zero visual clutter.
- **Asynchronous Concurrency**: Heavy database operations run off the Swing Event Dispatch Thread (EDT), ensuring zero UI freezing.

---

## 🛠️ Technology Stack

| Layer | Component | Version | Purpose |
|---|---|---|---|
| **Core Platform** | **Java (OpenJDK)** | **21 LTS** | Modern Java features (Records, Pattern Matching, robust concurrency). |
| **User Interface** | **Java Swing + FlatLaf** | **3.5.4** | Clean desktop windowing, High-DPI screen scaling, and dark theme support. |
| **Persistence** | **JDBC** | ANSI SQL | High-throughput, parameterized SQL queries with full ACID transaction safety. |
| **Connection Pool** | **HikariCP** | **5.1.0** | Ultra-low latency database connection pooling. |
| **Database** | **MySQL Server** | **8.4+** | Relational data integrity for schedules, seats, users, and reservations. |
| **Build & Packaging** | **Apache Maven** | **3.8+** | Standardized dependency management and executable fat JAR compilation. |

---

## 🚀 Quick Start

### Prerequisites
- **JDK 21** or later installed (`java -version`)
- **Maven 3.8+** installed (`mvn -version`)
- **MySQL 8.0+** running locally or remotely

### Installation & Run
```bash
# 1. Clone repository
git clone https://github.com/RishuKumarCodes/train-ticket-management-application.git
cd train-ticket-management-application

# 2. Configure database credentials
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your MySQL username and password

# 3. Initialize database schema
mysql -u root -p < src/main/resources/db/schema.sql

# 4. Build and run
mvn clean compile exec:java
```

---

## 📚 Documentation Hub (Waterfall SDLC)

All technical designs, requirements, and academic final year deliverables are maintained as living documentation in [`docs/`](docs/):

| Phase | Title | Key Documents |
|---|---|---|
| **Phase 1** | **Requirements Analysis** | [Problem Statement & Scope](docs/01-feasibility-and-requirements/problem-statement-and-scope.md) • [Feasibility Study](docs/01-feasibility-and-requirements/feasibility-study.md) • [IEEE 830 SRS](docs/01-feasibility-and-requirements/srs.md) |
| **Phase 2** | **System Design** | [MVC Architecture](docs/02-system-design/high-level-architecture.md) • [Database Design & ERD](docs/02-system-design/database-design.md) • [UI/UX Design System](docs/02-system-design/ui-ux-design-system.md) |
| **Phase 3** | **Implementation** | [Module Specifications](docs/03-implementation/module-specifications.md) • [Coding Standards](docs/03-implementation/coding-standards.md) |
| **Phase 4** | **Testing & QA** | [Software Test Plan](docs/04-testing/test-plan.md) • [Requirements Traceability Matrix (RTM)](docs/04-testing/requirements-traceability-matrix.md) |
| **Phase 5** | **Operations** | [Installation Guide](docs/05-user-and-admin-manual/installation-guide.md) • [User & Admin Manual](docs/05-user-and-admin-manual/user-guide.md) |
| **Phase 6** | **Academic Report** | [Final Year Report Template](docs/06-final-report/college-report-template.md) • [Viva Defense Guide](docs/06-final-report/viva-presentation-guide.md) |

---

## 📂 Project Architecture Snapshot

```text
train-ticket-management-application/
├── docs/                   # Phased Waterfall documentation hub (Phases 1-6)
├── src/
│   ├── main/java/          # Model (DAOs/Entities), View (Swing UI), Controller
│   └── main/resources/     # SQL schema, DB config, vector icons, media assets
├── pom.xml                 # Maven build & dependencies descriptor
├── CONTRIBUTING.md         # Contribution guidelines & Git workflow
├── AGENTS.md               # AI engineering instructions & Prime Directive
└── README.md               # Project documentation
```

---

## 🤝 Contributing
We adhere to a strict **Docs-as-Code** standard: any code modification must be accompanied by an update to the corresponding documentation in `docs/`. Please review [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
