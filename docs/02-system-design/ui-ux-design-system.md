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
| `--accent-primary` | `#6366F1` | Primary actions & active states |
| `--accent-hover` | `#4F46E5` | Hover fill |
| `--accent-emerald` | `#10B981` | Available seats, confirmed status |
| `--text-primary` | `#F8FAFC` | Primary text |
| `--text-muted` | `#94A3B8` | Subdued secondary labels |
