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

## 3. Color Tokens & Glassmorphism

| Token Name | Hex / RGBA | Role |
|---|---|---|
| `--bg-base` | `#0B0F19` | Main canvas background |
| `--surface-card` | `rgba(255, 255, 255, 0.05)` | Glassmorphic cards |
| `--surface-card-hover`| `rgba(255, 255, 255, 0.09)` | Hover elevation surface |
| `--border-subtle` | `rgba(255, 255, 255, 0.10)` | Hairline card border (1px) |
| `--accent-primary` | `#FA5909` | Primary brand orange actions & active states |
| `--accent-hover` | `#E04D05` | Brand orange hover fill |
| `--accent-emerald` | `#10B981` | Available seats, confirmed status |
| `--text-primary` | `#F8FAFC` | Primary text |
| `--text-muted` | `#94A3B8` | Subdued secondary labels |

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

## 5. Header Branding & Navigation Tabs

- **Heading Brand Icon**: `src/main/resources/assets/icons/icon-wide.png` (Aspect ratio $5:1$, $2000 \times 400\text{px}$).
- **Render Geometry**: Dynamically scaled to $175 \times 35\text{px}$ (or $180 \times 36\text{px}$) with bicubic interpolation (`RenderingHints.VALUE_INTERPOLATION_BICUBIC` / `Image.SCALE_SMOOTH`).
- **Extended Soft Downward Gradient Scrim**: Rendered by `uiOverlayPanel` across an extended $240\text{px}$ vertical falloff (`LinearGradientPaint` with 4 color stops: `rgba(0,0,0,0.37)` [alpha 95] at $y=0$ fading smoothly through `rgba(0,0,0,0.20)` at $y=84\text{px}$, `rgba(0,0,0,0.06)` at $y=168\text{px}$, and fully dissipating to `rgba(0,0,0,0)` at $y=240\text{px}$). This gentle, feather-soft transition provides optimal contrast and clarity for the white brand logo and navigation pills while blending imperceptibly into the ambient video background without any visible dark line or abrupt cutoff.
- **Borderless & Clean Tabs**: Header navigation tabs (`Book Journey`, `PNR Status`, `Train Schedule`, `Admin`, `My Account`) are rendered completely free of background capsules and outlines (`background: #00000000; borderWidth: 0;`).
- **Active State Highlighting**: Active tab highlights in primary brand orange (`#FA5909`, Inter Bold 14), while inactive tabs rest in high-contrast off-white (`#F1F5F9`, hover `#FFFFFF`).
- **Interactive Cursor**: `Cursor.HAND_CURSOR` across all interactive elements.

---

## 6. Cinematic Background Video & Bottom-Anchored Booking Card

Instead of a static obsidian canvas, RailFlow embeds an ambient full-screen looping video background behind the interface:

- **Video Asset**: `src/main/resources/assets/videos/hero.mp4` (1080p, 25.33s loop).
- **Embedded Player Architecture**: JavaFX `MediaPlayer` integrated inside Swing via `JFXPanel` under `JLayeredPane.DEFAULT_LAYER`.
- **Cover Sizing Algorithm**: Aspect ratio ($16:9$) is preserved dynamically using standard CSS `object-fit: cover` logic. Excess dimensions are cropped symmetrically without distortion or letterboxing.
- **Direct Video Rendering**: The video plays with 100% raw vibrancy and color saturation, without any artificial dark tint overlay covering the viewport.
- **Bottom-Anchored Floating White Card**: The train ticket search card and hero headlines are anchored at the bottom of the screen (`Box.createVerticalGlue()` at the top), leaving the upper and middle screen completely open to showcase the cinematic railway journey video.
- **Floating White Cards & High Contrast**: UI cards utilize clean, crisp white surfaces (`#FFFFFF`) with 24px corner curvature and soft ambient borders, providing maximum readability against the active moving train footage.
- **Hardware & Battery Optimization**:
  - Audio track is muted by default to eliminate unexpected background audio.
  - Video playback automatically pauses on window minimization (`windowIconified`) and resumes when restored (`windowDeiconified`).
  - Resources and native pipelines are freed on window close (`windowClosing`).

---

## 7. Ambient Journey Audio (`hero.wav`) & Dynamic OS Output Routing

RailFlow features an optional immersive ambient audio experience that complements the background video with full macOS audio hardware device routing:

- **Audio Asset**: `src/main/resources/assets/audio/hero.wav` (44.1 kHz, 16-bit stereo PCM loop).
- **Interactive Control**: A minimalist speaker icon button (`FlatSVGIcon` using `assets/icons/speaker-off.svg` and `speaker-on.svg`) positioned in the top-right header navigation next to "My Account".
- **Toggle Dynamics**:
  - **Muted State**: Displays `speaker-off.svg` in off-white (`#F1F5F9`) with tooltip `"Play ambient journey sound • Right-click to switch device"`.
  - **Playing State**: Displays `speaker-on.svg` in vibrant brand orange (`#FA5909`) with dynamic tooltip `"Mute ambient sound ([Active Device]) • Right-click to switch device"`.
- **Dynamic OS Output Routing Engine**:
  - Managed by `AudioManager` utilizing Java Sound (`javax.sound.sampled`).
  - Native CoreAudio device interrogation dynamically queries `kAudioHardwarePropertyDefaultOutputDevice` on macOS to bind to the exact active system output device (e.g. `External Headphones`, `MacBook Air Speakers`, `HA220Q` external monitor).
  - **Auto-Migration Watcher**: While playing, a daemon watcher detects when the user changes their sound output in macOS System Settings or Control Center, seamlessly migrating the live audio stream to the newly selected device without interrupting playback or restarting the track.
- **Hardware Output Selector Context Menu**: Right-clicking the speaker button opens a sleek `JPopupMenu` (`arc: 16`) listing all available system audio devices (`Auto (System Default)`, `MacBook Air Speakers`, `External Headphones`, etc.) with active radio selection.
- **Window Lifecycle Integration**: Automatically pauses audio when the window is minimized (`windowIconified`), resumes maintaining position when un-minimized (`windowDeiconified`), and gracefully closes mixer lines on application exit (`windowClosing`).
