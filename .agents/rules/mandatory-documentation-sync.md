# Rule: Mandatory Simultaneous Documentation Synchronization (Docs-as-Code)

> **CRITICAL & NON-NEGOTIABLE RULE**: Code and Documentation MUST ALWAYS co-evolve. **No task, pull request, bug fix, or feature is complete until its corresponding documentation in `docs/` is updated in the very same operation.**

---

## 1. The Co-Evolution Matrix

Whenever you modify any code or configuration, you MUST immediately update the corresponding documentation:

| When You Change / Create... | You MUST Simultaneously Update... |
|---|---|
| **SQL Schema / DDL** (`src/main/resources/db/schema.sql`) | 1. [`docs/02-system-design/database-design.md`](../../docs/02-system-design/database-design.md)<br>2. [`docs/database/schema-design.md`](../../docs/database/schema-design.md)<br>3. Entity-Relationship Diagrams (Mermaid) |
| **New Feature / Business Capability** | 1. [`docs/01-feasibility-and-requirements/srs.md`](../../docs/01-feasibility-and-requirements/srs.md) (Add `REQ-xxx` ID)<br>2. [`docs/04-testing/requirements-traceability-matrix.md`](../../docs/04-testing/requirements-traceability-matrix.md) (Map to module & test case) |
| **New UI Component / Animation / Theme** (`com.trainticket.view`) | 1. [`docs/02-system-design/ui-ux-design-system.md`](../../docs/02-system-design/ui-ux-design-system.md)<br>2. [`docs/ui-ux/jelly-animation-spec.md`](../../docs/ui-ux/jelly-animation-spec.md) (Record spring parameters $k, \zeta$ and pill radius) |
| **New Class, Package, or Controller Flow** (`com.trainticket.controller`, `model`, `util`) | 1. [`docs/03-implementation/module-specifications.md`](../../docs/03-implementation/module-specifications.md)<br>2. [`docs/02-system-design/high-level-architecture.md`](../../docs/02-system-design/high-level-architecture.md) |
| **Unit / Integration / System Test** (`src/test/java`) | 1. [`docs/04-testing/test-plan.md`](../../docs/04-testing/test-plan.md)<br>2. [`docs/04-testing/requirements-traceability-matrix.md`](../../docs/04-testing/requirements-traceability-matrix.md) (Mark status as Passed / Planned) |
| **Architectural or Library Decision** (e.g. state management, caching) | 1. Create a new numbered ADR in [`docs/adr/`](../../docs/adr/) |
| **Configuration / Environment / Run Steps** (`application.properties`, `pom.xml`) | 1. [`docs/05-user-and-admin-manual/installation-guide.md`](../../docs/05-user-and-admin-manual/installation-guide.md)<br>2. [`README.md`](../../README.md) |

---

## 2. Definition of Done (DoD) Checklist for AI Agents
Before signaling to the user that a task is finished:
- [ ] Has the source code been written/modified?
- [ ] Have the tests been written/verified?
- [ ] **HAS THE DOCUMENTATION IN `docs/` BEEN UPDATED TO REFLECT THE EXACT CURRENT STATE OF THE CODE?**
- [ ] Are Mermaid diagrams kept in sync with actual table/class structures?

**Failure to update documentation alongside code is considered an incomplete and broken delivery.**
