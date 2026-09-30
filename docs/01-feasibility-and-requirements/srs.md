# Software Requirements Specification (SRS)
## RailFlow — Train Ticket Management Application
**Document Standard:** IEEE Std 830-1998 / ISO/IEC/IEEE 29148:2018  
**Project Type:** College Final Year Capstone Project (Waterfall SDLC)  
**Status:** Baseline Approved  

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) document provides a complete description of all functional and non-functional requirements for **RailFlow: Train Ticket Management Desktop Application**. It serves as the formal baseline agreement for system design and test verification in accordance with the Waterfall development model.

### 1.2 Scope
RailFlow is a native Java 21 desktop application designed for high-performance railway ticket searching, seat booking, schedule administration, and ticket lifecycle management with liquid, jelly-spring UI animations.

### 1.3 Definitions, Acronyms, and Abbreviations
- **EDT**: Event Dispatch Thread (Swing GUI thread).
- **PNR**: Passenger Name Record (Unique 10-character booking identifier).
- **RTM**: Requirements Traceability Matrix.
- **ACID**: Atomicity, Consistency, Isolation, Durability.
- **RAC**: Reservation Against Cancellation.
- **WL**: Waitlist.

---

## 2. Overall Description

### 2.1 Product Perspective
RailFlow is a standalone 3-tier desktop application built using strict Model-View-Controller (MVC) architecture, communicating with a relational SQL database via JDBC and HikariCP connection pooling.

```
+---------------------+     SQL / JDBC      +------------------------+
| Swing Desktop View  | <-----------------> | Relational DB (MySQL)  |
| (Jelly UI Motion)   |   via Controller    | (ACID Transactions)    |
+---------------------+                     +------------------------+
```

### 2.2 User Classes and Characteristics
1. **Passenger / General User**:
   - Searches trains between source and destination stations.
   - Views real-time seat availability and selects coach/berth.
   - Books tickets, makes mock payments, downloads/views e-tickets, and cancels bookings.
2. **Station Master / Railway Admin**:
   - Manages train rosters, coaches, stations, and routes.
   - Adjusts schedules, monitors occupancy rates, and reviews revenue.

### 2.3 Operating Environment
- **Operating Systems**: macOS (Sonoma+), Windows (10/11), Linux (Ubuntu 22.04+).
- **Java Runtime**: OpenJDK / Oracle JDK 21 LTS or higher.
- **Database Engine**: MySQL 8.0+ / ANSI-SQL compatible database.

---

## 3. Specific Requirements (Numbered for Traceability)

### 3.1 Functional Requirements

| Requirement ID | Module | Description | Priority |
|---|---|---|---|
| **REQ-AUTH-01** | Authentication | The system shall support optional passenger login with registration (name, email or mobile phone, password) allowing passengers to authenticate via either email or phone while allowing guest exploration. | High |
| **REQ-AUTH-02** | Authentication | The system shall enforce Role-Based Access Control: passengers access customer booking flows; administrators authenticate via fixed credentials (`admin`) into a dedicated Station Master Command Console (no admin registration). | High |
| **REQ-TRN-01** | Train Search | The system shall permit train search by source station, destination station, and travel date. | High |
| **REQ-TRN-02** | Train Search | The search results shall display train number, name, departure time, arrival time, duration, and fare by coach type. | High |
| **REQ-SEAT-01** | Seat Layout | The system shall display an interactive visual coach seat grid with live status (Available, Booked, Selected). | High |
| **REQ-BKG-01** | Reservation | The system shall generate a unique 10-character alphanumeric PNR for every confirmed booking. | Critical |
| **REQ-BKG-02** | Reservation | The system shall execute seat booking and payment recording as an atomic ACID transaction. | Critical |
| **REQ-CNL-01** | Cancellation | The system shall allow passengers to cancel confirmed tickets and calculate refund amounts according to cancellation rules. | Medium |
| **REQ-ADM-01** | Administration | The admin module shall support full CRUD operations on trains, routes, stations, and coach layouts. | High |
| **REQ-ADM-02** | Reports | The system shall generate daily/weekly booking analytics and revenue reports. | Medium |

### 3.2 Non-Functional Requirements

| Requirement ID | Category | Metric / Specification |
|---|---|---|
| **NFR-PERF-01** | Performance | Database search queries shall return results within 250ms for up to 100,000 records. |
| **NFR-UI-01** | Aesthetics & Motion | All animations shall execute at a steady 60 FPS (16ms loop) using volume-conserving squash/stretch spring physics. |
| **NFR-UI-02** | Geometry | All buttons shall be fully rounded pill buttons or have an explicit minimum 24px border radius. |
| **NFR-UI-03** | Minimalism | Zero unnecessary or repetitive explanatory text; UI must be sleek, glassmorphic, and visually consistent across all views. |
| **NFR-REL-01** | Reliability & Safety | UI thread (EDT) shall never be blocked by database I/O or background processing. |
| **NFR-SEC-01** | Security | Passwords shall be hashed before database persistence; all SQL queries shall use `PreparedStatement` to eliminate SQL injection. |
