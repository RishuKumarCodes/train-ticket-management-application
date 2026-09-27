# Rule: MVC Architectural Pattern

This rule outlines the non-negotiable boundaries for Model-View-Controller in the RailFlow application.

## Directory & Package Structure
- `com.trainticket.model`
  - `entity/`: Plain domain entities (`Train`, `Station`, `Coach`, `Seat`, `Booking`, `Passenger`, `User`)
  - `dto/`: Data Transfer Objects for complex UI views (`TrainSearchResultDTO`, `BookingSummaryDTO`)
  - `dao/`: JDBC access objects (`TrainDAO`, `BookingDAO`, `UserDAO`, `ScheduleDAO`)
  - `service/`: High-level business logic, orchestrating transactions and multi-DAO operations (`BookingService`, `TrainSearchService`)
- `com.trainticket.view`
  - `common/`: Reusable animated components (`JellyButton`, `AnimatedCard`, `GlassPanel`, `SpringTextField`)
  - `pages/`: Full screen panels (`SearchTrainsView`, `SeatSelectionView`, `BookingConfirmationView`, `AdminDashboardView`)
  - `dialogs/`: Animated modal overlays (`TicketDetailsDialog`, `SeatFilterModal`)
  - `theme/`: Theme tokens, color palettes, typography, gradients
- `com.trainticket.controller`
  - Controller classes matching major flows (`SearchController`, `SeatSelectionController`, `BookingController`, `AuthController`)
  - Coordinate view events, invoke services on worker threads, update view models.

## Golden Rules
1. Views DO NOT talk to DAOs directly.
2. Models DO NOT import Swing or AWT classes.
3. Controllers DO NOT contain heavy SQL or raw database interactions; delegate to Services/DAOs.
4. Controllers update Views exclusively on the Swing Event Dispatch Thread (`SwingUtilities.invokeLater`).
