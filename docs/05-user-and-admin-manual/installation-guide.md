# Installation & Operational Deployment Guide
## RailFlow — Train Ticket Management Application
**Project Phase:** Phase 5 (Deployment & User Operations)  

---

## 1. System Requirements
- **Hardware**: Dual-Core 2.0 GHz+, 4GB RAM minimum (8GB recommended), 500MB free disk space.
- **Display**: Minimum resolution $1024 \times 768$ (Full HD $1920 \times 1080$ recommended for high DPI).
- **Software**:
  - JDK 21 (OpenJDK, Temurin, or Oracle JDK).
  - MySQL Server 8.0+ or compatible ANSI SQL database.
  - Apache Maven 3.8+.

---

## 2. Step-by-Step Installation

### Step 1: Database Setup
1. Log in to MySQL console:
   ```bash
   mysql -u root -p
   ```
2. Execute the schema initialization script:
   ```sql
   source src/main/resources/db/schema.sql;
   ```

### Step 2: Configure Application Credentials
1. Copy the example configuration template:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Open `src/main/resources/application.properties` and verify your MySQL port, username, and password.

### Step 3: Build & Package
To compile and assemble the executable JAR with all dependencies bundled:
```bash
mvn clean package
```
This produces the runnable jar inside the `target/` directory:
`target/train-ticket-management-1.0.0-SNAPSHOT.jar`

### Step 4: Run Application
Execute directly via Java or Maven:
```bash
# Via Maven
mvn exec:java

# Via Compiled Fat JAR
java -jar target/train-ticket-management-1.0.0-SNAPSHOT.jar
```
