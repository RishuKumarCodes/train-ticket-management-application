package com.trainticket.util.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * High-performance database connection pool backed by HikariCP.
 * Reads database credentials and pooling configuration from application.properties.
 */
public final class DatabaseConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPool.class);
    private static final String CONFIG_FILE = "/application.properties";
    private static final String FALLBACK_CONFIG_FILE = "/application.properties.example";

    private static HikariDataSource dataSource;

    static {
        initDataSource();
    }

    private DatabaseConnectionPool() {
        // Utility class
    }

    private static void initDataSource() {
        try {
            Properties props = new Properties();

            // Attempt to load application.properties; fall back to example if not created yet
            InputStream in = DatabaseConnectionPool.class.getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                logger.warn("application.properties not found. Falling back to application.properties.example");
                in = DatabaseConnectionPool.class.getResourceAsStream(FALLBACK_CONFIG_FILE);
            }

            if (in != null) {
                props.load(in);
                in.close();
            } else {
                throw new IllegalStateException("Neither application.properties nor application.properties.example could be found on classpath!");
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url", "jdbc:mysql://localhost:3306/train_ticket_db?useSSL=false"));
            config.setUsername(props.getProperty("db.username", "root"));
            config.setPassword(props.getProperty("db.password", ""));
            config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));

            // Pool tuning
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximum-size", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimum-idle", "2")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idle-timeout", "30000")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connection-timeout", "30000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.max-lifetime", "1800000")));

            // Recommended performance optimizations for MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP Database Connection Pool initialized successfully.");

        } catch (Exception e) {
            logger.error("Failed to initialize HikariCP connection pool: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves an active database connection from the connection pool.
     * Always use with try-with-resources.
     *
     * @return Connection an active SQL connection
     * @throws SQLException if a connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariCP DataSource is not initialized. Please verify your database configuration.");
        }
        return dataSource.getConnection();
    }

    /**
     * Tests whether a database connection can be established successfully.
     *
     * @return true if connected successfully, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                logger.info("Database connection test succeeded!");
            }
            return valid;
        } catch (SQLException e) {
            logger.warn("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Closes the connection pool upon application shutdown.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool shut down.");
        }
    }
}
