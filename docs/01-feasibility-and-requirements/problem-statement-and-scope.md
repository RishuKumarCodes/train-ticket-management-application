# Problem Statement & Project Scope
## RailFlow — Train Ticket Management Application
**Project Phase:** Phase 1 (Waterfall Requirements)  

---

## 1. Problem Statement
Traditional railway reservation desktop interfaces (especially legacy Swing/AWT and terminal utilities) suffer from several critical shortcomings:
1. **Rigid, Unintuitive User Experience**: Stiff rectangular buttons, crowded form fields, and absence of visual feedback when reserving seats.
2. **UI Thread Freezing**: Poor concurrency design causes UI lockups during slow network or database lookups.
3. **Complex State Synchronization**: Disconnect between seat availability maps and real-time database locks.

RailFlow solves these issues by pairing an **enterprise-grade MVC architecture** (Java 21, JDBC, HikariCP, MySQL) with an **Apple-grade, 60 FPS liquid spring animation engine**, delivering responsive tactile feedback and reliable ACID transactional guarantees.

---

## 2. Project Scope & Boundaries

### In Scope
- Passenger account registration and authentication.
- Multi-criteria train searching by origin station, destination station, and journey date.
- Real-time coach schematic and seat selection (Window, Lower, Middle, Upper berths).
- PNR generation and simulated instant payment processing.
- Ticket lifecycle management (viewing, downloading summary, and cancellation with refund calculations).
- Administrative portal for managing trains, schedules, coaches, and revenue analytics.

### Out of Scope
- Real physical payment gateway integration (mock payment flow will be used instead).
- Live GPS hardware telemetry for train speed (scheduled timetable simulation will be used instead).

