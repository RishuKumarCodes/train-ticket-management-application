# AGENTS.md — AI Engineering Guidelines for RailFlow

Welcome, AI Engineer / Pair Programmer! This repository hosts **RailFlow**, a modern desktop train ticket management application built with **Java 21**, **Swing**, **JDBC**, and **SQL**.

Our North Star for this application is: **rock-solid MVC architecture** combined with a **fluid, organic, jelly-flowing visual experience** that looks and feels like a modern 120Hz native app rather than a legacy Java Swing utility.

> ### 🚨 PRIME DIRECTIVE: MANDATORY CO-EVOLUTION OF CODE & DOCUMENTATION
> **Any code change, database modification, new UI component, or configuration tweak MUST be accompanied by an immediate update to the relevant documentation in `docs/` within the exact same turn/commit.** 
> - A task is **NEVER** complete until both the code AND the documentation are updated and in perfect alignment.
> - Stale, contradictory, or missing documentation is considered a critical bug and a delivery failure.
> - See detailed mappings in [`.agents/rules/mandatory-documentation-sync.md`](.agents/rules/mandatory-documentation-sync.md).

---

## 1. Core Architectural Pillars (Strict MVC)

Every component, model, and controller must strictly adhere to the Separation of Concerns:

```
                  +--------------------------------+
                  |           CONTROLLER           |
                  |  - Event Handlers & Listeners  |
                  |  - Async Worker Orchestration  |
                  |  - View-State Navigation       |
                  +---------------+----------------+
                                  |
               Dispatches Updates | Calls Actions
                                  v
+------------------+     State Changes     +------------------+
|      MODEL       | --------------------> |       VIEW       |
|  - Domain Entities|     (Listeners/Bus)  |  - Pure Swing UI |
|  - DAOs & Queries |                      |  - Jelly Physics |
|  - Business Rules |                      |  - Custom Render |
+------------------+                      +------------------+
```

### 1.1 Model Layer (`com.trainticket.model`)
- **Entities & DTOs**: Immutable where practical, containing pure business validation (e.g., ticket cancellation rules, age bounds).
- **Data Access Objects (DAOs)**: Pure JDBC using `PreparedStatement` only. Never concatenate raw SQL strings.
- **Connection Management**: Retrieve connections via `DatabaseManager` / `HikariCP` connection pool. Always use try-with-resources.
- **Zero UI Dependency**: Under no circumstances should any class in `com.trainticket.model` import `java.awt.*` or `javax.swing.*`.

### 1.2 View Layer (`com.trainticket.view`)
- **Passive Views**: Views should render state and expose interactive hooks (event listener setters, observational callbacks). They never execute SQL queries or heavy business calculations.
- **Universal Light Look & Feel**: Uses `FlatLightLaf` globally with custom frosted milk-glass rendering enhancements. Strictly no dark themes or dark surfaces anywhere in the application.
- **Glassmorphism & Gradients**: Subtle background blurs, rounded borders (`FlatBorder`), translucent milk-glass cards (`new Color(255, 255, 255, 235)` or pure white `#FFFFFF` sheets with `#E2E8F0` hairline borders).
- **Anti-Aliasing**: All custom paint routines MUST enable `RenderingHints`:
  ```java
  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
  g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
  ```

### 1.3 Controller Layer (`com.trainticket.controller`)
- Bridges user gestures from Views to Models.
- Handles validation, error feedback, screen transitions, and asynchronous operations.
- Triggers View animation state machines (e.g., trigger card expansion, shake on invalid input, ripple on click).

---

## 2. Swing Threading & Concurrency Contract (CRITICAL)

### Rule 2.1: The EDT Invariant
- **ALL UI MUTATIONS, REPAINTS, AND COMPONENT CREATION MUST RUN ON THE EVENT DISPATCH THREAD (EDT)**.
- If invoked outside the EDT:
  ```java
  SwingUtilities.invokeLater(() -> view.updateBookingState(booking));
  ```

### Rule 2.2: Zero Long-Running Tasks on the EDT
- **NEVER** run database calls, file I/O, heavy asset decoding, or network calls on the EDT.
- Use `SwingWorker<ResultType, IntermediateType>` or `CompletableFuture.runAsync()` with callback routed back via `SwingUtilities.invokeLater()`.
- Show animated jelly skeletons / shimmering loaders on the View while asynchronous tasks execute.

---

## 3. Apple-Like Smooth Jelly Stretchy Liquid Animation System

Every interaction, state change, and geometry resize MUST feel alive, fluid, and liquid:

1. **Volume-Preserving Squash & Stretch (Apple-Grade Liquid Motion)**:
   - When a component shrinks or is pressed, it must not just scale down uniformly; it must stretch in the perpendicular axis to simulate mass/liquid volume conservation:
     $$\text{Press}: \quad \text{Scale}_X = 1.06, \quad \text{Scale}_Y = 0.94$$
     $$\text{Release Overshoot}: \quad \text{Scale}_X = 0.96, \quad \text{Scale}_Y = 1.05 \longrightarrow 1.00$$
   - Any change in shape, width, height, or modal expansion must interpolate continuously with smooth spring elasticity rather than snapping or jumping.
2. **Spring Dynamics & Liquid Pacing**:
   - Avoid linear interpolations (`t`) and mechanical ease-in-out curves.
   - Use damped harmonic oscillator spring equations:
     - Stiffness ($k$): `160.0` - `220.0`
     - Damping Ratio ($\zeta$): `0.60` - `0.72` (produces organic liquid recoil without endless oscillation).
   - Frame rate: strictly 60 FPS (16ms loop) tied to the Swing EDT.
3. **Interactive Fluid Micro-Interactions**:
   - **Pill Buttons**: Morph and stretch with rubber-band tension on click and hover.
   - **Component Morphing**: Expanding a card into a modal must smoothly morph width, height, and corner radius dynamically.
   - **Seat Selection**: Seats ripple and squish organically upon selection.
   - **Toast & Overlays**: Liquid drop-in with bouncy settling curve from the top-center or top-right.

---

## 4. UI Geometry & Minimalist Aesthetic Contract (Strict Universal Light Theme)

### 4.1 Rounded Corners & Geometry Rules (Identical to Featured Destinations Cards)
- **Cards & Dialog Modals**: MUST have a **50px corner radius** (`arc: 100`, `arcWidth = 100`, `arcHeight = 100`, or `FlatClientProperties.STYLE = "arc: 100; borderWidth: 0;"`), identical to the Featured Destinations cards on the home page.
- **Card Borders**: **STRICTLY NO BORDERS ON ANY CARD**. Hairline borders (`#E2E8F0` or any stroke) are forbidden on cards. All depth and elevation are achieved purely through clean white/frosted glass surfaces and multi-tiered soft ambient drop shadows (`new Color(0, 0, 0, 12)` and `new Color(0, 0, 0, 20)`).
- **Buttons**: MUST be **full rounded pill buttons** (radius = height / 2, `arc: 999`). Stiff rectangular or sharp-edged buttons are strictly prohibited.
- **Text Inputs & Password Fields**: MUST be **full rounded pill shaped** (`arc: 999`, radius = height / 2) with generous inset padding (`16px-18px`), background `#F8FAFC`, border `#E2E8F0`, and focused border `#FA5909` (or `#0284C7` in admin context).
- **Popup Dialogs (`ModernModalDialog`)**: All modal dialogs MUST extend `ModernModalDialog`, operating as separate fixed-size windows with application modality (`APPLICATION_MODAL`), native macOS traffic lights (red close, yellow minimize, green status), draggable window headers, 60 FPS enter/exit spring animations, and multi-tier ambient shadows without edge clipping.

### 4.2 Sleek, Minimalist Visual Discipline & Uncluttered Typography
- **Main Headings MUST Use Bebas Neue**: All main headings across all cards, dialogs, modals, and screen sections MUST use the **Bebas Neue** font (`AssetManager.getFont("Bebas Neue", Font.BOLD, ...)`) in uppercase for a clean, monumental title aesthetic.
- **Zero Header Clutter (Strict Elimination Rule)**:
  - **NO ICONS**: Do NOT add icons (emojis, shields, lightning bolts, vector glyphs) in or next to main titles.
  - **NO SMALL ORANGE EYEBROW TITLES**: Do NOT add extra small orange titles/badges above the title (e.g., remove `RAILFLOW PASSENGER`, `STATION MASTER DISPATCH`).
  - **NO SUBTITLE DESCRIPTIONS**: Do NOT add explanatory subtitles or descriptions below the title.
  - Let the clean, monumental Bebas Neue headline stand proudly on its own with generous breathing space.
- **Zero Dark Themes**: The entire application (Home page, Search results, Booking, Seat selection, Login popups, Admin command center) MUST strictly use the Light Theme. Dark backgrounds or cyber-slate interfaces are forbidden.
- **Universal Color Tokens**:
  - **Canvas / Base Background**: Clean Slate/Sky Canvas (`#F8FAFC`).
  - **Card Surfaces**: Crisp Pure White (`#FFFFFF`) or Frosted Milk Glass (`rgba(255, 255, 255, 0.92)` / `235-248 alpha`).
  - **Primary Text**: Deep Obsidian / Slate (`#0F172A`).
  - **Subdued Text / Labels**: Muted Slate (`#64748B`).
  - **Primary Accent**: Brand Orange (`#FA5909`, hover `#E04D05`, pressed `#C93F00`).
  - **Secondary Accents**:
    - Sky Blue (`#2563EB` text on `#EFF6FF` pill / `#0284C7` admin portal accent)
    - Emerald Green (`#10B981` text on `#ECFDF5` pill)
    - Amber (`#EA580C` text on `#FFF7ED` pill)
    - Purple (`#9333EA` text on `#FAF5FF` pill)
  - **Error / Danger**: Coral Red (`#EF4444`).
- **Typography Scale**:
  - Main Headings / Display Titles: **Bebas Neue** (24px - 40px uppercase, `#0F172A`).
  - Field Labels: Roboto Bold 10px-11px uppercase in `#64748B`.
  - Body & Inputs: Roboto Bold 14px-15px (or Plain 13px) in `#0F172A`.
  - Buttons: Roboto Bold 13px-14px.


---

## 4. Asset Management & Pipeline

- **Images**: Located in `src/main/resources/assets/images/`. Support SVG via `FlatSVGIcon` for infinite resolution without pixelation, PNG for photographic textures.
- **Videos/Animations**: Located in `src/main/resources/assets/videos/`. Use looping frame animations or embedded media loaders.
- **Fonts**: Located in `src/main/resources/assets/fonts/` (Inter, Outfit, or JetBrains Mono).
- **Icons**: Located in `src/main/resources/assets/icons/`.
- All assets must be loaded via `AssetManager.load(...)` with an in-memory `SoftReference` cache to avoid memory leaks.

---

## 5. JDBC & Database Best Practices

1. **Connection Pooling**: Use `DatabaseConnectionPool.getConnection()` (backed by HikariCP).
2. **Transactions**: Any multi-step booking operation (e.g., reserve seat -> deduct balance -> generate ticket -> send notification) MUST be wrapped in:
   ```java
   connection.setAutoCommit(false);
   try {
       // operations...
       connection.commit();
   } catch (SQLException ex) {
       connection.rollback();
       throw ex;
   } finally {
       connection.setAutoCommit(true);
   }
   ```
3. **No Hardcoded SQL Constants in Views**: DAOs own all SQL schemas and queries.

---

## 6. How AI Agents Must Execute Tasks (Definition of Done)

Every task performed by an AI agent must pass this non-negotiable **Definition of Done (DoD)** checklist before finishing:

1. **Read Before Writing**: Always read existing models, DAOs, views, and controllers before modifying or adding code.
2. **Modular & Clean**: Write modular, readable code with descriptive variable names and full type safety.
3. **Simultaneous Documentation Sync (MANDATORY)**:
   - **Schema modified?** -> Update `src/main/resources/db/schema.sql` **AND** [`docs/02-system-design/database-design.md`](docs/02-system-design/database-design.md).
   - **New feature/flow added?** -> Update [`docs/01-feasibility-and-requirements/srs.md`](docs/01-feasibility-and-requirements/srs.md) with a new `REQ-xxx` ID **AND** add verification row in [`docs/04-testing/requirements-traceability-matrix.md`](docs/04-testing/requirements-traceability-matrix.md).
   - **New animated component/theme?** -> Document geometry (24px/pill) and physics ($k, \zeta$) in [`docs/02-system-design/ui-ux-design-system.md`](docs/02-system-design/ui-ux-design-system.md) and [`docs/ui-ux/jelly-animation-spec.md`](docs/ui-ux/jelly-animation-spec.md).
   - **New tests written?** -> Update [`docs/04-testing/test-plan.md`](docs/04-testing/test-plan.md) and mark the RTM status as `Passed`.
   - **Major architectural pivot?** -> Author a new record in [`docs/adr/`](docs/adr/).
4. **Zero Untracked Changes**: Any new class, table, or UI text must be reflected in the relevant documentation file in the same turn. Failure to update documentation alongside code is considered a broken delivery.

