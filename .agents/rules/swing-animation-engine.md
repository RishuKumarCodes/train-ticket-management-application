# Rule: Apple-Grade Jelly & Stretchy Liquid Animation Engine

This rule defines the exact implementation parameters for fluid, liquid, stretchy animations across the RailFlow application.

## 1. Organic Liquid & Stretchy Physics
Standard linear or stiff cubic transitions look mechanical. To achieve Apple-like tactile liquidity:

### 1.1 Volume Preservation (Squash & Stretch)
Real liquid objects conserve volume: when compressed along one axis, they expand along the other:
$$\text{Area} = \text{Scale}_X \times \text{Scale}_Y \approx 1.0$$

- **Button Press Down**:
  - Compress along Y: $\text{Scale}_Y = 0.92$
  - Bulge along X: $\text{Scale}_X = 1.07$
- **Button Release (Spring Rebound)**:
  - Stretch along Y: $\text{Scale}_Y = 1.06$
  - Narrow along X: $\text{Scale}_X = 0.96$
  - Elastic settle: Damped oscillation back to $(1.00, 1.00)$ over $\approx 280\text{ms}$.

### 1.2 Shape & Size Morphing (Continuous Liquid Interpolation)
Whenever an element expands, contracts, or changes geometry (e.g., search bar expanding, card unfolding into ticket details, button turning into a progress indicator):
- **NEVER** instantly swap bounds or jump in size.
- Interpolate $(x, y, \text{width}, \text{height}, \text{cornerRadius})$ simultaneously using dual spring solvers.
- Corner radii must smoothly interpolate towards the target radius (e.g., morphing from a 24px pill button into a 28px rounded modal).

---

## 2. Spring Solver Configuration
Use the second-order spring differential equation:
```java
// Spring parameters tuned for liquid/stretchy organic feel
public static final double TENSION = 190.0;    // Spring stiffness (k)
public static final double FRICTION = 14.5;    // Damping coefficient (c)
public static final double EPSILON = 0.001;    // Convergence threshold
```

### Motion Profile
- **Hover Transitions**: Light spring cushion ($k = 220, c = 18$).
- **Click Feedback**: High-energy rubber squash ($k = 180, c = 12$).
- **Modal Popups / Overlays**: Overshooting gravity drop with liquid settle ($k = 160, c = 13$).
- **Page Transitions**: Fluid sliding curtain with inertia and slight parallax.

---

## 3. High-DPI Rendering & Graphics2D Quality
Every custom animated view component (`JellyButton`, `LiquidPanel`, `AnimatedPill`) must apply:
```java
g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
```
Clip damaged regions: `component.repaint(damageX, damageY, damageW, damageH)` rather than re-rendering untouched parent hierarchies.
