# ADR 0002: Jelly Physics Animation Engine Architecture

## Status
Accepted

## Context
The application mandates "super fluid smooth jelly flowing animations of everything". Standard Swing components lack built-in motion physics or fluid interpolations. Using external, heavyweight animation libraries can lead to synchronization problems with the Swing Event Dispatch Thread (EDT) or sluggish frame pacing.

## Decision
Implement a lightweight, native Swing-compatible **Spring Physics Animation Engine** within `com.trainticket.util.animation`:
1. Use an underdamped harmonic oscillator differential equation solver ($F = -kx - cv$).
2. Drive frames using `javax.swing.Timer` at $16\text{ms}$ intervals directly bound to the EDT, eliminating multi-threading synchronization hazards on UI repaints.
3. Decouple physical state calculation from `Graphics2D` rendering.

## Consequences
- **Positive**: Zero external C/native library dependencies; zero EDT race conditions; organic, tactile rubber-band/jelly feedback for all interactive elements.
- **Negative / Trade-offs**: Care must be taken to restrict `repaint()` bounds to avoid burning CPU cycles on full-frame redraws.
