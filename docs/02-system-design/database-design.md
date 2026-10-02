# Database Design & Persistence Strategy
## RailFlow — Train Ticket Management Application
**Document Status:** Complete (Synced with live schema)  
**Detailed Tables Reference:** [SQL Tables Reference Manual](sql-tables-reference.md)

---

## 1. Database Philosophy & Persistence Approach
- **Engine**: MySQL 8.x / ANSI-SQL compatible relational database.
- **Access Strategy**: Pure JDBC using `PreparedStatement` to ensure SQL injection prevention and optimal driver-level query caching.
- **Connection Management**: High-performance connection pooling via **HikariCP**.
- **Transactions**: Multi-table operations (e.g. seat locking, booking creation, payment logging) will be wrapped in explicit transactions (`connection.setAutoCommit(false)` with rollback on exception).
- **Normalization**: Relational schema targeting Third Normal Form (3NF) to eliminate data redundancy.

---

## 2. Core Domain Entities

1. **User & Authentication Entity (`users`)**: Authenticated passengers and system administrators with Role-Based Access Control (RBAC). Passwords are cryptographically salted and hashed using PBKDF2 with HMAC-SHA-512 (65,536 iterations).
2. **Station & Route Entities**: Railway stations, routes, and intermediate halts (In progress).
3. **Train & Coach Entities**: Train types, coach compositions, and seating capacities (In progress).
4. **Schedule & Availability Entities**: Train runs, departure/arrival timetables, and real-time seat status.
5. **Booking & Passenger Entities**: Passenger reservations, unique PNR generation, and seat allocation labels.
6. **Payment & Transaction Entities**: Transaction auditing and refund records.

---

## 3. Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS {
        BIGINT id PK "Auto Increment"
        VARCHAR username UK "Internally derived unique username"
        VARCHAR email UK "Unique registered email (Nullable)"
        VARCHAR phone UK "Unique registered mobile phone (Nullable)"
        VARCHAR password_hash "PBKDF2-HMAC-SHA512 hex string (64 chars)"
        VARCHAR salt "Cryptographic random salt (32 chars hex)"
        VARCHAR full_name "Full legal passenger / admin name"
        ENUM role "PASSENGER, ADMIN"
        ENUM status "ACTIVE, SUSPENDED"
        TIMESTAMP created_at "Creation timestamp"
        TIMESTAMP updated_at "Auto-updated timestamp"
    }

    STATIONS {
        BIGINT id PK "Auto Increment"
        VARCHAR code UK "3-4 letter official IR station code"
        VARCHAR name "Full station display name"
        VARCHAR city "City name"
        VARCHAR state "State / Union Territory"
    }

    TRAINS {
        BIGINT id PK "Auto Increment"
        VARCHAR train_number UK "5-digit train number (e.g. 12952)"
        VARCHAR name "Official train name (e.g. Mumbai Rajdhani Express)"
        ENUM type "VANDE_BHARAT, RAJDHANI, SHATABDI, SUPERFAST, EXPRESS"
        BIGINT source_station_id FK "References stations(id)"
        BIGINT dest_station_id FK "References stations(id)"
        VARCHAR runs_on_days "7-char bitmask for Mon-Sun ('1111111')"
        ENUM status "ON_TIME, DEPARTED, DELAYED, CANCELLED"
    }

    TRAIN_ROUTES {
        BIGINT id PK "Auto Increment"
        BIGINT train_id FK "References trains(id)"
        BIGINT station_id FK "References stations(id)"
        INT stop_sequence "Ordered halt index (1, 2, 3...)"
        TIME arrival_time "Arrival time (null for origin)"
        TIME departure_time "Departure time (null for terminus)"
        INT halt_minutes "Stop duration in minutes"
        INT distance_km "Cumulative km from origin"
        INT day_count "Journey day counter (1, 2...)"
    }

    TRAIN_CLASSES {
        BIGINT id PK "Auto Increment"
        BIGINT train_id FK "References trains(id)"
        VARCHAR class_code "1A, 2A, 3A, SL, CC, EC"
        INT total_seats "Coach capacity"
        INT available_seats "Currently unreserved seats"
        INT rac_seats "RAC pool inventory"
        INT waitlist_seats "Waitlist pool inventory"
        DECIMAL base_fare "Standard general base fare (INR)"
    }

    BOOKINGS {
        BIGINT id PK "Auto Increment"
        VARCHAR pnr UK "10-character unique PNR (e.g. 284-9382194)"
        BIGINT user_id FK "References users(id), nullable for guest bookings"
        BIGINT train_id FK "References trains(id)"
        DATE travel_date "Scheduled date of journey departure"
        VARCHAR class_code "1A, 2A, 3A, SL, CC, EC"
        VARCHAR quota "GENERAL, TATKAL, PREMIUM_TATKAL, ALL_AC"
        VARCHAR concession "NONE, DIVYANGJAN, RAILWAY_PASS"
        DECIMAL total_fare "Total payable fare in INR"
        ENUM status "CONFIRMED, CANCELLED"
        TIMESTAMP created_at "Booking timestamp"
        TIMESTAMP updated_at "Update timestamp"
    }

    BOOKING_PASSENGERS {
        BIGINT id PK "Auto Increment"
        BIGINT booking_id FK "References bookings(id)"
        VARCHAR full_name "Full traveler legal name"
        INT age "Traveler age"
        VARCHAR gender "Traveler gender"
        VARCHAR seat_number "Assigned berth identifier (e.g. B4-23)"
        VARCHAR berth_type "LOWER, MIDDLE, UPPER, SIDE_LOWER, SIDE_UPPER"
        VARCHAR status "CONFIRMED, CANCELLED"
    }

    STATIONS ||--o{ TRAINS : "origin/dest"
    TRAINS ||--|{ TRAIN_ROUTES : "halts"
    STATIONS ||--o{ TRAIN_ROUTES : "located at"
    TRAINS ||--|{ TRAIN_CLASSES : "has seating inventory"
    USERS ||--o{ BOOKINGS : "places"
    TRAINS ||--o{ BOOKINGS : "reserved for"
    BOOKINGS ||--|{ BOOKING_PASSENGERS : "contains travelers"
```

---

## 4. Data Dictionary: Core Tables

### 4.1 Table: `users`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Unique identifier for user account |
| `username` | `VARCHAR(50)` | No | UK | None | Internal login/display identifier (auto-derived from email/phone) |
| `email` | `VARCHAR(100)` | Yes | UK | `NULL` | Registered email address (Primary auth identifier) |
| `phone` | `VARCHAR(20)` | Yes | UK | `NULL` | Registered mobile phone (Primary auth identifier) |
| `password_hash` | `VARCHAR(255)` | No | - | None | Hex-encoded PBKDF2-HMAC-SHA512 hash |
| `salt` | `VARCHAR(64)` | No | - | None | Cryptographically secure random 16-byte hex salt |
| `full_name` | `VARCHAR(100)` | No | - | None | Full legal passenger or administrator name |
| `role` | `ENUM('PASSENGER','ADMIN')` | No | - | `'PASSENGER'` | Access control tier |
| `status` | `ENUM('ACTIVE','SUSPENDED')`| No | - | `'ACTIVE'` | Account standing |
| `created_at` | `TIMESTAMP` | No | - | `CURRENT_TIMESTAMP` | Account creation timestamp |
| `updated_at` | `TIMESTAMP` | No | - | `CURRENT_TIMESTAMP` | Last profile update timestamp |

### 4.2 Table: `stations`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Station identifier |
| `code` | `VARCHAR(10)` | No | UK | None | Official station alphanumeric code (e.g. `NDLS`, `MMCT`, `BSB`) |
| `name` | `VARCHAR(100)` | No | - | None | Station display name (e.g. `New Delhi`, `Varanasi Junction`) |
| `city` | `VARCHAR(100)` | No | - | None | City name |
| `state` | `VARCHAR(100)` | No | - | None | State or Union Territory |

### 4.3 Table: `trains`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Train identifier |
| `train_number` | `VARCHAR(10)` | No | UK | None | Unique 5-digit train number (e.g. `12952`, `22436`) |
| `name` | `VARCHAR(100)` | No | - | None | Commercial train name |
| `type` | `ENUM` | No | - | `'EXPRESS'` | `VANDE_BHARAT`, `RAJDHANI`, `SHATABDI`, `SUPERFAST`, `EXPRESS` |
| `source_station_id` | `BIGINT` | No | FK | None | Origin station reference |
| `dest_station_id` | `BIGINT` | No | FK | None | Terminus station reference |
| `runs_on_days` | `VARCHAR(7)` | No | - | `'1111111'` | Active run schedule bitmask (Mon..Sun) |
| `status` | `ENUM` | No | - | `'ON_TIME'` | `ON_TIME`, `DEPARTED`, `DELAYED`, `CANCELLED` |

### 4.4 Table: `train_routes`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Route halt entry ID |
| `train_id` | `BIGINT` | No | FK | None | Parent train reference |
| `station_id` | `BIGINT` | No | FK | None | Halt station reference |
| `stop_sequence` | `INT` | No | - | None | Sequential stop index along the route (1, 2, 3...) |
| `arrival_time` | `TIME` | Yes | - | `NULL` | Scheduled arrival time (null at origin) |
| `departure_time` | `TIME` | Yes | - | `NULL` | Scheduled departure time (null at terminus) |
| `halt_minutes` | `INT` | No | - | `0` | Halt duration in minutes |
| `distance_km` | `INT` | No | - | `0` | Cumulative rail distance from origin |
| `day_count` | `INT` | No | - | `1` | Journey day counter |

### 4.5 Table: `train_classes`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Class pricing/inventory ID |
| `train_id` | `BIGINT` | No | FK | None | Parent train reference |
| `class_code` | `VARCHAR(10)` | No | - | None | `1A`, `2A`, `3A`, `SL`, `CC`, `EC` |
| `total_seats` | `INT` | No | - | `0` | Total coach class capacity |
| `available_seats`| `INT` | No | - | `0` | Real-time unreserved seat inventory |
| `rac_seats` | `INT` | No | - | `0` | Reservation Against Cancellation seats |
| `waitlist_seats`| `INT` | No | - | `0` | Waitlist bookings count |
| `base_fare` | `DECIMAL(10,2)`| No| - | `0.00` | Standard base fare in INR |

### 4.6 Table: `bookings`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Unique booking identifier |
| `pnr` | `VARCHAR(20)` | No | UK | None | Formatted 10-character unique PNR (e.g. `284-9382194`) |
| `user_id` | `BIGINT` | Yes | FK | `NULL` | Account holder reference (`users.id`), nullable for guest bookings |
| `train_id` | `BIGINT` | No | FK | None | Reserved train reference (`trains.id`) |
| `travel_date` | `DATE` | No | - | None | Scheduled departure date |
| `class_code` | `VARCHAR(10)` | No | - | `'SL'` | Booked travel class (`1A`, `2A`, `3A`, `SL`, `CC`, `EC`) |
| `quota` | `VARCHAR(30)` | No | - | `'GENERAL'` | Travel quota category |
| `concession` | `VARCHAR(40)` | No | - | `'NONE'` | Concession discount applied |
| `total_fare` | `DECIMAL(10,2)`| No| - | `0.00` | Total confirmed payable amount in INR |
| `status` | `ENUM` | No | - | `'CONFIRMED'`| Reservation state (`CONFIRMED`, `CANCELLED`) |
| `created_at` | `TIMESTAMP` | No | - | `CURRENT_TIMESTAMP` | Reservation creation timestamp |
| `updated_at` | `TIMESTAMP` | No | - | `CURRENT_TIMESTAMP` | Status change timestamp |

### 4.7 Table: `booking_passengers`
| Column | Type | Nullable | Key | Default | Description |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK | `AUTO_INCREMENT` | Passenger record identifier |
| `booking_id` | `BIGINT` | No | FK | None | Parent booking reference (`bookings.id`) |
| `full_name` | `VARCHAR(100)` | No | - | None | Traveler legal name |
| `age` | `INT` | No | - | None | Traveler age |
| `gender` | `VARCHAR(10)` | No | - | None | Traveler gender (`Male`, `Female`, `Other`) |
| `seat_number` | `VARCHAR(20)` | No | - | None | Assigned coach and seat label (e.g. `B4-23`) |
| `berth_type` | `VARCHAR(20)` | No | - | None | Berth preference (`LOWER`, `MIDDLE`, `UPPER`, `SIDE_LOWER`, etc.) |
| `status` | `VARCHAR(20)` | No | - | `'CONFIRMED'`| Traveler seat status |

---

## 5. Master Database Seeding & Maintenance Utilities

RailFlow provides automated zero-configuration database seeding via both GUI/shell launchers and a standalone CLI utility:

1. **Terminal Command Line**:
   ```bash
   ./RailFlow.command seed
   ```
   Or via Maven directly:
   ```bash
   mvn exec:java -Dexec.args="seed"
   ```

2. **macOS Finder Double-Click Launchers**:
   - `SeedDatabase.command`: Double-click directly in Finder to populate master tables.
   - `RailFlow.command`: Double-click in Finder; choose option `[2]` or let option `[1]` launch the desktop application automatically after 4 seconds.

3. **Seeded Data Portfolio & Master Database Scripts**:
   - **SQL Seeding Scripts**:
     - `src/main/resources/db/seed_data.sql`: Standalone master SQL script for importing full DDL/DML into any MySQL / MariaDB instance.
     - `src/main/resources/db/schema.sql`: Embedded schema parsed dynamically on startup or via `DatabaseSeeder`.
   - **Stations (50 Master Stations)**: Nationwide coverage across all 6 Indian Railway zones:
     - *North*: New Delhi (`NDLS`), Old Delhi (`DLI`), Hazrat Nizamuddin (`NZM`), Anand Vihar (`ANVT`), Chandigarh (`CDG`), Amritsar (`ASR`), Jammu Tawi (`JAT`), Varanasi (`BSB`), Kanpur Central (`CNB`), Prayagraj (`PRYJ`), Pt. Deen Dayal Upadhyaya (`DDU`), Lucknow Charbagh (`LKO`), Ghaziabad (`GZB`), Aligarh (`ALJN`), Agra Cantt (`AGC`), Gwalior (`GWL`), Gorakhpur (`GKP`).
     - *West*: Mumbai Central (`MMCT`), Chhatrapati Shivaji Maharaj Terminus (`CSMT`), Bandra Terminus (`BDTS`), Pune (`PUNE`), Ahmedabad (`ADI`), Surat (`ST`), Vadodara (`BRC`), Kota (`KOTA`), Jaipur (`JP`), Nagpur (`NGP`), Bhopal Habibganj / RKMP (`BPL`).
     - *South*: Chennai Central (`MAS`), Bengaluru City / KSR (`SBC`), Yesvantpur (`YPR`), Mysuru (`MYS`), Hyderabad Deccan (`HYB`), Secunderabad (`SC`), Vijayawada (`BZA`), Katpadi (`KPD`), Coimbatore (`CBE`), Madurai (`MDU`), Thiruvananthapuram Central (`TVC`), Ernakulam (`ERS`), Kozhikode (`CLT`).
     - *East*: Howrah (`HWH`), Sealdah (`SDAH`), Patna (`PNBE`), Bhubaneswar (`BBS`), Puri (`PURI`), Ranchi (`RNC`).
     - *Central & Northeast*: Raipur (`R`), Guwahati (`GHY`), Dibrugarh (`DBRG`).
   - **Train Fleet (22 Iconic Trains)**:
     - `12952`: New Delhi Tejas Rajdhani Express (NDLS ➔ MMCT)
     - `12954`: August Kranti Tejas Rajdhani Express (NDLS ➔ MMCT)
     - `22436`: Vande Bharat Express (NDLS ➔ BSB)
     - `12004`: Lucknow Swarna Shatabdi Express (NDLS ➔ LKO)
     - `12302`: Howrah Rajdhani Express (NDLS ➔ HWH)
     - `20607`: Vande Bharat Express (MAS ➔ MYS)
     - `12007`: Shatabdi Express (MAS ➔ MYS)
     - `12626`: Kerala Superfast Express (NDLS ➔ TVC via AGC, GWL, BPL, NGP, BZA, KPD, CBE, ERS)
     - `12138`: Punjab Mail (ASR ➔ CSMT via NDLS, AGC, GWL, BPL, BSL, MMR)
     - `12002`: New Delhi Shatabdi Express (NDLS ➔ BPL via AGC, GWL)
     - `22439`: Vande Bharat Express (NDLS ➔ JAT)
     - `12394`: Sampoorna Kranti Express (NDLS ➔ PNBE via CNB, DDU)
     - `12424`: Dibrugarh Rajdhani Express (NDLS ➔ DBRG via CNB, DDU, PNBE, GHY)
     - `12724`: Telangana Express (NDLS ➔ SC via AGC, GWL, BPL, NGP, KZJ)
     - `12622`: Tamil Nadu Express (NDLS ➔ MAS via AGC, GWL, BPL, NGP, BZA)
     - `12628`: Karnataka Express (NDLS ➔ SBC via AGC, GWL, BPL, NGP)
     - `12260`: Sealdah Duronto Express (NDLS ➔ SDAH via CNB, DDU)
     - `12802`: Purushottam Express (NDLS ➔ PURI via CNB, PRYJ, DDU, BBS)
     - `12951`: Mumbai Tejas Rajdhani Express (MMCT ➔ NDLS via ST, BRC, KOTA)
     - `12301`: Howrah New Delhi Rajdhani Express (HWH ➔ NDLS via DDU, PRYJ, CNB)
     - `22435`: Vande Bharat Express (BSB ➔ NDLS via PRYJ, CNB)
     - `20608`: Vande Bharat Express (MYS ➔ MAS via SBC, KPD)
   - **Halts & Inventory**: 110+ halt stop sequence records, 85+ coach class availability records with dynamic quota & concession pricing.
   - **Fixed Admin Seed**: `admin` / `admin` (Station Master dispatch account).
   - **Fixed Demo Passenger**: `passenger1` / `password123` (`passenger1@example.com`).


