# RailFlow Design System: Minimalist Glass & Liquid Motion

This document outlines the official design language, geometry specs, color tokens, and minimalism principles for RailFlow.

---

## 1. Geometry & Corner Radii

RailFlow strictly uses organic, rounded curvature throughout the desktop application:

```
[ Fully Rounded Pill Button: Arc = Height / 2 or 24px+ ]
(-------------------------------------------------------)

+-------------------------------------------------------+
|  Glass Card: Arc = 24px - 28px                        |
|                                                       |
|  [ Input Field: Arc = 22px ]                          |
|  (_________________________)                          |
|                                                       |
+-------------------------------------------------------+
```

### Specifications
- **Pill Buttons**: Corner radius equals half the height (or at least `24px`). Sharp corners are strictly forbidden.
- **Form Inputs**: `22px` - `24px` radius with `16px` horizontal padding.

- **Cards & Content Blocks**: `24px` - `28px` radius.
- **Modals / Popups**: `28px` radius floating overlays.

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

3. **Station Master Operations Console (`AdminDashboardFrame` & `AdminLoginDialog`)**:
   - **Admin Login Modal (`AdminLoginDialog`)**: Extends `ModernModalDialog` with $480 \times 460\text{px}$ card canvas, Bebas Neue title `STATION MASTER CONSOLE`, full pill Operator ID and Master Access Key inputs (`arc: 999`), Sky Blue pill authorize button (`#0284C7`), and cancel button.
   - **Admin Dashboard Layout (`AdminDashboardFrame`)**:
     - Sidebar: 250px pure white `#FFFFFF` sidebar with `#E2E8F0` right border, "RAILFLOW" title in `#0284C7`, pill navigation toggle buttons (`arc: 14`, selected background `#0284C7` with white text, hover `#F1F5F9`), operator badge, and pill exit button.
     - Top Command Bar: Solid white bar with live digital clock in `#0F172A` and pill database connection indicator badge (`#ECFDF5`/`#10B981`).
     - Operational Overview: 4 metric cards with 50px arc, `#FFFFFF` fill, 0px border, soft drop shadows, Bebas Neue metric headers, and high-contrast metric values; live train rosters table with 40px row height, `#FFFFFF` background, `#F8FAFC` headers, and status badges.

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
  - **Active Tab ("Book Journey")**: Solid pure white pill button (`#FFFFFF`, height 42px, width 132px) with dark slate typography (`#0F172A`, Roboto Bold 13px, `arc: 999`). Fits completely flush against the outer capsule with zero top, bottom, or left spacing (`FlowLayout.LEFT(0, 0)`), seamlessly coinciding with the capsule's outer left arc.
  - **Secondary Navigation Pills**: Translucent interactive buttons (`PNR Status`, `Train Schedule`, height 42px) with subtle glass hover highlights (`rgba(255, 255, 255, 0.14)`), styled in Roboto Bold 13px.
- **Header Actions (East)**:
  - **"Plan My Trip ↗" Action Button**: Pure white floating pill button (`#FFFFFF`, height 42px, `arc: 999`, width 160px) positioned directly to the left of the Login button, containing bold dark text (`#0F172A`, Roboto Bold 13px) and an embedded circular brand-orange badge (diameter 30px, `#FA5909`) with a crisp white diagonal arrow `↗` (`\u2197`).
  - **"Login" Button**: Pure white floating pill button (`#FFFFFF`, height 42px, `arc: 999`, width 72px) with dark slate typography (`#0F172A`, Roboto Bold 13px, padding 6px 14px).

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






