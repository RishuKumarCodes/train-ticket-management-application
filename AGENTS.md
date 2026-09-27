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
- **Modern Look & Feel**: Uses `FlatLaf` (Dark / Light themes) with custom rendering enhancements.
- **Glassmorphism & Gradients**: Subtle background blurs, rounded borders (`FlatBorder`), translucent cards (`new Color(255, 255, 255, 18)` or dark equivalents).
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

## 4. UI Geometry & Minimalist Aesthetic Contract

### 4.1 Rounded Corners & Geometry Rules
- **Buttons**: MUST be **full rounded pill buttons** (radius = height / 2) or have an explicit minimum **24px border radius** (`arcWidth = 48`, `arcHeight = 48`). Stiff rectangular or sharp-edged buttons are strictly prohibited.
- **Cards & Containers**: Generous rounded corners with **20px to 28px radius** (`FlatClientProperties.STYLE = "arc: 24"`).
- **Text Inputs & Search Fields**: Smooth pill or **20px-24px rounded borders** with subtle inset padding (12px-16px).
- **Modals & Overlays**: Floating glass panels with **28px corner radius** and soft ambient drop shadows.

### 4.2 Sleek, Minimalist Visual Discipline
- **Zero Unnecessary Text**: Eliminate redundant headers, explanatory tooltips, verbose instructions, and clutter. Let clean typography, intuitive icons, and layout hierarchy guide the user.
- **Universal Visual Consistency**: All pages (Train Search, Coach/Seat Selection, Booking Summary, Admin Dashboard) MUST share identical:
  - Color tokens (sleek dark glassmorphism, electric indigo / emerald green accents).
  - Button styles (identical 24px / pill geometry and hover dynamics).
  - Margins, paddings, and font scale.
- **Glassmorphism & Micro-Accents**: Subtle translucent background fills (`rgba(255, 255, 255, 0.05)` on dark surfaces) with 1px hairline borders (`rgba(255, 255, 255, 0.12)`).


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

