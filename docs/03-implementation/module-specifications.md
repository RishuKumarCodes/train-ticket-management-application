# Module Specifications & Architecture Breakdown
## RailFlow — Train Ticket Management Application
**Document Status:** Living Technical Specification (Updated as classes are implemented)

---

## 1. High-Level Package Architecture

The project follows a standard 3-tier MVC directory structure:

```
com.trainticket/
├── config/                     # Configuration loaders (DB connection settings, app constants)
├── model/                      # Business domain, entities, DTOs, and JDBC DAOs
├── view/                       # Passive Swing views, panels, dialogs, and themes
├── controller/                 # Action listeners and EDT asynchronous dispatchers
└── util/                       # Shared utilities (DB connection pool, animation helpers, asset loader)
```

---

## 2. Layer Contracts & Responsibilities

1. **Model Layer (`com.trainticket.model`)**:
   - Entities represent database rows.
   - DAOs own all SQL queries and `PreparedStatement` executions.
   - Services coordinate multi-DAO atomic transactions.
   - **Rule**: Never import Swing or AWT classes.

2. **View Layer (`com.trainticket.view`)**:
   - Renders state and registers listener callbacks.
   - **Rule**: Never execute SQL queries or long-running computations.

3. **Controller Layer (`com.trainticket.controller`)**:
   - Handles user interactions from Views.
   - Delegates business tasks to Model/Services using asynchronous workers (`SwingWorker`).
   - Updates View state exclusively on the Swing Event Dispatch Thread (EDT).

---

## 3. Implemented Modules Log
*(As we write classes and controllers, they will be registered and documented below)*
