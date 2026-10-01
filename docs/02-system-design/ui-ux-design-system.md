# RailFlow Design System: Minimalist Glass & Liquid Motion

This document outlines the official design language, geometry specs, color tokens, and minimalism principles for RailFlow.

---

## 1. Geometry & Corner Radii

RailFlow strictly uses organic, rounded curvature throughout the desktop application:

```
[ Fully Rounded Pill Button: Arc = Height / 2 or arc: 999 ]
(-------------------------------------------------------)

+-------------------------------------------------------+
|  Borderless White / Milk-Glass Card: Arc = 50px       |
|  (arc: 100, strictly 0px border, multi-tier shadow)   |
|                                                       |
|  [ Full Pill Input Field: Arc = 999 ]                 |
|  (__________________________________)                 |
|                                                       |
+-------------------------------------------------------+
```

### Specifications
- **Cards & Content Blocks**: Strictly **50px corner radius** (`arc: 100`) and **0px border** (`borderWidth: 0`). Elevation is achieved exclusively through white/frosted glass surfaces and multi-tiered soft drop shadows.
- **Modals / Popups (`ModernModalDialog`)**: Strictly **50px corner radius** (`arc: 100`), 0px border, native separate window modality, and 60 FPS spring animations.
- **Buttons**: Strictly **full rounded pill buttons** (`arc: 999`, radius = height / 2). Sharp or rectangular buttons are strictly forbidden.
- **Form Inputs & Password Fields**: Strictly **full rounded pill shaped** (`arc: 999`) with `18px` horizontal inset padding, `#F8FAFC` fill, and `#FA5909` focus ring.

---

## 2. Minimalist Visual Discipline

- **No Redundant Explanations**: Modern users don't need "Please enter your station below to find trains". An intuitive input with placeholder `From Station` and a train icon is cleaner and faster.
- **Consistent Visual Hierarchy**:
  - `Display / H1`: 24px - 28px SemiBold.
  - `Section / H2`: 18px - 20px Medium.
  - `Body`: 13px - 14px Regular.
  - `Caption / Badge`: 11px - 12px SemiBold Uppercase.
- **Unifying Pages**: From booking flow to admin dashboards, every page shares the same pill buttons, glass surfaces, and liquid animation transitions.

---

## 3. Color Tokens & Glassmorphism (Universal Light Theme)

All screens, modals, dialogs, and administrative consoles adhere strictly to the **Light Theme & Frosted Milk-Glass System** established by the home page:

| Token Name | Hex / RGBA | Role |
|---|---|---|
| `--bg-base` | `#F8FAFC` | Main canvas / viewport background |
| `--surface-card` | `rgba(255, 255, 255, 0.92)` | Frosted milk-glass cards & modals (alpha 235) |
| `--surface-pure-white` | `#FFFFFF` | Solid white cards, active tabs, and pill buttons |
| `--surface-subtle` | `#F1F5F9` | Tab capsules, guest buttons, and search badges |
| `--border-subtle` | `#E2E8F0` | 1px hairline border stroke |
| `--text-primary` | `#0F172A` | Primary typography (deep obsidian slate) |
| `--text-muted` | `#64748B` | Subdued secondary labels, captions, and placeholders |
| `--accent-primary` | `#FA5909` | Brand Orange actions, active indicators, and focus rings |
| `--accent-hover` | `#E04D05` | Brand Orange hover fill |
| `--accent-pressed` | `#C93F00` | Brand Orange active press fill |
| `--accent-blue` | `#2563EB` | Sky Blue icon badge (`#EFF6FF` background) |
| `--accent-emerald` | `#10B981` | Emerald Green status badge (`#ECFDF5` background) |
| `--accent-amber` | `#EA580C` | Amber destination badge (`#FFF7ED` background) |
| `--accent-purple` | `#9333EA` | Purple date badge (`#FAF5FF` background) |
| `--status-error` | `#EF4444` | Validation error text and indicators |

### 3.1 Visual Cohesion: Passenger Interface & Station Master Console

Every screen and dialog in RailFlow shares the identical clean light aesthetic:

1. **Modal Architecture: `ModernModalDialog` Reusable Base Component**:
   - **True Separate Window**: All application modals extend `ModernModalDialog`, operating as independent native modal windows (`ModalityType.APPLICATION_MODAL`) with a fixed, non-resizable footprint.
   - **Full Application Blocking**: Until the popup window is closed or authenticated, all user interaction with the background application is strictly blocked by the modal loop.
   - **macOS Window Controls (Traffic Lights)**: Integrated window title bar featuring native-grade macOS traffic lights:
     - **Red Close Button** (`#FF5F56`, hover `#E0443E` with `✕` glyph): Triggers fluid exit animation before disposing.
     - **Yellow Minimize Button** (`#FFBD2E`, hover `#DEA123` with `−` glyph): Minimizes application window cleanly.
     - **Green Status Indicator** (`#27C93F`): Active modal indicator.
   - **Draggable Title Bar**: Integrated drag-to-move listener on the window header allowing smooth repositioning anywhere on screen.
   - **Fluid Lifecycle Animations (60 FPS)**:
     - **Entrance**: Organic damped spring ease-out ($k \approx 180, \zeta \approx 0.65$), scaling $0.90 \to 1.00$ with simultaneous alpha fade $0.0 \to 1.0$ over 280ms.
     - **Exit**: Smooth quadratic deceleration curve scaling $1.00 \to 0.92$ with alpha fade $1.0 \to 0.0$ over 180ms prior to window disposal.
     - **Keyboard**: Escape key (`VK_ESCAPE`) triggers `animateClose()`.
   - **Multi-Tier Ambient Shadows**: 24px outer canvas padding ensuring 3-tier Gaussian ambient drop shadows (`new Color(0, 0, 0, 8)`, `new Color(0, 0, 0, 14)`, `new Color(0, 0, 0, 22)`) never clip against the OS window rectangle.
   - **Card Curvature & Borders**: Exactly **50px corner radius** (`arc: 100`, matching Featured Destinations cards) and **strictly 0px border** (`borderWidth: 0`).
   - **Typography**: Display headings rendered in condensed uppercase **Bebas Neue Bold** (`34pt - 36pt`). Zero header clutter (no icons, no small orange eyebrow badges, no subtitle descriptions).
   - **Full Rounded Pill Controls**: All text inputs, password fields, and action buttons are **100% full rounded pill shaped** (`arc: 999`, radius = height / 2) with generous 18px horizontal padding and Brand Orange focus rings (`#FA5909`).

2. **Passenger Authentication Modal (`AuthDialog`)**:
   - Extends `ModernModalDialog` with $520 \times 560\text{px}$ card canvas.
   - Monumental Bebas Neue title (`WELCOME TO RAILFLOW` / `CREATE ACCOUNT`).
   - Pill segmented tab switcher (`#F1F5F9` pill capsule, `arc: 999`).
   - 100% full pill inputs (`arc: 999`, `#F8FAFC` fill, `#E2E8F0` border, `#FA5909` focus ring).
   - Full pill Brand Orange submit button with squash/stretch liquid animation and "Continue as Guest" pill button.
   - Shortcut footer link to Station Master Operations Console.

3. **Full-Page Station Master Command Center (`AdminDashboardView` & `AdminLoginDialog`)**:
   - **Admin Login Modal (`AdminLoginDialog`)**: Extends `ModernModalDialog` with $480 \times 460\text{px}$ card canvas, monumental Bebas Neue title `STATION MASTER CONSOLE` (zero icons/eyebrows), full pill Operator ID and Master Access Key inputs (`arc: 999`), Sky Blue pill authorize button (`#0284C7`), and cancel button. Upon successful verification, dispatches directly into `MainFrame`'s full-page view rather than spawning an auxiliary popup window.
   - **Full-Page Embedded Admin Dashboard (`AdminDashboardView`)**:
     - Embedded seamlessly inside `MainFrame` using `CardLayout` (`"ADMIN"` vs `"PASSENGER"`), releasing video/audio thread resources during administration sessions.
     - **Top Command Bar**: Solid white bar with active operator identity pill (`#F0F9FF` / `#0284C7`), live real-time digital clock (`HH:mm:ss`), database health badge (`#ECFDF5` / `#10B981`), and a full rounded pill `"SIGN OUT & RETURN"` button (`#FEF2F2` / `#DC2626`).
     - **Left Operational Sidebar**: Pure white `#FFFFFF` bar with `#E2E8F0` right divider, uppercase Bebas Neue `STATION MASTER` header, and 5 navigation toggle pill buttons:
       - `Command Overview`
       - `Train Fleet Rosters`
       - `Stations & Route Halts`
       - `Booking Manifests`
       - `System Health & DB`
     - **Metric Cards & Data Tables**: 4 key performance cards (Active Trains, Stations Linked, Confirmed Bookings, Fleet On-Time Rate) with 50px corner radius, `#FFFFFF` fill, 0px border, multi-tier soft shadows, Bebas Neue metric headers, and high-contrast numbers; alongside live interactive tables for trains, stations, and passenger booking manifests.

4. **Dedicated Trains Search & Discovery Page (`TrainsPageView`)**:
   - **Architecture**: A dedicated full-page screen embedded in `MainFrame` (`"TRAINS"` card), invoked directly from the navigation bar or upon submitting a search query from `HomeView`.
   - **Interactive Pre-Filled Search Header**: Features `SearchCapsulePanel` anchored at the top with pre-filled search parameters (Origin, Destination, Travel Date, Quota, Concession), allowing passengers to refine parameters on the fly without returning to the home screen.
   - **Canvas & Theme**: Light Sky-Slate base (`#F8FAFC`) with smooth custom overlay scrollbar.
   - **Monumental Title**: Uppercase **Bebas Neue Bold** (`38pt`) `AVAILABLE TRAINS` with zero header clutter (no icons, no eyebrows, no descriptions).
   - **Query Meta Chips**: Rounded pill badges (`arc: 999`) for Route (`#EFF6FF` / `#1D4ED8`), Date (`#F1F5F9`), Quota (`#FFF7ED` / `#C2410C`), Concession (`#ECFDF5` / `#047857`), and Scheduled Count (`#FA5909`).
   - **Sorting Filters Bar**: Full rounded pill toggle buttons (`arc: 999`) for `DEPARTURE (EARLIEST)`, `DURATION (FASTEST)`, `ARRIVAL (EARLIEST)`, and `SEATS AVAILABLE`.
   - **Train Result Cards (`TrainResultCard`)**:
     - Strict **50px corner radius** (`arc: 100`, matching Featured Destinations cards) and **strictly 0px border** (`borderWidth: 0`).
     - Multi-tier Gaussian ambient drop shadows (`new Color(0, 0, 0, 10)` and `new Color(0, 0, 0, 14)`).
     - Card Header: Train number in Slate (`#64748B`), Train Name in Bebas Neue Bold (`26pt`, `#0F172A`), Type badge, live running status pill (`● ON TIME`), and active running days pill.
     - Journey Stepper: Departure time (`22pt` bold), Origin station code & name, center dashed travel line with duration (`15h 40m`) and distance (`1,384 km`), Arrival time with day offset badge (`+1 day`), and Destination station.
     - Coach Availability Pills (`CoachClassPillButton`): Interactive $126 \times 76\text{px}$ cards with `arc: 28`, dynamic fare, real-time availability status badges (Green `AVAILABLE`, Amber `RAC`, Coral `WL`), and Brand Orange selection ring.
     - Footer Actions: Full pill buttons for "VIEW ROUTE & TIMETABLE ↗" and "BOOK JOURNEY" (`#FA5909`).

5. **My Bookings Passenger History Page (`MyBookingsPageView`)**:
   - **Architecture**: Embedded full-page view inside `MainFrame` (`"MY_BOOKINGS"` card) accessible directly via the top navigation capsule.
   - **Unauthenticated / Guest State**:
     - Displays a centered, monumental 50px rounded frosted glass card (`#FFFFFF`, 0px border, multi-tier soft shadows).
     - Clean uppercase Bebas Neue headline: `VIEW YOUR TRAIN BOOKINGS` (zero icons, zero subtitles).
     - Full rounded pill CTA button `"LOG IN / SIGN UP"` (`#FA5909`, `arc: 999`) that opens `AuthDialog` with spring animation and seamlessly refreshes the booking list upon login.
   - **Authenticated Passenger State**:
     - Displays the passenger's registered identity badge (`#EFF6FF` pill).
     - Stream of confirmed reservation cards (`50px` rounded corners, `#FFFFFF`, 0px border, multi-tier soft ambient shadows):
       - Header: PNR badge (`#FA5909`), status pill (`● CONFIRMED`), and train number/name.
       - Timings & Journey details: Departure and arrival times, travel date, class code, and total payable fare.
       - Passenger Manifest: List of travelers, assigned coach/seats (e.g. `B4-23 LOWER`), age, and gender.
       - Actions: Full pill buttons for `"VIEW E-TICKET PASS"` and `"CANCEL BOOKING"` with confirmation prompt and live status updates.

5. **Station-Wise Route Timetable & Live Tracker Modal (`TrainRouteTimetableDialog`)**:
   - Extends `ModernModalDialog` with $780 \times 620\text{px}$ window canvas.
   - Monumental Bebas Neue title `STATION ROUTE & LIVE TIMETABLE`.
   - Journey Summary Capsule in `#F8FAFC` displaying train number, commercial name, route terminals, duration, and distance.
   - Center Timetable: Vertical rail track stepper showing halt circles (`● Passed`, `● Current Location`, `○ Scheduled`) paired with a custom styled `JTable` rendering stop sequence, station code/name, arrival, departure, halt duration, and cumulative rail km.

6. **Instant Passenger Expedition Booking Modal (`BookingDialog`)**:
   - Extends `ModernModalDialog` with $660 \times 620\text{px}$ window canvas.
   - Monumental Bebas Neue title `CONFIRM EXPEDITION BOOKING`.
   - Trip Summary Capsule: Train name/number, scheduled timings, selected coach class, and quota.
   - Form Fields: 100% full rounded pill inputs (`arc: 999`, height 42px) for Traveler Full Name (pre-populated from `AuthSession`), Age & Gender, and Contact Delivery Mobile/Email.
   - Fare Breakdown & Confirm CTA: Large total payable fare display and Brand Orange pill button `"CONFIRM & ISSUE TICKET"`.
   - Success View: Generates unique 10-digit PNR (`e.g., 284-9382194`), coach berth allocation, payment confirmation, and return button.

7. **Custom Modern Smooth Animated Dropdown & Calendar Date Selector Components**:
   - **`ModernSmoothDropdown<T>`**:
     - Eliminates rigid Swing `JComboBox` popups in favor of an undecorated transparent `JWindow` (`setBackground(new Color(0, 0, 0, 0))`).
     - **Geometry & Elevation**: Floating white card with 24px corner radius (`arc: 48`), 0px border, and multi-tier ambient drop shadow (`rgba(0, 0, 0, 0.08)` and `rgba(0, 0, 0, 0.12)`).
     - **Micro-Interactions**: Rotating vector chevron arrow ($0^\circ \to 180^\circ$ interpolation) synchronized with popup state.
     - **Spring Animation**: 60 FPS cubic ease-out drop-down motion gliding $12\text{px}$ vertically while fading in.
     - **List Rows**: Generous row padding, subtitle descriptions, selected item checkmark (`✓`), and soft pill hover highlights (`#F1F5F9`).
     - **Event Handling**: Global `AWTEventListener` outside-click dismissal, Escape key handling, and clean garbage collection.
   - **`ModernDatePicker`**:
     - Eliminates plain string text inputs for journey dates with an organic, interactive calendar popup.
     - **Header**: Monumental Bebas Neue Bold Month/Year title (e.g. `OCTOBER 2026`) with circular month stepper buttons (`‹` and `›`).
     - **Quick Shortcut Presets**: Horizontal pill buttons for `Today`, `Tomorrow`, and `+7 Days` for one-click scheduling.
     - **Weekday & Days Grid**: 7-column calendar matrix with uppercase weekday headers (`SU`, `MO`, `TU`, `WE`, `TH`, `FR`, `SA`).
     - **Date States**:
       - Past dates disabled and dimmed (`#CBD5E1`).
       - Current day highlighted with an orange outline ring (`#FA5909`).
       - Active selected date styled with a vibrant Brand Orange solid circle (`#FA5909`), white bold text, and subtle ambient glow.
       - Future days feature soft circular hover highlights (`#F1F5F9`).
     - **Animation & Dismissal**: 60 FPS smooth drop-in motion and automatic outside-click dismissal.

---

## 4. macOS Dock & App Icon Geometry (Apple HIG Compliance)

To match native macOS applications (QuickTime, Terminal, Finder), the RailFlow application icon strictly complies with Apple Human Interface Guidelines:

```
+-------------------------------------------------------+
| 512 x 512 Canvas (Transparent Background)            |
|                                                       |
|   50px Top Margin                                     |
|   +-----------------------------------------------+   |
|   | 412 x 412 Apple Superellipse Squircle Tile    |   |
|   | Curvature: Superellipse (|x/a|^4.4 + |y/b|^4.4) |   |
|   | Continuous Tangent Curvature (~93px radius)   |   |
|   | Depth Gradient (#FF6912 -> #ED4C00)           |   |
|   | Centered White Train Glyph                    |   |
|   +-----------------------------------------------+   |
|   | Ambient Drop Shadow (6px Y-offset, 22% alpha) |   |
|                                                       |
+-------------------------------------------------------+
```

- **Canvas Size**: $512 \times 512\text{px}$ RGBA with transparency.
- **Inner Icon Tile**: $412 \times 412\text{px}$ (80.5% grid ratio matching standard macOS app icon sizing), leaving uniform $50\text{px}$ margins on all sides.
- **Continuous Curvature**: Formula $(|x/a|^{4.4} + |y/b|^{4.4} \le 1)$ ensuring seamless $G_2$ curvature continuity into straight edges without the abrupt circular cuts of basic rounded rectangles.
- **Drop Shadow**: Two-tier soft Gaussian drop shadow (`rgba(0,0,0,0.22)` at 6px offset) to blend naturally against macOS wallpapers in the Dock.

---

## 5. Header Branding & Floating Frosted Glass Navigation Capsule

The top navigation bar adopts a modern floating frosted glass aesthetic with full-window video extension:

- **Full-Window Video Canvas**:
  - The live video background (`VideoBackgroundPanel` / `hero.mp4`) and top ambient scrim stretch across the **entire window up to the very top edge (`y = 0`)** behind the OS title bar area.
  - **Zero Title Bar Clutter**: The redundant window title text (`"RailFlow — Train Ticket Management"`) is completely suppressed (`apple.awt.windowTitleVisible = false`, `FlatClientProperties.TITLE_BAR_SHOW_TITLE = false`, and `setTitle("")`), keeping the space beside the window control buttons clean, minimal, and unobtrusive.
  - **macOS Integration**: Uses native AppKit full-window content (`apple.awt.fullWindowContent` and `apple.awt.transparentTitleBar`), allowing native macOS traffic lights (🔴 🟡 🟢) to float directly over the live mountain video.
  - **Windows Integration**: Uses FlatLaf custom decorations (`FlatClientProperties.USE_WINDOW_DECORATIONS` and `FULL_WINDOW_CONTENT`) with a 100% transparent title pane so Windows caption controls float over the video in the top-right corner.
  - **Window Dragging**: The header panel is registered as a native title bar caption (`FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION`), enabling effortless window movement by dragging across the top bar.
- **Header Geometry & Layout Offset**:
  - The header container (`header`) has top padding set to `36px` (`EmptyBorder(36, 48, 16, 48)`), ensuring that all UI elements (brand logo, music button, navigation capsule, and action buttons) sit comfortably below the OS title bar area without colliding with traffic lights or caption buttons.
- **Brand Logo & Audio Control (West)**:
  - **Brand Logo**: High-resolution wide brand mark (`icon-wide-white.png`, 180x36px).
  - **Ambient Music Button (`musicBtn`)**: Positioned directly next to the brand logo inside `brandPanel`. Styled as a circular pill button (diameter `42px`, `arc: 999`) with state-dependent appearance:
    - **When Music is OFF**: Solid pure white circular background (`#FFFFFF`) with dark obsidian slashed musical note (`music-cut.svg`, `#0F172A`).
    - **When Music is ON**: Translucent dark smoked glass background (`rgba(15, 23, 42, 0.24)` / 60 alpha) with real-time GPU live video backdrop blur (matching `navCapsule`), displaying the active musical note (`music.svg`) in pure white (`#FFFFFF`).
- **Floating Smoked Glass Capsule (Center)**:
  - Unified horizontal capsule panel with full pill geometry (`arc: 999`, height 42px).
  - Translucent smoked glass fill (`rgba(15, 23, 42, 0.24)` / 60 alpha) with soft ambient drop shadow (`rgba(0, 0, 0, 0.18)`), completely borderless (no outer rim stroke).
  - **Height & Vertical Alignment**: Capsule height is locked to exactly `42px` (matching `planTripBtn`, `loginBtn`, and `musicBtn`).
  - **Passenger 3-Tab Architecture**: Hosts exactly 3 dedicated navigation tabs:
    1. **Tab 0: "Book Journey"**: Switches directly to the scenic Alpine Home experience (`HomeView`).
    2. **Tab 1: "Trains"**: Switches directly to the dedicated train search and live results page (`TrainsPageView`).
    3. **Tab 2: "My Bookings"**: Switches directly to the user reservation history (`MyBookingsPageView`).
  - **Dynamic Pill Styling**: The active tab is rendered with a solid pure white pill button (`#FFFFFF`, height 42px, `arc: 999`) with dark slate typography (`#0F172A`, Roboto Bold 13px). Inactive tabs are rendered with translucent interactive glass styling with soft hover highlights (`rgba(255, 255, 255, 0.14)`).
  - Fits completely flush against the outer capsule with zero top/bottom spacing, seamlessly coinciding with the capsule's rounded arcs.
- **Header Actions (East)**:
  - **"Plan My Trip ↗" Action Button**: Pure white floating pill button (`#FFFFFF`, height 42px, `arc: 999`, width 160px) positioned directly to the left of the Login button, containing bold dark text (`#0F172A`, Roboto Bold 13px) and an embedded circular brand-orange badge (diameter 30px, `#FA5909`) with a crisp white diagonal arrow `↗` (`\u2197`).
  - **"Login" / User Account Button**: Pure white floating pill button (`#FFFFFF`, height 42px, `arc: 999`, min-width 72px) with dark slate typography (`#0F172A`, Roboto Bold 13px, padding 6px 14px). When authenticated, displays the passenger's first name with a sign-out click action.

---

## 6. Scenic Alpine Journey Backdrop, Hero Typography & Floating Ticket Booking Capsule

RailFlow delivers an awe-inspiring scenic travel visual language combining photographic alpine splendor with a streamlined floating search capsule preserving all original booking parameters:

- **Cinematic Video Backdrop**:
  - `src/main/resources/assets/videos/hero.mp4`: JavaFX `MediaPlayer` embedded inside Swing via `JFXPanel` under `JLayeredPane.DEFAULT_LAYER` with responsive cover scaling, transparent background rendering, and solid neutral obsidian canvas fallback (`#0F172A`).
- **Centered Hero Display Typography & Kinetic Reveal Carousel**:
  - **Eyebrow**: Seamless text phrase `"D I S C O V E R   Y O U R   N E X T"` with continuous tracking where `"D I S C O V E R   Y O U R   "` is rendered in crisp flat pure white (`#FFFFFF`) and `"N E X T"` is rendered in primary brand orange (`#FA5909`) with a square white background text highlight (`Color.WHITE`, 3px horizontal padding on either side, bounds matching font ascent/descent and text width $+ 6\text{px}$), both rendered in Roboto Bold dynamically scaled $advFontSize \times 0.080$ (13–20px) sharing an identical optical baseline without shadows.
  - **Staggered Invisible-Box Kinetic Headline**: Continuously cycles through four travel words:
    $$\text{"ADVENTURE"} \longrightarrow \text{"EXPERIENCE"} \longrightarrow \text{"JOURNEY"} \longrightarrow \text{"MEMORY"}$$
  - **Invisible Box Clipping Mask (Zero Fade / 100% Solid Opacity)**: Characters remain 100% solid pure white (`#FFFFFF`) at all times without opacity fading. Their visibility is governed purely by the mechanical boundary of the vertical clipping mask (`g2.clipRect(0, eyeY + eyeDescent + 1, w, wordAscent + wordDescent + 14)`), emerging cleanly from behind the bottom boundary and slicing off sharply above the top boundary below the eyebrow text highlight.
  - **Sextic Ease-Out Deceleration ($E(p) = 1 - (1 - p)^6$) & Extended Duration ($2180\text{ms}$)**: Characters launch rapidly and dedicate prolonged duration to an ultra-slow, feather-soft liquid landing tail ("starts very quickly and ends very slowly").
  - **Immediate Staggered Handoff Overlap**: Incoming characters do not wait for the entire word to exit; with a reduced $140\text{ms}$ delay, character 0 of the incoming word begins rising from beneath the bottom frame almost immediately as outgoing character 0 lifts, followed sequentially by each subsequent character with a $38\text{ms}$ stagger.
  - **Display Sizing**: Base display scale anchored to 68% viewport width for `"ADVENTURE"` (`Bebas Neue` Regular condensed display typeface, pure white `#FFFFFF`, zero drop shadows).
  - **Dead-Center Positioning**: The headline and eyebrow are stacked and mathematically centered both vertically and horizontally in the hero viewport space above the bottom search capsule (`(w - strWidth) / 2`, `(h - totalHeight) / 2`).
- **Global Typography Contract**:
  - `Bebas Neue` (`src/main/resources/assets/fonts/BebasNeue-Regular.ttf`): Dedicated condensed display typeface exclusively reserved for monumental display titles.
  - `Roboto` (`src/main/resources/assets/fonts/Roboto-*.ttf`): Global geometric sans-serif typeface configured as FlatLaf's default font (`UIManager.put("defaultFont", ...)`) across all buttons, inputs, labels, tables, dropdowns, and dialogs.
- **Floating Horizontal White Search Capsule Bar**:
  - A single continuous pill capsule (`arc: 999`, height 78px, preferred width 1120px) floating above the bottom viewport with multi-tier ambient drop shadow and seamless borderless frosted glass styling.
  - Divided into 5 distinct horizontal sections separated by subtle vertical dividers:
    1. **From Station Field (`fromField`)**: 38x38px pastel sky-blue circular avatar (`#EFF6FF`) with blue vector location pin (`#2563EB`), title `"From"`, and borderless text field with placeholder `"Station or City"` (default: `"New Delhi (NDLS)"`).
    2. **To Station Field (`toField`)**: 38x38px pastel amber circular avatar (`#FFF7ED`) with orange vector destination pin (`#EA580C`), title `"To"`, and borderless text field with placeholder `"Station or City"` (default: `"Mumbai Central (MMCT)"`).
    3. **Journey Date Field (`dateField`)**: 38x38px pastel purple circular avatar (`#FAF5FF`) with purple vector calendar icon (`#9333EA`), title `"Date"`, and pre-filled date field (`YYYY-MM-DD`).
    4. **Class Dropdown (`classDropdown`)**: 38x38px pastel emerald circular avatar (`#ECFDF5`) with emerald green ticket icon (`#10B981`), title `"Class"`, and borderless `JComboBox` containing `{"All Classes", "1A - AC First Class", "2A - AC 2 Tier", "3A - AC 3 Tier", "SL - Sleeper", "CC - Chair Car"}`.
    5. **Search Trains Action Button (`searchButton`)**: Full rounded pill button (`arc: 999`, height 50px) in vibrant brand orange (`#FA5909`, hover `#E04D05`, pressed `#C93F00`) displaying white bold text `"Search Trains"` and an embedded white circular badge (diameter 38px) with brand-orange search magnifying glass icon `🔍`.

---

## 7. Ambient Journey Audio (`hero.wav`) & Dynamic OS Output Routing

RailFlow features an optional immersive ambient audio experience that complements the background video with full macOS audio hardware device routing:

- **Audio Asset**: `src/main/resources/assets/audio/hero.wav` (44.1 kHz, 16-bit stereo PCM loop).
- **Interactive Control**: A circular pill button (`42x42px`, `arc: 999`) located in the header bar next to the brand logo.
- **Dynamic Dual-State Visual Presentation**:
  - **Muted State (OFF)**: Pure solid white circular background (`#FFFFFF`) with dark obsidian slashed musical note (`music-cut.svg`, `#0F172A`) and tooltip `"Play ambient journey sound • Right-click to switch device"`.
  - **Playing State (ON)**: Translucent smoked glass background (`rgba(15, 23, 42, 0.24)`) with live hardware GPU backdrop blur (matching `navCapsule`), displaying the active musical note (`music.svg`) in pure white (`#FFFFFF`) and dynamic tooltip `"Mute ambient sound ([Active Device]) • Right-click to switch device"`.
- **Dynamic OS Output Routing Engine**:
  - Managed by `AudioManager` utilizing Java Sound (`javax.sound.sampled`).
  - Native CoreAudio device interrogation dynamically queries `kAudioHardwarePropertyDefaultOutputDevice` on macOS to bind to the exact active system output device (e.g. `External Headphones`, `MacBook Air Speakers`, `HA220Q` external monitor).
  - **Auto-Migration Watcher**: While playing, a daemon watcher detects when the user changes their sound output in macOS System Settings or Control Center, seamlessly migrating the live audio stream to the newly selected device without interrupting playback or restarting the track.
- **Hardware Output Selector Context Menu**: Right-clicking the music button opens a sleek `JPopupMenu` listing all available system audio devices (`Auto (System Default)`, `MacBook Air Speakers`, `External Headphones`, etc.) with active radio selection.
- **Window Lifecycle Integration**: Automatically pauses audio when the window is minimized (`windowIconified`), resumes maintaining position when un-minimized (`windowDeiconified`), and gracefully closes mixer lines on application exit (`windowClosing`).

---

## 8. Real-Time Hardware GPU Video Blur & Glassmorphism Engine

RailFlow implements a dual-tier real-time frosted glass system that blurs the **actual live background video (`hero.mp4`) in real-time** via GPU shader hardware, with an automatic fallback to CPU mipmap convolution if the video is paused or inactive:

```
+-----------------------------------------------------------------------------------------+
| Live Video Real-Time GPU Backdrop Blur Pipeline                                          |
|                                                                                         |
|                  +-----------------------------> [Layer 0: Crisp Video MediaView]       |
|                  |                               (Full-screen scenic mountain playback) |
| [MediaPlayer] ---+                                                                      |
|  (hero.mp4)      |                                                                      |
|                  +-[GPU GaussianBlur(38)]------> [Layer 1: Live Blurred MediaView]      |
|                                                  (Hardware shader pass on GPU)          |
|                                                                 |                       |
|                                                                 v                       |
|                                                    [Dynamic Scissor/Clip Bounds]        |
|                                                    (searchCapsule / navCapsule pill)    |
|                                                                 |                       |
|                                                                 v                       |
|                                                  [Swing UI Frosted Glass Overlay]       |
|                                                  (Translucent wash + hairline border)   |
+-----------------------------------------------------------------------------------------+
```

### 8.1 High-Performance Video & Native Glassmorphism Architecture (`VideoBackgroundPanel`)
- **Single Master `MediaView` Pipeline**:
  - A single dedicated `MediaPlayer` and `MediaView` instance drives the background display with JavaFX hardware node caching (`setCache(true)`, `CacheHint.SPEED`).
  - Eliminates secondary blurred `MediaView` nodes to prevent redundant multi-pass Gaussian blur shaders on 1080p frames, keeping Apple Silicon (M1/M2/M3) whisper-quiet and cool.
- **Smart Occlusion Culling**:
  - An intelligent viewport listener detects when the user scrolls down into the "Featured Destinations" section (`scrollY > heroH * 0.70`) and automatically pauses the background video (`pauseVideo()`).
  - Video playback instantly resumes when scrolled back up (`resumeVideo()`), achieving literal 0.0% CPU and 0.0% GPU video decode overhead while browsing destination cards.
- **Native Java2D Glassmorphism**:
  - Cards (`searchCapsule`, `navCapsule`, `musicBtn`) render rich frosted milk glass styling directly in Java2D with multi-tier ambient drop shadows, semi-translucent fills (`rgba(255, 255, 255, 0.92)`), and hairline specular borders without cross-thread JavaFX coordination.

### 8.2 Base Obsidian Surface Rendering
- In `VideoBackgroundPanel.paintComponent`, the underlying canvas renders a neutral solid obsidian background (`#0F172A` / `new Color(15, 23, 42)`).
- During window resizing or media buffer adjustments, the UI maintains seamless dark continuity with zero image flickering or static artifact exposure.

### 8.3 Active Application in RailFlow
1. **Floating Search Capsule Bar (`createSearchCapsule`)**:
   - **Dimensions & Geometry**: Pill (`arc: 999`, height 80px, preferred size `1160 x 80px`, minimum size `960 x 80px`, insets `EmptyBorder(0, 16, 0, 0)`).
   - **Glass Tint**: Translucent frosted white wash (`rgba(255, 255, 255, 0.84)` / alpha 215) with multi-tiered ambient drop shadow (`rgba(0, 0, 0, 0.08)` and `rgba(0, 0, 0, 0.12)`).
   - **Column Weight Distribution**:
     - `From Station`: `weightx = 0.27`
     - `To Station`: `weightx = 0.28` (expanded width prevents station names like "Mumbai Central (MMCT)" from truncating)
     - `Journey Date`: `weightx = 0.17`
     - `Class Dropdown`: `weightx = 0.16`
     - `Search Action`: `weightx = 0.12` (hosts 130x60px solid brand orange pill CTA button with centered 17px Roboto Bold white "Search" text, no icon)
   - **Concentric Margins & Segment Alignment**:
     - All input segments use `BorderLayout(12, 0)` with uniform `EmptyBorder(10, 12, 10, 8)`:
       - `BorderLayout.WEST`: 42x42px pastel circular badge avatar (From: Blue `#EFF6FF` / Pin `#2563EB`; To: Amber `#FFF7ED` / Pin `#EA580C`; Date: Purple `#FAF5FF` / Calendar `#9333EA`; Class: Emerald `#ECFDF5` / Ticket glyph `#10B981`).
       - `BorderLayout.CENTER`: Vertical stack with vertical glue containing:
         - Header Label: 11px Roboto Bold in muted slate (`#64748B`), uppercase (`FROM`, `TO`, `JOURNEY DATE`, `CLASS`).
         - Value / Input: 15px Roboto Bold in obsidian (`#0F172A`), caret anchored at position 0 to guarantee leading text visibility, zero border, transparent background.
         - Class Dropdown: 14px Roboto Bold in obsidian (`#0F172A`) with custom `DefaultListCellRenderer`.
     - **Concentric Search Button Geometry**: The Search action segment uses `FlowLayout(RIGHT, 10, 10)`. With capsule height $H = 80\text{px}$ ($R_{capsule} = 40\text{px}$) and button height $h = 60\text{px}$ ($R_{button} = 30\text{px}$), the radial and linear clearance is identically $10\text{px}$ across top, bottom, right, and along the entire radial arc ($R - r = 10\text{px}$).
   - **Dynamic Result**: Scenic live video motion glides smoothly through the translucent pill, while inputs display clear typography hierarchy and zero text clipping.
2. **Top Navigation Capsule (`navCapsule`)**:
   - **Dimensions & Geometry**: Floating Pill (`arc: 999`, height 42px, width constrained via `getPreferredSize()` and `getMaximumSize()` to only the required button width $\sim360\text{px}$, padding `EmptyBorder(0, 4, 0, 4)`) centered horizontally within a `FlowLayout(CENTER)` wrapper.
   - **Glass Tint**: Dark smoked glass fill (`rgba(15, 23, 42, 0.63)` / `new Color(15, 23, 42, 160)`) with multi-tiered deep ambient shadow (`rgba(0, 0, 0, 0.25)` and `rgba(0, 0, 0, 0.40)`) and delicate 1px hairline rim (`rgba(255, 255, 255, 0.12)`).
   - **Typography**: 13px Roboto Bold (`#0F172A` on solid white active pill, `#FFFFFF` on translucent pills).
   - **Dynamic Result**: Alpine sky and train motion drift smoothly through behind the dark smoked navigation pill with rich floating contrast and clean breathing room to the right-hand action controls.

---

## 9. Scrollable Landing Experience & Featured Destinations Showcase

RailFlow features a vertical continuous-scroll architecture that transitions from the cinematic hero landing down into a curated editorial destinations directory:

```
+-----------------------------------------------------------------------------------------+
| [JScrollPane] - Transparent Viewport, Mac-Style Overlay Scrollbar (8px)                |
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   | 1. Hero Section (Adaptive Viewport Height ~680px, Fully Transparent)            |   |
|   |                                                                                 |   |
|   |       [Dead-Center in Viewport: Vertically & Horizontally Centered]             |   |
|   |       - Eyebrow: "D I S C O V E R   Y O U R   " + "N E X T" (Flush White Highlight)     |   |
|   |       - Kinetic Carousel: ADVENTURE ➔ EXPERIENCE ➔ JOURNEY ➔ MEMORY             |   |
|   |         (Invisible Clipping Box, Quintic Ease-Out, 35ms Stagger, 68% Width)     |   |
|   |                                                                                 |   |
|   |       [Pinned to Viewport Bottom with 32px Margin Below]                        |   |
|   |       - Floating White Search Capsule Bar (1160x80px with real-time GPU blur)  |   |
|   +---------------------------------------------------------------------------------+   |
|                                         |                                               |
|                                         | (Continuous Scroll / Mouse Wheel / Trackpad)  |
|                                         v                                               |
|   +---------------------------------------------------------------------------------+   |
|   | 2. Featured Destinations Sheet (Solid White Background, Top Arc: 70px radius)   |   |
|   |                                                                                 |   |
|   |    +-----------------------------------------------------------------------+    |   |
|   |    | FeaturedDestinationsHeader (Height: 200px, Generous Vertical Margins):|    |   |
|   |    |   Watermark: "DESTINATION" (145-215px Bebas Neue, #F1F5F9)            |    |   |
|   |    |   Title:     "Featured Destinations" (34-46px Roboto Bold, #0F172A)    |    |   |
|   |    +-----------------------------------------------------------------------+    |   |
|   |                                                                                 |   |
|   |    - Editorial Subtitle: Scenic mountain passes, royal heritage & express lines |   |
|   |    - Filter Pills: [🌟 All Routes] [⚡ High Speed] [🏔 Scenic] [❄️ Snow]...     |   |
|   |                                                                                 |   |
|   |    - 2-Column Destination Cards Grid (24px Arc, Ambient Drop Shadows):          |   |
|   |        [Card 1: Vande Bharat Express (NDLS ➔ BSB) | From ₹1,750 | Book ↗]      |   |
|   |        [Card 2: Himalayan Queen (KLK ➔ SML)       | From ₹850   | Book ↗]      |   |
|   |        [Card 3: Konkan Kanya (CSMT ➔ MAO)         | From ₹1,240 | Book ↗]      |   |
|   |        [Card 4: Kashmir Valley Express (BAHL ➔ BRML) | From ₹450 | Book ↗]    |   |
|   |        [Card 5: Palace on Wheels (NDLS ➔ JP)      | From ₹3,200 | Book ↗]      |   |
|   |        [Card 6: Glacier Express (ZMT ➔ STM)       | From ₹14,800| Book ↗]      |   |
|   +---------------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------------+
```

### 9.1 Overlay Scroll Architecture (`scrollPane`)
- **Transparent Viewport**: Both `scrollPane` and its `JViewport` are configured with `setOpaque(false)`, allowing the scenic live video and ambient canvas lighting to illuminate the hero section.
- **Mac-Style Overlay Scrollbar**:
  - Width: `8px` thin profile, positioned along the right edge.
  - Zero arrow buttons (`createDecreaseButton` / `createIncreaseButton` return zero-size components).
  - Track: Fully transparent.
  - Thumb: Soft rounded pill (`arc: 6px`) in `rgba(148, 163, 184, 0.50)` (`#94A3B8`), darkening to `rgba(100, 116, 139, 0.70)` on hover.
  - Wheel Dynamics: `unitIncrement = 24` for rapid, fluid navigation.
- **Smart Occlusion Culling & Thread Decoupling**:
  - The scroll viewport is completely decoupled from JavaFX video rendering and repaint storms.
  - A lightweight, non-blocking `ChangeListener` on the scroll viewport monitors vertical offset. When the destination sheet covers more than 70% of the hero section (`scrollY > heroH * 0.70`), it triggers `VideoBackgroundPanel.getInstance().pauseVideo()` and sleeps text/particle animations.
  - When scrolling back into the hero section, video playback instantly resumes (`resumeVideo()`). This drops video decoding and CPU usage to virtually 0% while browsing cards, keeping M2 MacBooks cool and trackpad scrolling at native 60/120Hz.
- **Cubic Ease-Out Smooth Scroll (`smoothScrollTo`)**:
  - Driven by a 60 FPS `javax.swing.Timer` (16ms loop) executing a cubic deceleration curve:
    $$\text{ease}(t) = 1.0 - (1.0 - t)^3, \quad t = \frac{\Delta t}{380\text{ms}}$$
  - Used by the destination card `"Book Route ↗"` action to glide back to the search bar.

### 9.2 Watermark Header Architecture (`FeaturedDestinationsHeader`)
Replicates the exact typographic aesthetic of the featured destinations design specification with enhanced vertical breathing room and responsive scaling:
- **Generous Vertical Margins**: Section container top margin enhanced to `EmptyBorder(116, 60, 84, 60)` with `230px` header height and a spacious `60px` vertical gap to the cards gallery for an airy, luxurious layout.
- **Corner Radius**: Solid white sheet configured with a sweeping **`70px`** top corner radius (`cornerArc = 140`), creating an ultra-smooth, luxury pill-soft transition from the hero video backdrop.
- **Responsive Dynamic Scaling & Exact Glyph Centering**:
  - In larger widths ($w \ge 1200\text{px}$), the foreground title dynamically scales from `34pt` up to **`46pt`** bold (`#0F172A`).
  - The background condensed watermark `DESTINATION` (`Bebas Neue`) scales proportionally from `145pt` up to **`215pt`** with a refined, subtly darkened sky slate tone (`#E4EAF1` / `rgb(228, 234, 241)`).
  - Centered using `GlyphVector.getVisualBounds()` so the foreground text sits with mathematical precision at the exact horizontal and vertical midpoint of the watermark glyphs.

### 9.3 Destination Visual Gallery (3-in-a-Row Image Cards)
- **Grid Layout**: 3 columns (`GridLayout(0, 3, 24, 24)`), max container width `1200px` centered horizontally.
- **Card Geometry**:
  - `360 x 480px` (3:4 portrait aspect ratio matching native 1086x1448 image assets).
  - Rounded corners: **`50px`** border radius (`cornerRadius = 50, arc = 100`, clipped via `RoundRectangle2D.Float`).
  - Center-crop cover rendering ensuring zero distortion or letterboxing across varied viewports.
  - Multi-tier ambient drop shadow with subtle elevation on hover.
  - **Completely Borderless**: Zero hairline borders or stroke outlines for an organic, edge-to-edge fluid presentation.
  - Base placeholder surface: `#E4EAF1` sky slate tint matching the header watermark.
- **Animated Arrow Button (bottom-right corner)**:
  - Circular pill (`52×52px oval`), positioned `24px` from the card's bottom-right edge.
  - **Resting state**: Dark navy background (`#0F172A`) with a white `→` arrow (horizontal shaft + chevron head).
  - **Hover state**: Brand orange background (`#FA5909`) with a white `↗` arrow (rotated `-45°`).
  - Transition driven by a `16ms javax.swing.Timer` over `~180ms`, linearly interpolating `arrowProgress` (0.0 → 1.0):
    - Background color: `lerp(#0F172A → #FA5909, arrowProgress)`.
    - Arrow rotation: `rotate(-45° × arrowProgress)`.
  - Drop shadow intensifies on hover (`alpha 30 → 55`, `dy 2 → 4`) for lift effect.
  - Arrow geometry: `2.5px` round-cap/join stroke; shaft half-length `9px`; chevron arm `7px`.
- **Per-Character Kinetic Reveal Animation (`paintMonumentName`)**:
  - **Typographic Z-Ordering & Tight Line Spacing**: Monument title is rendered in monumental Bebas Neue Bold (`96pt` for multi-line, `88pt` for single-line; with `80pt` preserved for character-dense 3-line layout `STATUE\nOF\nUNITY`, and `76pt` calibrated for `HAWA\nMAHAL`) in deep slate (`#0F172A`), placed **behind the scenic image** in z-order so it reveals dynamically through photographic transparency/silhouettes. Line spacing between consecutive lines is reduced to $\text{lineStep} \approx \text{ascent} \times 1.04$ ($\sim70\text{px}$ vs $90\text{px}$) for a compressed, impactful editorial headline aesthetic.
  - **Chopped-Off Slot Masking (Zero Fade / 100% Solid Opacity)**: Each character renders into its individual bounding-box clip slot with height clamped precisely to $\text{lineStep}$. No alpha blending is applied; characters emerge from completely outside the clip slot from below and slice off cleanly at slot boundaries with zero cross-line visual interference.
  - **Full Out-of-Screen Travel Distance**: Characters travel $\text{travelDistance} = \text{ascent} + 20\text{px}$ ($\sim88\text{px}$). At rest or start of animation, characters are positioned completely below the clip slot (zero pixels visible), emerging smoothly upward into view and sinking completely below the slot before disappearing on exit.
  - **Custom Octic Ease-Out Curve ($E(p) = 1 - (1 - p)^8$) & Extended $1400\text{ms}$ Duration**: Custom non-linear velocity profile engineered for snappy initial entrance followed by an extremely slow, prolonged, liquid landing tail ("starts quickly and lands with a slow, feather-soft crawl").
  - **Character Stagger Delay ($30\text{ms}$)**: Delay between consecutive character launches is calibrated to $30\text{ms}$, producing an organic, rhythmic wave of emerging letterforms.
  - **Simultaneous Multi-Line Animation**: Characters across multiple lines share within-line character stagger indices (`staggerIdx[ci] = ci`), starting enter and exit animations simultaneously across all lines.
  - **Curated Layout Styles**:
    - `""` (Single/Multi-line Centered): Each line horizontally centered (e.g. `LAKE\nPALACE`, `MUNNAR`, `ALLEPPEY`).
    - `"left"` (Multi-line Left-Aligned): Left-aligned against the 28px left margin (`HAWA\nMAHAL` uses split diagonal layout with line 0 `HAWA` flush left at 28px and line 1 `MAHAL` flush right against a safe 36px right margin, with a 50% vertical interlock + 26px downward offset).
    - `"right"` (Multi-line Right-Aligned): Right-aligned against a 28px margin (e.g. `VARANASI\nGHATS`, `STATUE\nOF\nUNITY`).
    - `"staircase"` / `"staircase:<ratio>"` (Multi-line Indented Staircase): Line 0 left-aligned at 28px; line 1 begins horizontally at a configurable percentage of line 0's width (e.g. 100% for `DAL\nLAKE`; 55% for `PANGONG\nTSO`), stepped downward (`TAJ\nMAHAL` uses a compact 50% vertical drop + 6px downward offset for `MAHAL` relative to `TAJ`).
  - **Instantaneous Zero-Lag Exit & Symmetric Timing Physics**: The instant the user's cursor leaves the card (`mouseExited`), the exit animation initiates immediately with zero waiting or queue delays. The closing animation takes the **exact same duration as the opening animation** ($1400\text{ms}$ per-character duration, $30\text{ms}$ reverse stagger, total duration $\text{charTotalMs} = (L_{max}-1) \times 30 + 1400\text{ms}$). Using the symmetric liquid curve $E(p) = 1 - (1 - p)^8$, characters plunge down swiftly on initial un-hover before easing into a slow, buttery landing. Per-character continuous displacement snapshots (`animStartSlideY` from `currentCharSlideY`) guarantee seamless reversals mid-flight with zero visual popping or jumps if the user rapidly hovers and un-hovers across cards.
- **Location Row (`paintLocationRow`)**:
  - Rendered **above the image** in bottom-left row aligned with the arrow button, featuring a frosted glass pill backdrop, orange accent dot, and Roboto Bold 12px location label (`#0F172A`).
  - Subtle fade-up animation ($10\text{px}$ slide up with quadratic ease-out) coordinated with card hover state.

---

## 10. Custom Modern Animated Selectors & Date Picker Architecture

To maintain the Universal Light Theme, borderless glassmorphism, and fluid 60 FPS animation standards, standard Swing `JComboBox` and raw text inputs have been replaced with dedicated custom components: `ModernSmoothDropdown<T>` and `ModernDatePicker`.

### 10.1 `ModernSmoothDropdown<T>`
- **Architecture**: A borderless floating dropdown card rendered within an undecorated, translucent `JWindow` anchored to the root window hierarchy.
- **Visual Discipline (Strict 0px Border & Soft Ambient Shadow)**:
  - **Zero Border**: In accordance with RailFlow's borderless card directive, the dropdown card features strictly **0px border** (no hairline or stroke outlines).
  - **4-Tier Soft Ambient Drop Shadow**: Replaces single harsh drop shadows with 4 diffused, feathered shadow layers:
    - Layer 1: `rgba(0, 0, 0, 0.04)` at `(x - 6, y + 8, w + 12, h + 2)`, arc `38px`
    - Layer 2: `rgba(0, 0, 0, 0.06)` at `(x - 4, y + 5, w + 8, h)`, arc `36px`
    - Layer 3: `rgba(0, 0, 0, 0.10)` at `(x - 2, y + 3, w + 4, h - 2)`, arc `34px`
    - Layer 4: `rgba(0, 0, 0, 0.14)` at `(x - 1, y + 1, w + 2, h - 2)`, arc `32px`
  - **Surface**: Ultra-clean frosted milk-glass white (`rgba(255, 255, 255, 0.996)` / 254 alpha) with `32px` rounded card corners (`CARD_ARC = 32`).
- **Screen Boundary Collision & Auto-Flip**:
  - Automatically queries active `GraphicsConfiguration` screen insets (taskbars, macOS dock/menu bar).
  - Compares available vertical room below trigger (`spaceBelow`) versus above trigger (`spaceAbove`).
  - If `spaceBelow < naturalHeight`, automatically flips upward to render above the trigger with inverted spring entrance.
  - Clamps coordinates strictly within `[screenLeft, screenRight - popupWidth]` and `[screenTop, screenBottom - popupHeight]` to guarantee zero off-screen overflow under any window position or multi-monitor setup.
- **Kinetic Micro-Interactions**:
  - Chevron arrow smoothly rotates from down ($0^\circ$) to up ($180^\circ$) over 120ms with cubic deceleration.
  - Hover highlights and selection state utilize brand orange (`#FA5909`) with soft amber rounded pill pills (`#FFF7ED`).
  - Global `AWTEventListener` outside-click listener smoothly dismisses the popup when clicking anywhere outside the component.

### 10.2 `ModernDatePicker`
- **Architecture**: Custom interactive calendar popup with monthly grid navigation, fast presets, and month header.
- **Year-Less Compact Trigger Sizing**:
  - Formatted strictly as **`"EEE, dd MMM"`** (e.g., `"Fri, 02 Oct"`, omitting redundant year digits).
  - Reduces trigger label width by over 30% (`preferredSize` set to `96 x 24px`), freeing up horizontal real estate within the search capsule for long origin and destination station names.
- **Visual Discipline & Elevation**:
  - Strictly **0px border** with identical 4-tier soft ambient drop shadow.
  - Month and year title rendered in monumental **Bebas Neue Bold** (`18pt`).
  - Circular navigation buttons (`<` and `>`) and quick-select presets (`Today`, `Tomorrow`) with rounded pill geometry.
  - Selected date highlighted with solid Brand Orange (`#FA5909`) pill and pure white typography.
  - Disabled past dates rendered in muted slate (`#CBD5E1`) and unclickable.
- **Screen Collision Detection**:
  - Employs identical screen-aware boundary detection and auto-flip physics as `ModernSmoothDropdown`, ensuring the calendar window never clips outside viewport or display boundaries.

### 10.3 `StationAutocompleteDropdown` & Search Capsule Space Allocation
- **Architecture**: A dedicated autocomplete selector window (`StationAutocompleteDropdown`) bound to the `FROM` and `TO` search fields.
- **Search Capsule Width Stabilization (Zero Horizontal Resizing Jitter)**:
  - In standard Swing, `JTextField` dynamically changes its preferred width based on `FontMetrics.stringWidth(getText())`, which in a `GridBagLayout` causes adjacent columns to resize, oscillate, and shift noticeably while typing.
  - **The Fix**: Both `fromField` and `toField` override `getPreferredSize()` to return a constant baseline width (`80 x 24px`), while `SearchCapsulePanel` configures `GridBagConstraints.weightx = 0.50` on column 0 (`FROM`) and column 2 (`TO`), with `weightx = 0.0` on fixed columns (Swap button, Date, Quota, Concession, Search button).
  - This guarantees that 100% of the remaining horizontal space in the capsule is evenly split between origin and destination stations, remaining completely rock-solid and stable regardless of text length or cursor movement.
- **Real-Time Interactive Autocomplete**:
  - Automatically matches substrings against station name, official 3-4 letter IR code (`NDLS`, `MMCT`, `CSMT`), city (`Mumbai`, `Delhi`), and state (`Maharashtra`).
  - Displays official station code badge pills (`[NDLS]`, `[MMCT]`) in muted blue/orange alongside station name and city/state subtitle.
  - Full keyboard accessibility: Up/Down arrow navigation (`VK_UP`, `VK_DOWN`), selection on Enter (`VK_ENTER`), dismissal on Escape (`VK_ESCAPE`).
  - Borderless 0px card with 4-tier soft ambient drop shadow and screen boundary detection.
- **Strict Catalog Validation**:
  - Passengers are restricted to selecting valid railway stations available in the catalog.
  - If a user types arbitrary or unrecognized text and navigates away (focus loss), the input field automatically rejects the invalid entry and smoothly reverts to the previously selected valid station.

### 10.4 Strict Video Background & Canvas Isolation
- **Home View Exclusive Video**:
  - The looping background video player (`VideoBackgroundPanel`) and its play/pause state machine operate exclusively on the Home / Book Journey view (`BOOK_JOURNEY`).
- **Complete Canvas Opadity on Internal Views**:
  - Navigating to `TrainsPageView` (train search results), `MyBookingsPageView` (passenger journey history), or `AdminDashboardView` (operations console) explicitly halts video playback (`pauseVideo()`) and hides the video panel (`setVisible(false)`).
  - The root container panels, content wrappers, and scroll viewports for all secondary pages have `setOpaque(true)` and background `#F8FAFC` (Clean Slate Canvas).
  - The ambient top dark scrim gradient on `MainFrame` is conditionally painted only when `isHomeViewActive == true`. On all other pages, a crisp light canvas header is maintained with zero paused video frames or dark overlays bleeding through.

