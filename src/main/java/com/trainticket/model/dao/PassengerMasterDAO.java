package com.trainticket.model.dao;

import com.trainticket.model.PassengerMasterRecord;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object for Saved Passengers Master List.
 * Allows users to persist and quickly populate co-travelers during booking.
 */
public class PassengerMasterDAO {

    private static final Logger logger = LoggerFactory.getLogger(PassengerMasterDAO.class);
    private static final Map<Long, List<PassengerMasterRecord>> IN_MEMORY_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(100);

    static {
        // Seed default co-travelers for global/guest session (key 0L) and admin/default user (1L)
        List<PassengerMasterRecord> defaults = new ArrayList<>();
        defaults.add(new PassengerMasterRecord(1L, 0L, "Rishu Kumar", 26, "M", "LOWER"));
        defaults.add(new PassengerMasterRecord(2L, 0L, "Priya Sharma", 24, "F", "SIDE LOWER"));
        defaults.add(new PassengerMasterRecord(3L, 0L, "Vikram Malhotra", 58, "M", "LOWER"));
        defaults.add(new PassengerMasterRecord(4L, 0L, "Sunita Malhotra", 55, "F", "LOWER"));
        IN_MEMORY_STORE.put(0L, defaults);

        List<PassengerMasterRecord> user1List = new ArrayList<>(defaults);
        IN_MEMORY_STORE.put(1L, user1List);
    }

    public List<PassengerMasterRecord> getPassengersForUser(Long userId) {
        Long key = (userId != null) ? userId : 0L;
        if (DatabaseConnectionPool.isAvailable() && userId != null) {
            String sql = "SELECT id, user_id, full_name, age, gender, berth_preference FROM saved_passengers WHERE user_id = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<PassengerMasterRecord> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new PassengerMasterRecord(
                                rs.getLong("id"),
                                rs.getLong("user_id"),
                                rs.getString("full_name"),
                                rs.getInt("age"),
                                rs.getString("gender"),
                                rs.getString("berth_preference")
                        ));
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database read saved_passengers failed ({}), falling back to memory.", ex.getMessage());
            }
        }
        return new ArrayList<>(IN_MEMORY_STORE.getOrDefault(key, IN_MEMORY_STORE.get(0L)));
    }

    public synchronized PassengerMasterRecord savePassenger(Long userId, PassengerMasterRecord record) {
        Long key = (userId != null) ? userId : 0L;
        long recordId = ID_GENERATOR.incrementAndGet();
        PassengerMasterRecord saved = new PassengerMasterRecord(
                recordId,
                key,
                record.getFullName(),
                record.getAge(),
                record.getGender(),
                record.getBerthPreference()
        );

        IN_MEMORY_STORE.computeIfAbsent(key, k -> new ArrayList<>()).add(saved);

        if (DatabaseConnectionPool.isAvailable() && userId != null) {
            String sql = "INSERT INTO saved_passengers (user_id, full_name, age, gender, berth_preference) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, userId);
                stmt.setString(2, saved.getFullName());
                stmt.setInt(3, saved.getAge());
                stmt.setString(4, saved.getGender());
                stmt.setString(5, saved.getBerthPreference());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                logger.warn("Could not persist saved passenger to DB: {}", ex.getMessage());
            }
        }
        return saved;
    }

    public synchronized boolean deletePassenger(Long userId, Long recordId) {
        Long key = (userId != null) ? userId : 0L;
        List<PassengerMasterRecord> list = IN_MEMORY_STORE.get(key);
        if (list != null) {
            list.removeIf(p -> p.getId() != null && p.getId().equals(recordId));
        }

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "DELETE FROM saved_passengers WHERE id = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, recordId);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                logger.warn("Could not delete saved passenger from DB: {}", ex.getMessage());
            }
        }
        return true;
    }
}
