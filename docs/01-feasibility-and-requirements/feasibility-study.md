# Feasibility Study Report
## RailFlow — Train Ticket Management Application
**Project Phase:** Phase 1 (Waterfall Inception & Analysis)  

---

## 1. Technical Feasibility
- **Technology Stack**: Java 21, Swing, FlatLaf, HikariCP, MySQL 8.x.
- **Feasibility Assessment**: High.
  - Java 21 provides enterprise-grade stability, long-term support (LTS), and modern runtime optimizations.
  - Swing enables direct control over `Graphics2D` pipelines, allowing high-performance double-buffered rendering for custom spring-physics animations without external heavy dependencies.
  - FlatLaf provides cross-platform High-DPI support, dynamic dark mode, and vector SVG rasterization.
  - HikariCP guarantees sub-millisecond connection pooling overhead.

---

## 2. Operational Feasibility
- **Target Users**: General railway passengers and railway administrative operators.
- **Usability Factors**:
  - The interface follows strict minimalist principles with intuitive visual affordances, replacing clunky traditional railway forms with responsive 24px pill controls and interactive coach seat maps.
  - Fluid spring animations provide immediate tactile feedback, reducing operator error and double-booking mistakes.

---

## 3. Economic Feasibility
- **Cost Analysis**:
  - **Development Cost**: $0 (100% open-source software stack: Java OpenJDK, FlatLaf, MySQL Community Edition, Maven, VS Code/IntelliJ Community).
  - **Deployment Hardware**: Standard consumer PC/laptop with 4GB+ RAM and standard dual-core processor.
- **Feasibility Assessment**: Highly Feasible with zero licensing expenditure.

---

## 4. Schedule & Waterfall Milestones

```mermaid
gantt
    title Waterfall Lifecycle Schedule
    dateFormat  YYYY-MM-DD
    section Phase 1: Requirements
    Problem Definition & Scope    :done, 2026-09-01, 7d
    SRS Documentation & Sign-off  :done, 2026-09-08, 7d
    section Phase 2: Design
    System & Database Architecture :active, 2026-09-15, 10d
    UI/UX Specs & Wireframes       :active, 2026-09-20, 8d
    section Phase 3: Implementation
    Model & DAO Layer              :2026-10-01, 14d
    View & Animation Engine        :2026-10-15, 14d
    Controller & Integration       :2026-10-29, 10d
    section Phase 4: Testing
    Unit & Integration Testing     :2026-11-10, 10d
    System Verification & RTM      :2026-11-20, 8d
    section Phase 5: Final Report
    Thesis Compilation & Defense   :2026-12-01, 14d
```
