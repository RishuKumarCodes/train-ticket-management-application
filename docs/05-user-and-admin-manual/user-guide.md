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
3. **Journey Date**: Open the calendar picker (`ModernDatePicker`) to select your travel date or use rapid shortcut presets (`Today`, `Tomorrow`, `+7 Days`).
4. **Quota & Concession**: Optionally select travel quota (General, Tatkal +30%, Premium Tatkal +50%, All AC) and concession (None, Divyangjan 50% discount, Railway Pass ₹40).
5. Click **Search Trains** (Brand Orange pill CTA) to navigate directly to the dedicated **Route Search Results Page** (`TrainSearchResultsView`).

### 1.3 Route Search Results vs. Trains Fleet Tab
1. **Route Search Results Page (`TrainSearchResultsView`)**:
   - Displays all matching train runs between the queried origin and destination stations.
   - Includes the pre-filled top `SearchCapsulePanel` allowing instant re-queries or station swapping (`⇄`) on the fly.
   - Sort by **Earliest Departure**, **Fastest Duration**, **Earliest Arrival**, or **Available Seats**.
   - Review journey metrics: Departure/Arrival times, transit duration, distance in km, and train type badge (Vande Bharat, Rajdhani, Shatabdi, Superfast).
   - Click `TIMETABLE & STATUS` to open [`LiveTrainTrackerDialog`](../../src/main/java/com/trainticket/view/dialog/LiveTrainTrackerDialog.java).
   - Select desired coach class pill (e.g. `3A`, `2A`, `1A`, `SL`, `CC`, `EC`) showing live seat availability and calculated fare.
   - Click `← BACK TO HOME` to return to the hero screen anytime.

2. **Trains Tab: Fleet Discovery & Train Number Search (`TrainsPageView`)**:
   - Click the **Trains** tab on the navigation bar to enter the fleet discovery catalog.
   - Search by 5-digit Train Number (e.g. `12952`, `22436`, `12004`) or Train Name (e.g. `Vande Bharat`, `Rajdhani`, `Shatabdi`, `Tejas`).
   - Use quick-filter flagship chips (`12952 Tejas Rajdhani`, `22436 Vande Bharat`, `12004 Shatabdi Express`, etc.) to jump straight to iconic trains.
   - Click `SHOW ALL FLEET` to browse all 100+ active trains in the national railway roster.
   - Inspect train route halts, timings, and live status directly from each card.

### 1.4 Instant Reservation & PNR Generation
1. Click **Book Now** on the selected train result card.
2. In the [`BookingDialog`](../../src/main/java/com/trainticket/view/dialog/BookingDialog.java) window, fill in passenger details (Full Name, Age, Gender, Berth Preference).
3. Review fare summary and click **Confirm & Issue Ticket**.
4. The system issues a unique 10-digit PNR and automatically records the booking in your account.

### 1.5 Checking PNR Status & Managing Bookings
1. Navigate to the **My Bookings** tab on the top navigation bar.
2. **Instant PNR Lookup (Zero Login Required)**:
   - Enter any 10-digit PNR in the **CHECK ANY PNR STATUS** search capsule anchored at the top of the page.
   - Click **CHECK STATUS** (or select a quick sample pill) to open [`PnrStatusDialog`](../../src/main/java/com/trainticket/view/dialog/PnrStatusDialog.java) and inspect live confirmation status, assigned coach/berth, or print an official E-Ticket pass.
3. **Personal Booking Expeditions**:
   - If logged in, all your confirmed ticket cards appear directly below the search bar, with PNR badges, scheduled timings, passenger berth allocations (`B1-12`), and a **SAVED TRAVELERS 👥** management button.
   - If operating as a guest, click **LOG IN / SIGN UP** to sync bookings to your account.
4. **Ticket Cancellation**:
   - Click **Cancel Ticket** on any booking card (or from the PNR Status modal) to trigger immediate cancellation, release seat inventory back to the fleet, and view refund totals.

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
  - **Booking Details Inspection**: Click any booking row in the table (or select a row and click **VIEW BOOKING DETAILS**) to open the official Electronic Reservation Slip (`ETicketPassDialog`). Inspect traveler names, age/gender, assigned coach and berth numbers, timetable stepper, QR barcode, and fare breakdown receipt.
  - **Administrative Cancellation**: Station Masters can process ticket cancellations directly from the reservation slip; doing so automatically replenishes seat quotas and reactively updates dashboard analytics and manifests.
  - **Export Manifest (CSV)**: Export the complete manifest registry to an RFC 4180-compliant CSV spreadsheet.
- **System Telemetry & Health**:
  - Live diagnostics displaying HikariCP connection pool status, MySQL engine state, Java runtime version, memory allocation, and OS metrics.

### 2.3 Exiting to Passenger View
- Click **← Exit to Passenger View** on the left sidebar (or **SIGN OUT & RETURN** on the top command bar) to terminate the admin session and return to the passenger home page.

