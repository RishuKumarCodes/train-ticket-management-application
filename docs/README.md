# RailFlow Master Documentation Hub & Waterfall Lifecycle Guide

Welcome to the central documentation repository for **RailFlow: Train Ticket Management Application**.

This hub solves the classic dilemma in academic engineering: **bridging the gap between living software documentation (for developers & AI pair programmers) and formal academic project reports (for university evaluation and viva voce defense)**.

---

## 🧭 The Gold Standard Architecture: Where & How to Store Docs

### 1. The Dilemma: Separate Website vs. Repo Markdown vs. `.docx`
| Format / Location | Pros | Cons | Verdict |
|---|---|---|---|
| **Separate Website** (Docusaurus / MkDocs) | Looks slick | Overkill for desktop app, requires hosting setup, difficult to submit as college thesis. | ❌ Not recommended initially. |
| **Direct `.docx` / Word** | What colleges require | Terrible for version control, impossible to diff in Git, gets out of sync with code instantly. | ❌ Do NOT make `.docx` your primary source. |
| **Markdown in Repo (`docs/`)** | **Single Source of Truth**, tracked in Git alongside code, supports Mermaid diagrams, readable anywhere. | Doesn't look like a standard university printed booklet. | **PRIMARY SOURCE OF TRUTH** |

### 2. The Recommended Hybrid Workflow
1. **Author & Maintain in Git Markdown (`docs/`)**:
   - Write all requirements, system designs, ER diagrams, and test cases in Markdown within `docs/`.
   - Diagrams are written as text using **Mermaid** (`erDiagram`, `sequenceDiagram`, `gantt`).
2. **Export to `.docx` / PDF on Demand for College Submissions**:
   - When report deadlines arrive, compile your markdown files into a university-formatted `.docx` or PDF using **Pandoc** or VS Code extensions (e.g. *Markdown Preview Enhanced* or *Pandoc PDF*):
     ```bash
     pandoc docs/01-feasibility-and-requirements/srs.md \
            docs/02-system-design/high-level-design.md \
            -o docs/06-final-report/Project_Report_Draft.docx
     ```
   - This ensures **you never have to rewrite the same documentation twice**.

---

## 📂 Waterfall Lifecycle Folder Map

Every directory corresponds to a sequential Waterfall SDLC phase:

```text
docs/
├── README.md                              # Documentation Master Plan & Lifecycle Guide (this file)
│
├── 01-feasibility-and-requirements/       # PHASE 1: Requirements Engineering
│   ├── problem-statement-and-scope.md     # Project motivation, constraints & boundaries
│   ├── feasibility-study.md               # Technical, operational, and economic feasibility + Gantt
│   └── srs.md                             # IEEE 830 / ISO 29148 compliant Software Requirements
│
├── 02-system-design/                      # PHASE 2: System Design (HLD & LLD)
│   ├── high-level-architecture.md         # 3-tier MVC, EDT threading model & subsystem architecture
│   ├── database-design.md                 # ERD, 3NF relational normalization & data dictionary
│   ├── sql-tables-reference.md            # Complete 7-table schema, data dictionary & DDL definitions
│   └── ui-ux-design-system.md             # 24px/pill geometry, Apple jelly spring physics & dark theme
│
├── 03-implementation/                     # PHASE 3: Coding & Package Architecture
│   ├── module-specifications.md           # Model, View, Controller, Util package responsibilities
│   └── coding-standards.md                # Java 21 conventions, HikariCP transactions & EDT rules
│
├── 04-testing/                            # PHASE 4: Verification & Quality Assurance
│   ├── test-plan.md                       # IEEE 829 test strategy & unit/integration test cases
│   └── requirements-traceability-matrix.md# NASA/IEEE RTM mapping REQ-xx -> Module -> Test Cases
│
├── 05-user-and-admin-manual/              # PHASE 5: Deployment & Operations
│   ├── installation-guide.md              # JDK 21, MySQL, Maven step-by-step setup
│   └── user-guide.md                      # Passenger booking & Admin roster user manual
│
├── 06-final-report/                       # PHASE 6: University Evaluation & Viva Voce
│   ├── college-report-template.md         # Complete chapter-by-chapter thesis structure
│   └── viva-presentation-guide.md         # Viva defense questions, slide deck guide & demo tips
│
└── adr/                                   # Architectural Decision Records
    ├── template.md                        # ADR template
    ├── 0001-ui-architecture-and-flatlaf.md# ADR: FlatLaf adoption
    └── 0002-jelly-physics-animation-system.md # ADR: Spring physics motion engine
```

---

## 🎯 What to Include vs. What NOT to Include

### ✅ What to Include (Critical for College + Engineering)
- **Clear Requirement Identifiers**: Every requirement has a stable tag (`REQ-AUTH-01`, `REQ-BKG-01`) for traceability.
- **Visual UML Diagrams**: Use Mermaid for Sequence, Class, and ER diagrams.
- **Requirements Traceability Matrix (RTM)**: Proves to professors that every single feature requested in the SRS was tested.
- **Clear Engineering Decisions**: Explain *why* Java Swing was augmented with custom physics and FlatLaf instead of legacy Nimbus.

### ❌ What NOT to Include
- **Raw Code Dumps**: Never paste hundreds of lines of Java source code into project reports. Include only concise code snippets illustrating key algorithms (e.g. spring equation solver, atomic transaction rollback).
- **Outdated / Stale Text**: Don't maintain separate notes that conflict with the actual database schema or UI layout. Update docs alongside code.
- **Generic Wikipedia Definitions**: Avoid pages of generic boilerplate like "What is Java?", "What is a database?". Keep explanations specific to **RailFlow's architecture and rationale**.

---

## 🔄 Maintenance Rule: The AI-Assisted Docs-as-Code Contract
When working with AI pair programmers:
- Every schema change in `src/main/resources/db/schema.sql` MUST trigger an update in `docs/02-system-design/database-design.md`.
- Every new controller or view flow MUST be referenced in the [RTM](docs/04-testing/requirements-traceability-matrix.md).
- Keep `AGENTS.md` updated so AI agents respect these documentation boundaries.
