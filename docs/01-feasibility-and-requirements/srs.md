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
| **REQ-TRN-01** | Train Search | The system shall permit multi-criteria train search by origin station, destination station, travel date, quota (General, Tatkal, Premium Tatkal, All AC), and concession (None, Divyangjan, Railway Pass). | High |
| **REQ-TRN-02** | Train Search | The search results stream shall display train name, number, type badge, live status, departure/arrival timings, duration, distance, coach availability pills (1A, 2A, 3A, SL, CC, EC), real-time seat counts (AVAILABLE, RAC, WL), dynamic pricing, and sorting filters. | High |
| **REQ-TRN-03** | Train Timetable | The system shall display a station-wise route timetable and live progress tracker dialog showing ordered halts, arrival/departure times, halt minutes, cumulative km, and transit status. | High |
| **REQ-TRN-04** | Reservation | The system shall provide an instant passenger booking modal capturing traveler credentials, displaying fare breakdown, and generating 10-digit PNR tickets. | High |
| **REQ-SEAT-01** | Seat Layout | The system shall display an interactive visual coach seat grid with live status (Available, Booked, Selected). | High |
| **REQ-BKG-01** | Reservation | The system shall generate a unique 10-character alphanumeric PNR for every confirmed booking. | Critical |
| **REQ-BKG-02** | Reservation | The system shall execute seat booking and payment recording as an atomic ACID transaction. | Critical |
| **REQ-CNL-01** | Cancellation | The system shall allow passengers to cancel confirmed tickets and calculate refund amounts according to cancellation rules. | Medium |
| **REQ-ADM-01** | Administration | The admin module shall support full CRUD operations on trains, routes, stations, and coach layouts. | High |
| **REQ-ADM-02** | Reports | The system shall generate daily/weekly booking analytics and revenue reports. | Medium |
| **REQ-NAV-01** | Navigation | The system shall provide a persistent 3-tab passenger navigation capsule (Book Journey, Trains, My Bookings) allowing instantaneous view switching with fluid pill state transitions. | High |
| **REQ-TRN-05** | Train Discovery | The system shall render a dedicated Trains search page (`TrainsPageView`) hosting an interactive pre-filled search capsule header, route summary chips, sorting filters, and live train result cards with instant booking hooks. | High |
| **REQ-BKG-03** | Passenger History | The system shall provide a dedicated My Bookings page (`MyBookingsPageView`) with guest authentication prompts and authenticated booking histories, displaying confirmed e-ticket cards, PNR statuses, passenger lists, and cancellation actions. | High |
| **REQ-ADM-03** | Administration | When authenticating as Station Master, the application shall transition to an embedded full-page Admin Command Center (`AdminDashboardView`) in `MainFrame`, eliminating popup windows while providing live fleet management, halts, booking manifests, and clean return to passenger view. | High |
| **REQ-DB-02** | Master Seeder | The system shall provide automated database seeding via `DatabaseSeeder`, `./RailFlow.command seed`, and `SeedDatabase.command` to reset stations, trains, routes, and admin user data. | High |
| **REQ-UI-01** | Animated Selectors | The system shall provide custom animated dropdown selectors (`ModernSmoothDropdown`) with transparent floating window cards, 60 FPS cubic spring drop-down motion, rotating chevron indicator, subtitle badges, and click-outside dismissal. | High |
| **REQ-UI-02** | Date Picker | The system shall provide a custom minimalist calendar picker (`ModernDatePicker`) with transparent floating card, Bebas Neue month headers, quick shortcut presets (`Today`, `Tomorrow`, `+7 Days`), day grid with past-date disabling, and circular selection highlights. | High |
| **REQ-UI-03** | Station Autocomplete & Capsule Width Stability | The system shall provide a custom animated station autocomplete dropdown (`StationAutocompleteDropdown`) with real-time station matching, code pills (`[NDLS]`), keyboard navigation, screen collision protection, and strict catalog validation (reverting invalid manual inputs), while stabilizing the search capsule layout so origin and destination fields evenly consume remaining space without width jumping during typing. | High |
| **REQ-AUD-01** | CoreAudio Routing | The system shall track native macOS CoreAudio output devices via `AudioManager`, route ambient sounds to the active hardware device, and auto-migrate live streams when OS devices change. | Medium |
| **REQ-ARCH-01** | Modularization | The view layer shall decompose large monolithic classes into reusable components (`TrainResultCard`, `CoachClassPillButton`, `BookingTicketCard`, `AppHeaderPanel`, `AdminSidebar`, `AdminTopBar`, `AdminStatCard`) eliminating all duplicate code. | High |
| **REQ-NAV-02** | Navigation | The navigation capsule shall adapt its visual style based on the active page context: dark smoked-glass pill with white text on the Home (video) page; frosted-white pill with dark text on light-canvas pages (Trains, My Bookings). The active tab indicator shall use a white fill on Home and a black (`#0F172A`) fill on light pages. | High |
| **REQ-AUD-02** | Audio | The ambient music toggle button shall be visible exclusively on the Home (Book Journey) page. When the user navigates away from Home, ambient audio shall stop automatically and the music button shall hide. The button shall reappear when the user returns to the Home page. | High |
| **REQ-UI-04** | Plan My Trip Page | The system shall provide a dedicated standalone "Plan My Trip" full-page view (`PlanMyTripView`) with its own page header (orange pill back-arrow button, Bebas Neue "PLAN MY TRIP" title, "India • All Destinations" location pill) and a scrollable 3-column grid of `DestinationImageCard` tiles rendered on the light `#F8FAFC` canvas without any video background or shared `AppHeaderPanel`. | High |

### 3.2 Non-Functional Requirements

| Requirement ID | Category | Metric / Specification |
|---|---|---|
| **NFR-PERF-01** | Performance | Database search queries shall return results within 250ms for up to 100,000 records. |
| **NFR-UI-01** | Aesthetics & Motion | All animations shall execute at a steady 60 FPS (16ms loop) using volume-conserving squash/stretch spring physics. |
| **NFR-UI-02** | Geometry | All buttons shall be fully rounded pill buttons or have an explicit minimum 24px border radius. |
| **NFR-UI-03** | Minimalism | Zero unnecessary or repetitive explanatory text; UI must be sleek, glassmorphic, and visually consistent across all views. |
| **NFR-REL-01** | Reliability & Safety | UI thread (EDT) shall never be blocked by database I/O or background processing. |
| **NFR-SEC-01** | Security | Passwords shall be hashed before database persistence; all SQL queries shall use `PreparedStatement` to eliminate SQL injection. |
