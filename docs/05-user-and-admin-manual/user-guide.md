# User & Administrator Manual
## RailFlow — Train Ticket Management Application
**Document Status:** Complete Operational Manual  
**Theme Standard:** Universal Light Theme (`FlatLightLaf`, 50px borderless cards, full pill controls)

---

## 1. Passenger Workflow

### 1.1 Global Header Navigation & Controls
- **3-Tab Navigation Capsule**: Located in the top header (`AppHeaderPanel`), passengers can transition smoothly between:
  - **Book Journey**: Colossal animated hero section, search capsule, and featured destinations.
  - **Trains**: Direct train discovery catalog with pre-filled search bar and live result cards.
  - **My Bookings**: Personal booking history, e-ticket manifests, and cancellation management.
- **Ambient Audio & CoreAudio Routing**: Click the speaker pill in the header to toggle ambient train audio. Right-click or choose from the dropdown to select the active macOS CoreAudio output hardware device.
- **Authentication**: Click the pill button in the top right to open [`AuthDialog`](../../src/main/java/com/trainticket/view/dialog/AuthDialog.java). Sign in or register with email or mobile phone, or continue as guest.

### 1.2 Multi-Criteria Train Search
1. Open the search capsule on either the Home screen or the Trains page.
2. **Origin & Destination**: Use the animated smooth dropdown (`ModernSmoothDropdown`) to search stations by city or 3-4 letter code (e.g. `NDLS`, `MMCT`, `HWH`, `BSB`).
3. **Journey Date**: Open the calendar picker (`ModernDatePicker`) to select your travel date or use rapid shortcut pills (`Today`, `Tomorrow`, `+7 Days`).
4. **Quota & Concession**: Optionally select travel quota (General, Tatkal +30%, Premium Tatkal +50%, All AC) and concession (None, Divyangjan 50% discount, Railway Pass ₹40).
5. Click **Search Trains** (Brand Orange pill CTA).

### 1.3 Train Schedules, Sorting & Timetables
1. On the search results stream:
   - Sort by **Earliest Departure**, **Fastest Duration**, **Latest Arrival**, or **Available Seats**.
   - Review journey metrics: Departure/Arrival times, transit duration, distance in km, and train type badge (Vande Bharat, Rajdhani, Shatabdi, Superfast).
2. Click **View Route & Halts** to open [`TrainRouteTimetableDialog`](../../src/main/java/com/trainticket/view/dialog/TrainRouteTimetableDialog.java) showing station-wise halt durations and live transit tracker.
3. Select desired coach class pill (e.g. `3A`, `2A`, `1A`, `SL`, `CC`, `EC`) showing live seat availability (`AVAILABLE`, `RAC`, `WL`) and calculated fare.

### 1.4 Instant Reservation & PNR Generation
1. Click **Book Now** on the selected train result card.
2. In the [`BookingDialog`](../../src/main/java/com/trainticket/view/dialog/BookingDialog.java) window, fill in passenger details (Full Name, Age, Gender, Berth Preference).
3. Review fare summary and click **Confirm & Issue Ticket**.
4. The system issues a unique 10-digit PNR and automatically records the booking in your account.

### 1.5 Managing Bookings & Ticket Cancellation
1. Navigate to the **My Bookings** tab.
2. If operating as a guest, click **Sign In** to view your history or look up a ticket by PNR.
3. Review your confirmed ticket cards displaying PNR badges, travel stations, scheduled timings, and assigned passenger berth allocations (`B1-12`).
4. Click **Cancel Ticket** to trigger the cancellation confirmation modal and calculate refundable fare.

---

## 2. Station Master Administrator Workflow

### 2.1 Authenticating into Operations Console
1. In the top-right header, click the Auth button and select **Station Master Console** (or open [`AdminLoginDialog`](../../src/main/java/com/trainticket/view/dialog/AdminLoginDialog.java)).
2. Enter the fixed master credentials (`admin` / `admin`).
3. Upon authentication, RailFlow transitions the main application window into the embedded full-page **Station Master Command Center** (`AdminDashboardView`), unloading passenger video and audio assets to conserve memory.

### 2.2 Operational Modules
- **Command Overview**:
  - Top metric cards (`AdminStatCard`): Active Trains, Daily Passengers, On-Time Dispatch Rate, and Gross Revenue.
  - Live Train Rosters table with train numbers, origins, destinations, statuses, and daily schedules.
- **Train Fleet Rosters**:
  - Full train inventory inspection with train types, running days bitmask, and operational status.
- **Stations & Route Halts**:
  - Junction catalogs with station codes, platform counts, railway zones, and halt sequences.
- **Passenger Booking Manifests**:
  - Master reservation registry showing all system PNRs, traveler routes, fares, and active statuses.
- **System Telemetry & Health**:
  - Live diagnostics displaying HikariCP connection pool status, MySQL engine state, Java runtime version, memory allocation, and OS metrics.

### 2.3 Exiting to Passenger View
- Click **← Exit to Passenger View** on the left sidebar (or **SIGN OUT & RETURN** on the top command bar) to terminate the admin session and return to the passenger home page.

