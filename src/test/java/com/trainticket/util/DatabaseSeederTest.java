package com.trainticket.util;

import com.trainticket.util.db.DatabaseConnectionPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying database schema script integrity, statement parsing, and connection pool properties.
 */
public class DatabaseSeederTest {

    private static final String SCHEMA_PATH = "/db/schema.sql";

    @Test
    @DisplayName("schema.sql exists on classpath and parses cleanly into executable SQL commands")
    void testSchemaSqlParsing() throws Exception {
        InputStream in = getClass().getResourceAsStream(SCHEMA_PATH);
        assertNotNull(in, "schema.sql should be present in resources /db/schema.sql");

        List<String> statements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder current = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }
                current.append(line).append(" ");
                if (line.endsWith(";")) {
                    String stmt = current.toString().trim();
                    if (stmt.endsWith(";")) {
                        stmt = stmt.substring(0, stmt.length() - 1).trim();
                    }
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current.setLength(0);
                }
            }
        }

        assertFalse(statements.isEmpty(), "schema.sql should produce multiple executable SQL statements");
        assertTrue(statements.size() >= 15, "schema.sql should contain at least 15 statements (tables + seed data)");

        // Verify key tables are defined in schema
        boolean hasStationsTable = statements.stream().anyMatch(s -> s.toUpperCase().contains("CREATE TABLE IF NOT EXISTS STATIONS"));
        boolean hasTrainsTable = statements.stream().anyMatch(s -> s.toUpperCase().contains("CREATE TABLE IF NOT EXISTS TRAINS"));
        boolean hasBookingsTable = statements.stream().anyMatch(s -> s.toUpperCase().contains("CREATE TABLE IF NOT EXISTS BOOKINGS"));
        boolean hasUsersTable = statements.stream().anyMatch(s -> s.toUpperCase().contains("CREATE TABLE IF NOT EXISTS USERS"));

        assertTrue(hasStationsTable, "Schema must define stations table");
        assertTrue(hasTrainsTable, "Schema must define trains table");
        assertTrue(hasBookingsTable, "Schema must define bookings table");
        assertTrue(hasUsersTable, "Schema must define users table");

        // Verify iconic trains seeded
        boolean hasRajdhani = statements.stream().anyMatch(s -> s.contains("12952"));
        boolean hasVandeBharat = statements.stream().anyMatch(s -> s.contains("22436"));
        assertTrue(hasRajdhani, "Schema should seed Tejas Rajdhani Express (12952)");
        assertTrue(hasVandeBharat, "Schema should seed Vande Bharat Express (22436)");
    }

    @Test
    @DisplayName("DatabaseConnectionPool reports availability state without crashing")
    void testConnectionPoolAvailability() {
        // Under standalone unit testing without local MySQL, isAvailable/testConnection should return false safely
        boolean available = DatabaseConnectionPool.isAvailable();
        // Just verify it completes execution cleanly without uncaught exceptions
        assertNotNull(Boolean.valueOf(available));
    }

    @Test
    @DisplayName("application.properties.example template exists on classpath with expected keys")
    void testApplicationPropertiesExampleExists() throws Exception {
        InputStream in = getClass().getResourceAsStream("/application.properties.example");
        assertNotNull(in, "application.properties.example should be present in resources");

        java.util.Properties props = new java.util.Properties();
        props.load(in);
        assertTrue(props.containsKey("db.url"), "Properties template must define db.url");
        assertTrue(props.containsKey("db.username"), "Properties template must define db.username");
    }
}
