# Rule: UI Aesthetic, Geometry & Minimalist Discipline

This rule defines the strict visual and geometric constraints for all screens, dialogs, and components in RailFlow.

---

## 1. Corner Radius & Geometry Standards

| Component Type | Corner Radius / Arc | Description |
|---|---|---|
| **Buttons (`JellyButton`)** | **Pill (Height / 2) or Min 24px** | Fully rounded pills. No sharp or rectangular edges allowed. |
| **Input Fields / Search Bars** | **22px - 24px** | Pill-like inputs with generous horizontal padding (16px+). |
| **Cards & Containers** | **24px - 28px** | High curvature cards with subtle 1px border highlights. |
| **Modals & Dialogs** | **28px** | Floating glassmorphic surfaces with heavy backdrop blur. |
| **Badges & Tags** | **Pill (12px - 16px)** | Minimal pill tags for status (e.g. `AVAILABLE`, `CONFIRMED`). |

---

## 2. Minimalist & Sleek Aesthetic Standards

1. **Zero Text Clutter**:
   - Do NOT include unnecessary explanatory paragraphs, verbose helper texts, or repetitive labels.
   - Use clean, universally understood icons paired with single-word or short action labels (e.g., `Search`, `Select`, `Book`, `Confirm`).
   - Allow generous whitespace and breathing room rather than packing elements edge-to-edge.

2. **Cross-Page Cohesion & Uniformity**:
   - Every single view (Search, Seat Map, Passenger Entry, Ticket Summary, Admin Panel) MUST use the exact same:
     - Design tokens (colors, font hierarchy, spacing).
     - Component primitives (`JellyButton`, `GlassCard`, `MinimalInputField`).
     - Animation response curves (uniform stretchy liquid feel).

3. **Color Palette & Glassmorphic Surface**:
   - **Background**: Deep obsidian/slate (`#0B0E14` or `#0F172A`).
   - **Card Surfaces**: Translucent dark obsidian with 6%-10% white alpha (`rgba(255, 255, 255, 0.06)`).
   - **Borders**: 1px hairline stroke with 12% white alpha (`rgba(255, 255, 255, 0.12)`).
   - **Primary Accent**: Electric Indigo / Vivid Violet (`#6366F1` / `#8B5CF6`).
   - **Success / Available**: Emerald Teal (`#10B981`).
   - **Warning / Waiting**: Amber Warm (`#F59E0B`).
