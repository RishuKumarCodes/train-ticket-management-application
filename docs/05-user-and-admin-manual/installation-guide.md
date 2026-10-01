# Installation & Operational Deployment Guide
## RailFlow — Train Ticket Management Application
**Project Phase:** Phase 5 (Deployment & User Operations)  

---

## 1. System Requirements
- **Hardware**: Dual-Core 2.0 GHz+, 4GB RAM minimum (8GB recommended), 500MB free disk space.
- **Display**: Minimum resolution $1024 \times 768$ (Full HD $1920 \times 1080$ recommended for high DPI).
- **Software**:
  - JDK 17 LTS or JDK 21 LTS (OpenJDK, Temurin, or Oracle JDK).
  - MySQL Server 8.0+ or compatible ANSI SQL database.
  - Apache Maven 3.8+ (e.g. installed via `brew install maven`).

---

## 2. Step-by-Step Installation

### Step 1: Database Setup & Seeding

RailFlow provides multiple automated ways to seed database tables, master stations, trains, and routes:

#### Option A: Automated Java Seeder (Recommended)
```bash
# Seed via Maven command
mvn exec:java -Dexec.args="seed"

# Or via the RailFlow macOS CLI launcher
./RailFlow.command seed

# Or double-click SeedDatabase.command in macOS Finder
```

#### Option B: Direct MySQL Console Execution
1. Log in to MySQL console:
   ```bash
   mysql -u root -p
   ```
2. Execute the schema initialization script:
   ```sql
   source src/main/resources/db/schema.sql;
   ```

*(Note: If MySQL is not running on your development workstation, RailFlow automatically detects this and gracefully falls back to a thread-safe in-memory dataset with 18 stations, 7 iconic express trains, and admin account, so you can explore without a local database).*

### Step 2: Configure Application Credentials
1. Copy the example configuration template:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Open `src/main/resources/application.properties` and verify your MySQL port, username, and password.
   > **Security Note**: `application.properties`, custom profile properties (`application-*.properties`), and `.env` files are excluded in `.gitignore` to prevent credential exposure.

### Step 3: Run the Application
You can run the application using any of the following methods:

#### Method A: Interactive macOS Launcher (Double-Clickable)
Double-click `RailFlow.command` in macOS Finder (or run `./RailFlow.command` in terminal). It offers a 2-option prompt that automatically launches the desktop application after 4 seconds if no selection is made.

#### Method B: From Terminal via Maven
```bash
mvn exec:java
```

#### Method C: Continuous Live-Reload Dev Mode
```bash
./dev.sh
```

#### Method D: Directly in Your IDE (IntelliJ / VS Code)
Open `src/main/java/com/trainticket/Main.java` and click the **Run ▶** button above `public static void main(String[] args)`.

#### Method E: Via Compiled Standalone Executable (.jar)
```bash
# Compile and assemble standalone fat JAR
mvn clean package

# Run the packaged JAR
java -jar target/train-ticket-management-1.0.0-SNAPSHOT.jar
```


