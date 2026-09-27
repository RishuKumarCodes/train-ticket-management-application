# Contributing to RailFlow

Thank you for contributing to **RailFlow**! Whether you are a team member, a peer student, an open-source contributor, or an AI pair programmer, this guide defines our standards for engineering, aesthetics, and documentation.

---

## 🌟 The Prime Directives

1. **Strict MVC Architecture**: Zero business or SQL logic in Views; zero Swing/AWT imports in Models.
2. **Apple-Grade Liquid Stretchy Physics & 24px Pill Geometry**: Every button must be a full rounded pill (`arc = height / 2`) or have a minimum `24px` border radius. All components squash, stretch, and spring organically. Zero sharp rectangles.
3. **The Swing EDT Invariant**: Never block the Event Dispatch Thread with database calls, file I/O, or heavy calculations. Always use `SwingWorker` or `CompletableFuture`.
4. **Mandatory Documentation Synchronization (Docs-as-Code)**: **No code change is complete without updating the corresponding documentation in `docs/` within the exact same pull request or commit.**

---

## 📋 Git Workflow & Branching Strategy

We follow a structured branch naming and pull request model:

### Branch Naming Conventions
- `feature/<short-description>`: New functional capabilities (e.g., `feature/pnr-search`, `feature/coach-seat-grid`).
- `fix/<short-description>`: Bug and defect fixes (e.g., `fix/edt-concurrency-leak`).
- `docs/<short-description>`: Academic and technical documentation enhancements (e.g., `docs/rtm-matrix-update`).
- `refactor/<short-description>`: Code restructuring without functional changes.

### Conventional Commit Messages
Please format all commit messages according to the Conventional Commits specification:
```text
feat(booking): implement atomic seat reservation with HikariCP
fix(animation): fix spring damping oscillation overshoot on pill button
docs(srs): add REQ-AUTH-03 for two-factor verification
test(dao): add rollback unit test for seat reservation collision
```

---

## 🛠️ Development Setup & Quality Checks

Before pushing code or opening a Pull Request, run the local verification suite:

```bash
# 1. Clean and compile
mvn clean compile

# 2. Run unit and integration tests
mvn test

# 3. Verify executable packaging
mvn package
```

---

## 📑 The Co-Evolution Matrix (Mandatory Docs Sync)

Before your Pull Request can be merged, verify that all applicable documentation has been updated:

| If You Modify... | You MUST Update... |
|---|---|
| Database Schema (`src/main/resources/db/schema.sql`) | [`docs/02-system-design/database-design.md`](docs/02-system-design/database-design.md) & Mermaid ERD |
| Features or Requirements | [`docs/01-feasibility-and-requirements/srs.md`](docs/01-feasibility-and-requirements/srs.md) & [`docs/04-testing/requirements-traceability-matrix.md`](docs/04-testing/requirements-traceability-matrix.md) |
| UI Components or Animations | [`docs/02-system-design/ui-ux-design-system.md`](docs/02-system-design/ui-ux-design-system.md) & [`docs/ui-ux/jelly-animation-spec.md`](docs/ui-ux/jelly-animation-spec.md) |
| Package Structure or Controllers | [`docs/03-implementation/module-specifications.md`](docs/03-implementation/module-specifications.md) |
| Test Suites | [`docs/04-testing/test-plan.md`](docs/04-testing/test-plan.md) & RTM status |

---

## 🎨 UI & Aesthetics Checklist
- [ ] Are buttons fully rounded pills or $\ge 24\text{px}$ radius?
- [ ] Is there **zero unnecessary explanatory text** or verbose clutter?
- [ ] Does the UI match across all pages (color tokens, font hierarchy, glassmorphism)?
- [ ] Does clicking trigger volume-preserving squash and stretch ($S_x \times S_y \approx 1$)?

---

## 🤝 Need Help?
Check out our [Documentation Hub](docs/README.md) or open an issue on the repository.
