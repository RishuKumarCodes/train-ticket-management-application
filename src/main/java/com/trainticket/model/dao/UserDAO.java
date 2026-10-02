package com.trainticket.model.dao;

import com.trainticket.model.User;
import com.trainticket.model.UserRole;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object (DAO) for User persistence.
 * Uses pure JDBC with PreparedStatements and HikariCP connection pooling.
 * Includes a thread-safe in-memory cache fallback for frictionless local development.
 */
public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    // Fixed default Administrator cryptographic seed
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_SALT = "0123456789abcdef0123456789abcdef";
    public static final String DEFAULT_ADMIN_HASH = "463d121d09680f21241659d31b0389901d0479bcf389c231258d6c3f5c36050f";

    // In-memory fallback repository for dev/offline mode
    private static final Map<String, UserRecord> IN_MEMORY_USERS = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(100);

    static {
        // Seed default fixed administrator
        User admin = User.createAdmin(1L, DEFAULT_ADMIN_USERNAME, "admin@railflow.internal", "Station Master Admin");
        IN_MEMORY_USERS.put(DEFAULT_ADMIN_USERNAME.toLowerCase(), new UserRecord(admin, DEFAULT_ADMIN_HASH, DEFAULT_ADMIN_SALT));
        IN_MEMORY_USERS.put(admin.getEmail().toLowerCase(), new UserRecord(admin, DEFAULT_ADMIN_HASH, DEFAULT_ADMIN_SALT));
    }

    /**
     * Finds a user with their stored credentials by either username or email.
     *
     * @param identifier Username or email address
     * Finds a user with their stored credentials by either username, email, or mobile phone.
     *
     * @param identifier Username, email address, or phone number
     * @return Optional containing UserRecord if found, empty otherwise
     */
    public Optional<UserRecord> findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String cleaned = identifier.trim().toLowerCase();
        String rawTrimmed = identifier.trim();
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT id, username, email, phone, full_name, password_hash, salt, role, status, created_at " +
                         "FROM users WHERE LOWER(username) = ? OR LOWER(email) = ? OR phone = ? LIMIT 1";

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, cleaned);
                stmt.setString(2, cleaned);
                stmt.setString(3, rawTrimmed);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        User user = mapRowToUser(rs);
                        String hash = rs.getString("password_hash");
                        String salt = rs.getString("salt");
                        return Optional.of(new UserRecord(user, hash, salt));
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database query failed ({}), checking in-memory repository for identifier '{}'.", ex.getMessage(), cleaned);
            }
        }

        // Check in-memory fallback
        UserRecord rec = IN_MEMORY_USERS.get(cleaned);
        if (rec == null) {
            rec = IN_MEMORY_USERS.get(rawTrimmed);
        }
        if (rec == null) {
            rec = IN_MEMORY_USERS.values().stream()
                    .filter(r -> (r.user().getPhone() != null && r.user().getPhone().trim().equals(rawTrimmed))
                              || (r.user().getEmail() != null && r.user().getEmail().equalsIgnoreCase(cleaned))
                              || (r.user().getUsername() != null && r.user().getUsername().equalsIgnoreCase(cleaned)))
                    .findFirst()
                    .orElse(null);
        }
        return Optional.ofNullable(rec);
    }

    /**
     * Checks if a username is already taken.
     *
     * @param username Candidate username
     * @return true if taken, false otherwise
     */
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        String cleaned = username.trim().toLowerCase();

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT 1 FROM users WHERE LOWER(username) = ? LIMIT 1";

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, cleaned);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database query failed ({}), checking in-memory store for username '{}'.", ex.getMessage(), cleaned);
            }
        }

        return IN_MEMORY_USERS.containsKey(cleaned);
    }

    /**
     * Checks if an email is already registered.
     *
     * @param email Candidate email address
     * @return true if registered, false otherwise
     */
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String cleaned = email.trim().toLowerCase();

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT 1 FROM users WHERE LOWER(email) = ? LIMIT 1";

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, cleaned);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database query failed ({}), checking in-memory store for email '{}'.", ex.getMessage(), cleaned);
            }
        }

        return IN_MEMORY_USERS.values().stream()
                .anyMatch(rec -> rec.user().getEmail() != null && rec.user().getEmail().equalsIgnoreCase(cleaned));
    }

    /**
     * Checks if a mobile phone number is already registered.
     *
     * @param phone Candidate phone number
     * @return true if registered, false otherwise
     */
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        String cleaned = phone.trim();

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT 1 FROM users WHERE phone = ? LIMIT 1";

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, cleaned);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database query failed ({}), checking in-memory store for phone '{}'.", ex.getMessage(), cleaned);
            }
        }

        return IN_MEMORY_USERS.values().stream()
                .anyMatch(rec -> rec.user().getPhone() != null && rec.user().getPhone().trim().equals(cleaned));
    }

    /**
     * Persists a new user record into the database.
     *
     * @param user         Domain user entity
     * @param passwordHash Hex-encoded hashed password
     * @param salt         Hex-encoded salt
     * @return Persisted User entity with assigned ID
     * @throws SQLException if persistence fails
     */
    public User createUser(User user, String passwordHash, String salt) throws SQLException {
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "INSERT INTO users (username, email, phone, full_name, password_hash, salt, role, status, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getPhone());
                stmt.setString(4, user.getFullName());
                stmt.setString(5, passwordHash);
                stmt.setString(6, salt);
                stmt.setString(7, user.getRole().name());
                stmt.setString(8, user.getStatus());
                stmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                long generatedId;
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }

                User persisted = new User(
                        generatedId,
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getFullName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt()
                );

                // Also keep in-memory cache synchronized
                saveToMemoryFallback(persisted, passwordHash, salt);
                return persisted;

            } catch (SQLException ex) {
                logger.debug("Database insert failed ({}); persisting to in-memory fallback repository.", ex.getMessage());
            }
        }

        long newId = ID_GENERATOR.incrementAndGet();
        User fallbackUser = new User(
                newId,
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
        saveToMemoryFallback(fallbackUser, passwordHash, salt);
        return fallbackUser;
    }

    /**
     * Returns all registered users (excluding duplicate index keys), ordered by ID descending.
     * Queries the database first; falls back to the in-memory store.
     *
     * @return List of all User entities
     */
    public List<User> getAllUsers() {
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT id, username, email, phone, full_name, password_hash, salt, role, status, created_at " +
                         "FROM users ORDER BY id DESC";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
                return users;
            } catch (SQLException ex) {
                logger.debug("Database getAllUsers failed ({}), falling back to in-memory store.", ex.getMessage());
            }
        }

        // Deduplicate in-memory entries (same user stored under username, email, phone keys)
        Map<Long, User> deduped = new LinkedHashMap<>();
        for (UserRecord rec : IN_MEMORY_USERS.values()) {
            User u = rec.user();
            if (u.getId() != null) {
                deduped.put(u.getId(), u);
            }
        }
        List<User> result = new ArrayList<>(deduped.values());
        result.sort((a, b) -> Long.compare(
                b.getId() != null ? b.getId() : 0L,
                a.getId() != null ? a.getId() : 0L));
        return result;
    }

    /**
     * Updates a user's status in both the database and in-memory cache.
     *
     * @param userId    The user's numeric ID
     * @param newStatus Either "ACTIVE" or "BLOCKED"
     * @return true if at least one store was updated, false otherwise
     */
    public boolean updateUserStatus(Long userId, String newStatus) {
        if (userId == null || newStatus == null) return false;
        String cleanStatus = newStatus.trim().toUpperCase();
        boolean dbUpdated = false;

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "UPDATE users SET status = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cleanStatus);
                stmt.setLong(2, userId);
                int rows = stmt.executeUpdate();
                dbUpdated = rows > 0;
                logger.info("DB status update for userId={} to {} ({} rows)", userId, cleanStatus, rows);
            } catch (SQLException ex) {
                logger.debug("DB updateUserStatus failed ({}), applying to in-memory cache only.", ex.getMessage());
            }
        }

        // Sync in-memory cache
        boolean memUpdated = false;
        for (Map.Entry<String, UserRecord> entry : IN_MEMORY_USERS.entrySet()) {
            UserRecord rec = entry.getValue();
            if (userId.equals(rec.user().getId())) {
                User old = rec.user();
                User updated = new User(
                        old.getId(), old.getUsername(), old.getEmail(), old.getPhone(),
                        old.getFullName(), old.getRole(), cleanStatus, old.getCreatedAt());
                IN_MEMORY_USERS.put(entry.getKey(), new UserRecord(updated, rec.passwordHash(), rec.salt()));
                memUpdated = true;
            }
        }
        return dbUpdated || memUpdated;
    }

    private void saveToMemoryFallback(User user, String hash, String salt) {
        UserRecord record = new UserRecord(user, hash, salt);
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            IN_MEMORY_USERS.put(user.getUsername().toLowerCase(), record);
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            IN_MEMORY_USERS.put(user.getEmail().toLowerCase(), record);
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            IN_MEMORY_USERS.put(user.getPhone().trim(), record);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String fullName = rs.getString("full_name");
        String roleStr = rs.getString("role");
        UserRole role = UserRole.PASSENGER;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception ignored) {
        }
        String status = rs.getString("status");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

        return new User(id, username, email, phone, fullName, role, status, createdAt);
    }
}
