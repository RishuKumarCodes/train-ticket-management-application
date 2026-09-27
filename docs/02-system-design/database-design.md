# Database Design & Persistence Strategy
## RailFlow — Train Ticket Management Application
**Document Status:** Working Draft (To be evolved iteratively alongside code)

---

## 1. Database Philosophy & Persistence Approach
- **Engine**: MySQL 8.x / ANSI-SQL compatible relational database.
- **Access Strategy**: Pure JDBC using `PreparedStatement` to ensure SQL injection prevention and optimal driver-level query caching.
- **Connection Management**: High-performance connection pooling via **HikariCP**.
- **Transactions**: Multi-table operations (e.g. seat locking, booking creation, payment logging) will be wrapped in explicit transactions (`connection.setAutoCommit(false)` with rollback on exception).
- **Normalization**: Relational schema targeting Third Normal Form (3NF) to eliminate data redundancy.

---

## 2. Core Domain Entities (To Be Modeled)

As we develop the application modules, we will define and document the exact schemas here:

1. **User & Authentication Entity**: Passengers and administrative users with role-based access.
2. **Station & Route Entities**: Railway stations, routes, and intermediate halts.
3. **Train & Coach Entities**: Train types, coach compositions, and seating capacities.
4. **Schedule & Availability Entities**: Train runs, departure/arrival timetables, and real-time seat status.
5. **Booking & Passenger Entities**: Passenger reservations, unique PNR generation, and seat allocation labels.
6. **Payment & Transaction Entities**: Transaction auditing and refund records.

---

## 3. Entity Relationship Diagram (ERD)
*(The formal Mermaid ERD will be authored and updated here as we define each table's columns and foreign keys during implementation.)*

---

## 4. Data Dictionary & Table Schemas
*(Detailed column definitions, data types, primary/foreign keys, and indexes will be logged here as tables are added to `src/main/resources/db/schema.sql`.)*
