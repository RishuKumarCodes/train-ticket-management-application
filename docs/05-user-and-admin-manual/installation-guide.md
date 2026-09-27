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

### Step 3: Run the Application
You can run the application using any of the following methods:

#### Method A: From Terminal via Maven
```bash
mvn clean compile exec:java
```

#### Method B: Directly in Your IDE (VS Code / IntelliJ)
Open `src/main/java/com/trainticket/Main.java` and click the **Run ▶** button above `public static void main(String[] args)`.

#### Method C: Via Compiled Executable (.jar)
```bash
# Compile and assemble standalone fat JAR
mvn clean package

# Run the packaged JAR
java -jar target/train-ticket-management-1.0.0-SNAPSHOT.jar
```

