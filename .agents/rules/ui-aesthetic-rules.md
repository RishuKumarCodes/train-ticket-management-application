# Rule: UI Aesthetic, Geometry & Minimalist Discipline

This rule defines the strict visual and geometric constraints for all screens, dialogs, and components in RailFlow.

---

## 1. Corner Radius & Geometry Standards
 
 | Component Type | Corner Radius / Arc | Description |
 |---|---|---|
 | **Cards, Containers & Dialog Modals** | **50px (`arc: 100`, `arcWidth = 100`)** | Identical curvature to Featured Destinations cards. **STRICTLY 0px BORDER** (`borderWidth: 0`). Hairline borders are completely prohibited on cards. |
 | **Buttons (`JellyButton`, action pills)** | **Pill (`arc: 999`, height / 2)** | Fully rounded pills. No sharp or rectangular edges allowed. |
 | **Input Fields & Password Boxes** | **Pill (`arc: 999`, height / 2)** | 100% full rounded pill inputs with generous horizontal padding (16px-18px). |
 | **Badges & Tags** | **Pill (12px - 16px, `arc: 999`)** | Minimal pill tags for status (e.g. `AVAILABLE`, `CONFIRMED`). |
 | **Modal Dialogs (`ModernModalDialog`)** | **50px (`arc: 100`) + Pill Controls** | Separate modal windows with macOS traffic lights, drag-to-move, 60 FPS enter/exit spring animations, and multi-tier ambient shadows. |

---

## 2. Minimalist & Sleek Aesthetic Standards

1. **Uncluttered Monumental Headings (Bebas Neue Mandate)**:
   - **Main Headings MUST Use Bebas Neue**: All main headings across cards, dialog modals, headers, and screens MUST use **Bebas Neue** (`AssetManager.getFont("Bebas Neue", Font.BOLD, ...)`).
   - **NO Header Icons**: Do NOT place emojis, shield icons, lightning bolts, or vector symbols beside or above titles.
   - **NO Small Orange Eyebrow Titles**: Do NOT add extra small orange titles above headings (e.g., remove `RAILFLOW PASSENGER`, `STATION MASTER DISPATCH`).
   - **NO Description Below Titles**: Do NOT add explanatory helper sentences or subtitles directly below headings. Keep the title clean, monumental, and confident.

2. **Borderless Cards & Containers**:
   - Every card surface (dialog panels, destination cards, metric widgets) must have **NO borders** (`borderWidth: 0`, no `g2.drawRoundRect(...)`).
   - Depth and separation are achieved strictly through clean white `#FFFFFF` or frosted milk glass `rgba(255, 255, 255, 248)` fills and soft ambient multi-tiered drop shadows (`new Color(0, 0, 0, 12)` and `new Color(0, 0, 0, 20)`).

3. **Cross-Page Cohesion & Uniformity (Universal Light Theme Mandate)**:
   - **Zero Dark Themes**: The entire application (Home page, Train Search, Search Results, Route Timetable, Passenger Authentication, Booking Checkout, Admin Command Center) MUST strictly use the Light Theme. Dark backgrounds or cyber-slate interfaces are forbidden.
   - Every single view MUST use the exact same:
     - Design tokens (colors, font hierarchy, spacing).
     - Component primitives (`JellyButton`, `GlassCard`, pill inputs).
     - Animation response curves (uniform stretchy liquid feel).

4. **Color Palette & Design Tokens (Home Page Palette)**:
   - **Primary Canvas**: Clean Slate/Sky Base (`#F8FAFC`).
   - **Card & Sheet Surfaces**: Crisp Pure White (`#FFFFFF`) or Frosted Milk Glass (`rgba(255, 255, 255, 0.92)` / `235-248 alpha`).
   - **Primary Text**: Deep Obsidian / Slate (`#0F172A`).
   - **Subdued Text / Labels**: Muted Slate (`#64748B`).
   - **Primary Accent**: Brand Orange (`#FA5909`, hover `#E04D05`, pressed `#C93F00`).
   - **Secondary Pill Accents**:
     - Sky Blue (`#2563EB` text on `#EFF6FF` pill / `#0284C7` for admin portal)
     - Emerald Green (`#10B981` text on `#ECFDF5` pill)
     - Pastel Amber (`#EA580C` text on `#FFF7ED` pill)
     - Pastel Purple (`#9333EA` text on `#FAF5FF` pill)
   - **Error / Danger**: Crimson Coral (`#EF4444`).

5. **Typography Scale**:
   - **Main Headings / Display Titles**: **Bebas Neue** (24px - 40px uppercase, `#0F172A`).
   - **Field Labels**: Roboto Bold 10px - 11px uppercase in `#64748B`.
   - **Inputs & Body**: Roboto Bold 14px - 15px (or Plain 13px) in `#0F172A`.
   - **Button Typography**: Roboto Bold 13px - 14px (White or `#0F172A`).

