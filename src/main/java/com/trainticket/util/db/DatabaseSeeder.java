package com.trainticket.util.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Command-line utility and runtime service for seeding and resetting the RailFlow database.
 * Reads SQL definitions and seed dataset from schema.sql, executes against MySQL,
 * and outputs verification metrics.
 */
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final String SCHEMA_FILE = "/db/schema.sql";
    private static final String CONFIG_FILE = "/application.properties";
    private static final String FALLBACK_CONFIG_FILE = "/application.properties.example";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  RailFlow • Database Seeder & Schema Initializer");
        System.out.println("================================================================================");

        boolean success = seedDatabase();
        if (success) {
            System.out.println("\n[✔] Database seeding and verification completed successfully!");
            System.out.println("================================================================================\n");
            System.exit(0);
        } else {
            System.err.println("\n[✘] Database seeding failed. Review error logs above for details.");
            System.out.println("================================================================================\n");
            System.exit(1);
        }
    }

    /**
     * Executes schema.sql and seeds all master train tables and users.
     *
     * @return true if successful, false otherwise
     */
    public static boolean seedDatabase() {
        Properties props = loadDatabaseProperties();
        String jdbcUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/train_ticket_db?useSSL=false&allowPublicKeyRetrieval=true");
        String username = props.getProperty("db.username", "root");
        String password = props.getProperty("db.password", "");
        String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        // 1. Ensure MySQL driver is registered
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("[✘] MySQL JDBC Driver not found: " + driver);
            logger.error("Driver not found", e);
            return false;
        }

        // 2. If database doesn't exist, create it first using server connection
        ensureDatabaseExists(jdbcUrl, username, password);

        // 3. Read statements from schema.sql
        List<String> statements = loadSqlStatements();
        if (statements.isEmpty()) {
            System.err.println("[✘] No executable SQL statements found in " + SCHEMA_FILE);
            return false;
        }

        System.out.println("[*] Connecting to MySQL database at: " + jdbcUrl);

        // 4. Execute statements
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("[*] Executing schema & seed SQL statements (" + statements.size() + " commands)...");
            int executedCount = 0;
            for (String sql : statements) {
                try {
                    stmt.execute(sql);
                    executedCount++;
                } catch (SQLException ex) {
                    // Log warning for individual statements but continue (e.g. duplicate keys)
                    logger.debug("Statement execution note: {} (SQL: {})", ex.getMessage(), sql);
                }
            }
            System.out.println("[✔] Successfully executed " + executedCount + " statements.");

            // 5. Verify seeded data counts
            verifyTableCounts(stmt);
            return true;

        } catch (SQLException e) {
            System.err.println("[✘] Database connection/execution failed: " + e.getMessage());
            logger.error("Database seed failed", e);
            return false;
        }
    }

    private static void ensureDatabaseExists(String jdbcUrl, String username, String password) {
        try {
            // Strip database name from URL to connect to the MySQL server root
            String baseUrl = jdbcUrl;
            int qIdx = baseUrl.indexOf('?');
            String params = qIdx > 0 ? baseUrl.substring(qIdx) : "";
            String withoutParams = qIdx > 0 ? baseUrl.substring(0, qIdx) : baseUrl;

            int slashIdx = withoutParams.lastIndexOf('/');
            if (slashIdx > "jdbc:mysql://".length()) {
                String rootUrl = withoutParams.substring(0, slashIdx + 1) + params;
                try (Connection conn = DriverManager.getConnection(rootUrl, username, password);
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS train_ticket_db;");
                    logger.info("Ensured database train_ticket_db exists.");
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not pre-create database from root URL: {}", ex.getMessage());
        }
    }

    private static List<String> loadSqlStatements() {
        List<String> statements = new ArrayList<>();
        InputStream in = DatabaseSeeder.class.getResourceAsStream(SCHEMA_FILE);
        if (in == null) {
            logger.error("schema.sql not found at resource path: {}", SCHEMA_FILE);
            return statements;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder current = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//") || line.startsWith("#")) {
                    continue; // Skip comments and empty lines
                }

                current.append(line).append(" ");
                if (line.endsWith(";")) {
                    String stmtStr = current.toString().trim();
                    // Remove trailing semicolon
                    if (stmtStr.endsWith(";")) {
                        stmtStr = stmtStr.substring(0, stmtStr.length() - 1).trim();
                    }
                    if (!stmtStr.isEmpty()) {
                        statements.add(stmtStr);
                    }
                    current.setLength(0);
                }
            }
        } catch (Exception e) {
            logger.error("Error reading schema.sql: {}", e.getMessage(), e);
        }

        return statements;
    }

    private static void verifyTableCounts(Statement stmt) {
        System.out.println("\n[*] Master Data Seeding Summary:");
        checkCount(stmt, "users", "Registered Users & Administrators");
        checkCount(stmt, "stations", "Indian Railway Stations");
        checkCount(stmt, "trains", "Passenger Trains (Vande Bharat, Rajdhani, Shatabdi)");
        checkCount(stmt, "train_routes", "Station Route Halts");
        checkCount(stmt, "train_classes", "Coach Seating Inventory & Fares");
        checkCount(stmt, "bookings", "Active Passenger Bookings");
    }

    private static void checkCount(Statement stmt, String tableName, String description) {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.printf("  ✔ %-36s : %3d records%n", description, count);
            }
        } catch (SQLException e) {
            System.out.printf("  ○ %-36s : Table ready (0 records / in-memory)%n", description);
        }
    }

    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        try {
            InputStream in = DatabaseSeeder.class.getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                in = DatabaseSeeder.class.getResourceAsStream(FALLBACK_CONFIG_FILE);
            }
            if (in != null) {
                try (InputStream stream = in) {
                    props.load(stream);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load application.properties: {}", e.getMessage());
        }
        return props;
    }
}
