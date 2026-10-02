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
| **REQ-SRCH-04** | Search Interaction | The search capsule shall provide an interactive Swap Route button (⇄) with animated 180° damped harmonic overshoot rotation that interchanges origin and destination stations and triggers an automatic real-time search re-query. | High |
| **REQ-PNR-01** | PNR Enquiry | The system shall provide a standalone PNR Status Enquiry dialog (`PnrStatusDialog`) accessible directly from the My Bookings page (`MyBookingsPageView`) without requiring login, supporting 10-digit PNR lookups, real-time coach/seat allocation, charting status, and passenger cancellation. | High |
| **REQ-SEAT-01** | Seat Layout | The system shall display an interactive visual coach seat map (`CoachSeatSelectionDialog`, `CoachSeatMapPanel`, `SeatButton`) rendering 2D interior layouts (bays, aisles, washrooms), supporting multiple coach classes (3AC, 2AC, 1AC, CC, SL), coach switching (B1–B4), seat capacity enforcement, and passenger berth allocation. | High |
| **REQ-PAY-01** | Payment Gateway | The system shall provide a realistic mock payment gateway modal (`PaymentGatewayDialog`) supporting UPI QR codes with a live 5-minute countdown, Credit/Debit cards, Net Banking (SBI, HDFC, ICICI, Axis), and 1-click RailFlow Wallet checkout with NPCI authorization simulation. | High |
| **REQ-BKG-01** | Reservation | The system shall generate a unique 10-character alphanumeric PNR for every confirmed booking. | Critical |
| **REQ-BKG-02** | Reservation | The system shall execute seat booking and payment recording as an atomic ACID transaction. | Critical |
| **REQ-CNL-01** | Tiered IRCTC Cancellation & Refund Engine | The system shall provide an IRCTC-compliant refund calculation engine (`CancellationRefundEngine`) calculating tiered deductions based on time-to-departure (>48h flat minimum per class, 12–48h 25% fee, 4–12h 50% fee, <4h non-refundable) and passenger counts. | High |
| **REQ-CNL-02** | Liquid Cancellation Modal Dialog | The system shall provide a bespoke animated cancellation modal (`CancelTicketModalDialog`) extending `ModernModalDialog` with journey recap, transparent fare deduction breakdown, cancellation reason selector, and instant refund transaction confirmation. | High |
| **REQ-CNL-03** | Database Persistence & E-Ticket Cancellation State | The system shall execute atomic SQL transactions for ticket cancellation in `BookingDAO`, replenish reserved seat inventory in `TrainDAO`, update passenger status, and display a prominent cancellation alert banner on digital e-tickets (`ETicketPassDialog`). | High |
| **REQ-ADM-01** | Administration | The admin module shall support full CRUD operations on trains, routes, stations, and coach layouts. | High |
| **REQ-ADM-02** | Reports | The system shall generate daily/weekly booking analytics and revenue reports. | Medium |
| **REQ-NAV-01** | Navigation | The system shall provide a persistent 3-tab passenger navigation capsule (Book Journey, Trains, My Bookings) allowing instantaneous view switching with fluid pill state transitions. | High |
| **REQ-TRN-05** | Route Search Results Page | When searching from the Home page, the system shall render a dedicated Route Search Results page (`TrainSearchResultsView`) hosting a pre-filled top route search capsule with station swapping, route summary chips, sorting filters, and live train result cards with instant booking hooks. | High |
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
| **REQ-UI-04** | Plan My Trip Page | The system shall provide a dedicated standalone "Plan My Trip" full-page view (`PlanMyTripView`) featuring a fixed floating back button anchored at the top-left in the layered palette layer, a single scrolling header ("FEATURED DESTINATIONS", "Explore iconic places across India by train") that scrolls naturally with page content, and a responsive fluid card grid (`ResponsiveCardGridLayout`) displaying 2 to 5 columns dynamically with edge-to-edge width distribution and 4:3 portrait aspect ratio scaling on the light `#EEF2F6` canvas without any video background or shared `AppHeaderPanel`. | High |
| **REQ-ADM-04** | Fleet Commissioning | The Station Master shall be able to commission a new train into the fleet (`AddTrainDialog`) with train number, name, type, origin/destination, running days bitmask, coach class capacities, base fares, and route halts. | High |
| **REQ-ADM-05** | Operational Status & Delay Broadcaster | The Station Master shall be able to update train operational status (`ON_TIME`, `DELAYED`, `CANCELLED`, `DEPARTED`) and broadcast delay minutes (`EditTrainStatusDialog`), which shall reflect immediately on passenger search cards and timetable trackers. | High |
| **REQ-ADM-06** | Fleet Retirement | The Station Master shall be able to decommission and retire trains from the active fleet with confirmation modal validation. | Medium |
| **REQ-ADM-07** | Dynamic Admin Metrics & Analytics | The Admin Overview dashboard shall dynamically compute active train counts, confirmed passenger counts, on-time dispatch rates, gross revenue in INR, and corridor performance bars from live operational data. | High |
| **REQ-ADM-08** | CSV Manifest & Fleet Export | The Station Master shall be able to export passenger booking manifests and train fleet rosters to RFC 4180-compliant CSV files (`CsvExportService`) via native save dialogs. | Medium |
| **REQ-BKG-04** | Inventory Decrement | The system shall atomically decrement available seats in the corresponding coach class upon booking confirmation, transitioning to RAC and Waitlist when capacity is exhausted. | Critical |
| **REQ-BKG-05** | Inventory Replenishment | When a booking is cancelled, reserved seat inventory shall be automatically replenished in both in-memory cache and MySQL database. | High |
| **REQ-PAS-01** | E-Ticket Pass Modal | The system shall provide a high-fidelity Electronic Reservation Slip digital pass dialog (`ETicketPassDialog`) featuring official RailFlow header, PNR badge, timetable stepper, passenger berth grid, fare breakdown receipt, barcode, and QR matrix graphic. | High |
| **REQ-PAS-02** | E-Ticket Print & Image Export | The E-Ticket pass dialog shall provide native operating system printing via Java 2D `PrinterJob` and 300 DPI PNG image export via `ImageIO`. | High |
| **REQ-USR-03** | Saved Passenger Master List | The system shall maintain a saved passenger master list (`PassengerMasterDAO`) allowing passengers to manage frequent co-travelers and auto-populate booking passenger rows with 1 click. | Medium |
| **REQ-TRK-01** | Unified Live Status & Route Timetable | The system shall provide a unified live train running status and route timetable dialog (`LiveTrainTrackerDialog`) displaying current transit position, delay minutes, platform track allocations, scheduled halt timings, cumulative distances, and 60 FPS live radar tracking, accessible directly from train result cards. | High |
| **REQ-TRN-06** | Fleet Discovery & Number Search in Trains Tab | The second tab (`TrainsPageView`) shall be dedicated to discovering the national fleet and searching by 5-digit train number or train name, providing instant filtering, flagship quick-chips, full journey route display, and direct triggers for Live Status / Timetable and Booking. | High |
| **REQ-USR-04** | Passenger Master List Management Dialog | The system shall provide a dedicated `SavedPassengersDialog` modal extending `ModernModalDialog` to view, add, and delete frequent travelers, accessible from `MyBookingsPageView` and automatically saving newly booked co-travelers to the user's master list. | Medium |
| **REQ-DST-01** | Featured Destination Trip Planning | Clicking any featured destination card on the Home page or Plan My Trip page shall open an interactive trip planning modal (`PlanDestinationTripDialog`) extending `ModernModalDialog`. It shall prompt for the start station (`FROM`), travel date, seating class, and quota. It shall perform real-time route validation; if direct trains are unavailable from the chosen starting station to the destination station, it shall display an in-popup warning pill and disable search. Clicking "FIND TRAINS ↗" shall navigate to `TrainSearchResultsView` displaying matching trains to that destination. | High |
| **REQ-ADM-09** | Featured Destinations Administration | The Station Master Administrator Command Center (`AdminDashboardView`) shall include a dedicated "Featured Destinations" panel (`DESTINATIONS`) featuring a responsive 2-column grid of borderless 50px cards (`AdminDestinationCard`) resting directly on the light canvas (eliminating nested card containers). Each destination card shall display an anti-aliased photo thumbnail on the left and metadata (monument name in Bebas Neue, location, station code badge) on the right, alongside dedicated `EDIT` and `DELETE` pill buttons (with deletion confirmation modal). The modal dialog (`AddDestinationDialog`) shall support both adding and editing destinations, enabling administrators to select and upload any image file from their PC via `JFileChooser` with live thumbnail preview, as well as choosing from bundled photography presets. Updates shall persist via `FeaturedDestinationDAO` and notify all views reactively. | High |
| **REQ-ADM-10** | Scrollable Analytics Overview Dashboard | The Station Master Administrator Overview tab shall be wrapped in a fluid, borderless `JScrollPane` (unit increment 20px, horizontal scroll disabled), eliminating rigid fixed top header bars that constrained vertical real estate. The view shall feature an integrated scrolling header ("ANALYTICS OVERVIEW" in Bebas Neue) with live 1-second digital clock and network health operational pills. | High |
| **REQ-ADM-11** | Vector Curved Area & Line Chart | The overview workspace shall include a custom vector-rendered spline area and line chart (`AnalyticsLineChart`) visualizing 7-day gross revenue and passenger traffic trends with cubic spline interpolation, gradient area fills, benchmark gridlines, glowing milestone nodes, and peak milestone callout badges. | High |
| **REQ-ADM-12** | Seat Class Demand Share Donut Chart | The overview workspace shall include a custom vector-rendered modern donut chart (`AnalyticsDonutChart`) displaying seat class demand and capacity distribution (3A, 2A, 1A, SL, CC/EC) with clean divider arcs, central capacity utilization percentage, and minimalist right-aligned legend. | High |
| **REQ-ADM-13** | Hourly Network Dispatch Histogram | The overview workspace shall include a custom vector-rendered column histogram (`AnalyticsBarChart`) illustrating 24-hour departure volume across 8 time windows with rounded capsule tops, highlighting morning and evening peak rush intervals in vibrant Brand Orange. | High |
| **REQ-ADM-14** | Elevated KPI Metric Cards with Trend Badges | The overview metrics row shall feature 50px borderless rounded cards (`AdminStatCard`) with multi-tier soft ambient shadows, monumental Bebas Neue figures, and color-coded status pills indicating trend velocity and target fulfillment. | High |
| **REQ-ADM-15** | 100% Real Database Analytics & Dynamic Mathematical Grounding | Every metric, graph point, slice, and progress bar across the Overview dashboard shall derive directly from live domain models and database tables (`trains`, `train_classes`, `bookings`, `booking_passengers`) with zero hardcoded/mock placeholder numbers. This includes DayOfWeek revenue and passenger velocity aggregation, fleet-wide coach capacity and seat occupancy summation, 24-hour departure schedule binning with dynamic peak window detection, and live high-density corridor seat utilization tracking. | Critical |
| **REQ-ADM-16** | Passenger Booking Manifest Inspection & E-Ticket Details View | The Station Master shall be able to inspect detailed passenger booking records directly from the Passenger Booking Manifests table (`AdminDashboardView`). Clicking any booking item or selecting a row and clicking 'VIEW BOOKING DETAILS' shall open the official Electronic Reservation Slip modal dialog (`ETicketPassDialog`), presenting full passenger berth allocations, coach numbers, timetable stepper, fare breakdown receipt, QR barcode, PNR clipboard utility, and ticket cancellation capability with reactive metric refresh. | High |
| **REQ-ADM-17** | System Health & Diagnostics Telemetry Dashboard | The Station Master Administrator Command Center (`AdminDashboardView`) shall feature a dedicated, scrollable System Health & Telemetry workspace (`HEALTH`) wrapped in a borderless `JScrollPane`. The workspace shall display an integrated header with real-time status pill, manual refresh trigger, and live clock; 4 elevated KPI stat cards (Database Engine, JVM Heap Allocated, Active Threads/Concurrency, UI Engine 60 FPS); a primary telemetry row containing a Core Subsystems Status & Integrity Matrix alongside a multi-segment JVM Heap Allocation Visualizer with host platform specs; and a secondary row providing direct operational controls (force JVM garbage collection, test database ping, export telemetry report to clipboard) and a live diagnostic event audit log. | High |
| **REQ-ADM-18** | User Directory & Account Management | The Station Master shall have access to a dedicated `USERS` workspace in the Admin Dashboard sidebar. The workspace shall present a searchable table of all registered passenger and admin accounts showing: numeric ID, full name, username, contact (email or phone), role badge, join date, and a colour-coded status chip (green `ACTIVE` / red `BLOCKED`). The admin shall be able to (a) filter rows in real-time via a live-search pill input, (b) toggle any user's status between `ACTIVE` and `BLOCKED` via a confirmation dialog, and (c) open a drill-down User Detail modal (`showUserDetailDialog`) that displays an identity header, three info cards (Username, Contact, Member Since), and a full booking-history table. Status changes shall be persisted via `UserDAO.updateUserStatus()` in both the database and in-memory cache. All data loads shall execute on a background `SwingWorker` thread to preserve EDT responsiveness. | High |

### 3.2 Non-Functional Requirements

| Requirement ID | Category | Metric / Specification |
|---|---|---|
| **NFR-PERF-01** | Performance | Database search queries shall return results within 250ms for up to 100,000 records. |
| **NFR-UI-01** | Aesthetics & Motion | All animations shall execute at a steady 60 FPS (16ms loop) using volume-conserving squash/stretch spring physics. |
| **NFR-UI-02** | Geometry | All buttons shall be fully rounded pill buttons or have an explicit minimum 24px border radius. |
| **NFR-UI-03** | Minimalism | Zero unnecessary or repetitive explanatory text; UI must be sleek, glassmorphic, and visually consistent across all views. |
| **NFR-REL-01** | Reliability & Safety | UI thread (EDT) shall never be blocked by database I/O or background processing. |
| **NFR-SEC-01** | Security | Passwords shall be hashed before database persistence; all SQL queries shall use `PreparedStatement` to eliminate SQL injection. |
