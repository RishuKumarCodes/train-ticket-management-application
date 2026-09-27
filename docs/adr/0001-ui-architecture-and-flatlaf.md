# ADR 0001: UI Look & Feel Framework Selection (FlatLaf)

## Status
Accepted

## Context
Standard Java Swing components (Metal, Nimbus, or native platform themes) look dated, inconsistent across platforms (macOS vs Windows vs Linux), and have poor High-DPI support. We need a clean, customizable foundation that supports dark mode, SVG icons, and smooth modern borders without abandoning Swing.

Options considered:
1. **Standard Swing Nimbus**: Outdated styling, clunky customizations, difficult SVG integration.
2. **JavaFX**: Modern, but user requirements explicitly requested **Swing**.
3. **FlatLaf (Flat Look and Feel)**: State-of-the-art modern Swing Look and Feel with IntelliJ-grade theming, dynamic dark/light switching, vector SVG icon support, and complete High-DPI scaling.

## Decision
Adopt **FlatLaf** as the core Look and Feel provider, combined with custom `Graphics2D` rendering overlays for jelly animations.

## Consequences
- **Positive**: Clean, modern aesthetics out-of-the-box; built-in SVG rasterization via `FlatSVGIcon`; high-resolution display support.
- **Negative / Trade-offs**: Adds dependency on `com.formdev:flatlaf` and `flatlaf-extras`.
