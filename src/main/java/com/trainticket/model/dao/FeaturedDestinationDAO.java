package com.trainticket.model.dao;

import com.trainticket.model.FeaturedDestination;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object for Featured Destinations.
 * Handles persistence to MySQL with HikariCP connection pooling,
 * paired with an in-memory development fallback and listener notification pipeline.
 */
public class FeaturedDestinationDAO {

    private static final Logger logger = LoggerFactory.getLogger(FeaturedDestinationDAO.class);
    private static final FeaturedDestinationDAO INSTANCE = new FeaturedDestinationDAO();

    private final List<FeaturedDestination> inMemoryDestinations = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(100L);

    public static FeaturedDestinationDAO getInstance() {
        return INSTANCE;
    }

    public FeaturedDestinationDAO() {
        initDefaultDestinations();
    }

    private void initDefaultDestinations() {
        inMemoryDestinations.add(new FeaturedDestination(1L, "DAL\nLAKE", "Srinagar, Jammu & Kashmir", "JAT", "/assets/images/dal-lake.png", "staircase"));
        inMemoryDestinations.add(new FeaturedDestination(2L, "TAJ\nMAHAL", "Agra, Uttar Pradesh", "AGC", "/assets/images/taj-mahal.png", "staircase"));
        inMemoryDestinations.add(new FeaturedDestination(3L, "MUNNAR", "Kerala, India", "ERS", "/assets/images/Munnar, Kerala.png", ""));
        inMemoryDestinations.add(new FeaturedDestination(4L, "PANGONG\nTSO", "Leh, Ladakh", "JAT", "/assets/images/lake-in-ladakh.png", "staircase:0.55"));
        inMemoryDestinations.add(new FeaturedDestination(5L, "HAWA\nMAHAL", "Jaipur, Rajasthan", "JP", "/assets/images/hawa-mahal.png", "left"));
        inMemoryDestinations.add(new FeaturedDestination(6L, "VARANASI\nGHATS", "Varanasi, Uttar Pradesh", "BSB", "/assets/images/Varanasi ghat.png", "right"));
        inMemoryDestinations.add(new FeaturedDestination(7L, "LAKE\nPALACE", "Udaipur, Rajasthan", "JP", "/assets/images/Lake Palace (Jag Niwas).png", ""));
        inMemoryDestinations.add(new FeaturedDestination(8L, "ALLEPPEY", "Kerala, India", "ERS", "/assets/images/Alleppey,kerela.png", ""));
        inMemoryDestinations.add(new FeaturedDestination(9L, "STATUE\nOF\nUNITY", "Kevadia, Gujarat", "BRC", "/assets/images/statue-of-unity.png", "right"));
    }

    /**
     * Retrieves all active featured destinations.
     */
    public List<FeaturedDestination> getAllDestinations() {
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT id, monument_name, location_text, station_code, image_path, layout_style FROM featured_destinations ORDER BY id ASC";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<FeaturedDestination> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new FeaturedDestination(
                            rs.getLong("id"),
                            rs.getString("monument_name"),
                            rs.getString("location_text"),
                            rs.getString("station_code"),
                            rs.getString("image_path"),
                            rs.getString("layout_style")
                    ));
                }
                if (!results.isEmpty()) {
                    return Collections.unmodifiableList(results);
                }
            } catch (SQLException e) {
                logger.warn("Database query for featured destinations failed, using in-memory store: {}", e.getMessage());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(inMemoryDestinations));
    }

    /**
     * Adds a new featured destination to the system.
     */
    public FeaturedDestination addDestination(FeaturedDestination dest) {
        FeaturedDestination created;
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "INSERT INTO featured_destinations (monument_name, location_text, station_code, image_path, layout_style) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, dest.getMonumentName());
                stmt.setString(2, dest.getLocationText());
                stmt.setString(3, dest.getStationCode());
                stmt.setString(4, dest.getImagePath());
                stmt.setString(5, dest.getLayoutStyle());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        long generatedId = keys.getLong(1);
                        created = new FeaturedDestination(generatedId, dest.getMonumentName(), dest.getLocationText(),
                                dest.getStationCode(), dest.getImagePath(), dest.getLayoutStyle());
                    } else {
                        created = dest;
                    }
                }
            } catch (SQLException e) {
                logger.warn("Database insert for featured destination failed, falling back to memory: {}", e.getMessage());
                created = new FeaturedDestination(nextId.incrementAndGet(), dest.getMonumentName(), dest.getLocationText(),
                        dest.getStationCode(), dest.getImagePath(), dest.getLayoutStyle());
            }
        } else {
            created = new FeaturedDestination(nextId.incrementAndGet(), dest.getMonumentName(), dest.getLocationText(),
                    dest.getStationCode(), dest.getImagePath(), dest.getLayoutStyle());
        }

        inMemoryDestinations.add(created);
        notifyChangeListeners();
        logger.info("Added featured destination: {} -> {}", created.getSingleLineName(), created.getStationCode());
        return created;
    }

    /**
     * Removes a featured destination by ID.
     */
    public boolean deleteDestination(Long id) {
        if (id == null) return false;

        boolean removedDb = false;
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "DELETE FROM featured_destinations WHERE id = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                int rows = stmt.executeUpdate();
                removedDb = rows > 0;
            } catch (SQLException e) {
                logger.warn("Database deletion for destination {} failed: {}", id, e.getMessage());
            }
        }

        boolean removedMemory = inMemoryDestinations.removeIf(d -> id.equals(d.getId()));
        boolean success = removedDb || removedMemory;
        if (success) {
            notifyChangeListeners();
            logger.info("Removed featured destination id: {}", id);
        }
        return success;
    }

    /**
     * Updates an existing featured destination.
     */
    public boolean updateDestination(FeaturedDestination dest) {
        if (dest == null || dest.getId() == null) return false;

        boolean updatedDb = false;
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "UPDATE featured_destinations SET monument_name = ?, location_text = ?, station_code = ?, image_path = ?, layout_style = ? WHERE id = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dest.getMonumentName());
                stmt.setString(2, dest.getLocationText());
                stmt.setString(3, dest.getStationCode());
                stmt.setString(4, dest.getImagePath());
                stmt.setString(5, dest.getLayoutStyle());
                stmt.setLong(6, dest.getId());
                int rows = stmt.executeUpdate();
                updatedDb = rows > 0;
            } catch (SQLException e) {
                logger.warn("Database update for destination {} failed: {}", dest.getId(), e.getMessage());
            }
        }

        boolean updatedMemory = false;
        for (int i = 0; i < inMemoryDestinations.size(); i++) {
            if (inMemoryDestinations.get(i).getId().equals(dest.getId())) {
                inMemoryDestinations.set(i, dest);
                updatedMemory = true;
                break;
            }
        }

        boolean success = updatedDb || updatedMemory;
        if (success) {
            notifyChangeListeners();
            logger.info("Updated featured destination id: {}", dest.getId());
        }
        return success;
    }

    /**
     * Registers a listener to be invoked whenever destinations are added or removed.
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered change listener.
     */
    public void removeChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    private void notifyChangeListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                logger.error("Error in destination change listener: {}", e.getMessage(), e);
            }
        }
    }
}
