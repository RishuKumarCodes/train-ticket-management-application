package com.trainticket.model.dao;

import com.trainticket.model.Station;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Station nodes.
 * Executes PreparedStatements against HikariCP connection pool with an
 * in-memory dataset fallback for reliable offline operation.
 */
public class StationDAO {

    private static final Logger logger = LoggerFactory.getLogger(StationDAO.class);

    // In-memory fallback repository of major Indian Railway stations
    private static final Map<String, Station> IN_MEMORY_STATIONS = new ConcurrentHashMap<>();

    static {
        registerStation(new Station(1L, "NDLS", "New Delhi", "New Delhi", "Delhi"));
        registerStation(new Station(2L, "MMCT", "Mumbai Central", "Mumbai", "Maharashtra"));
        registerStation(new Station(3L, "CSMT", "Chhatrapati Shivaji Maharaj Terminus", "Mumbai", "Maharashtra"));
        registerStation(new Station(4L, "HWH", "Howrah Junction", "Kolkata", "West Bengal"));
        registerStation(new Station(5L, "BSB", "Varanasi Junction", "Varanasi", "Uttar Pradesh"));
        registerStation(new Station(6L, "CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh"));
        registerStation(new Station(7L, "PRYJ", "Prayagraj Junction", "Prayagraj", "Uttar Pradesh"));
        registerStation(new Station(8L, "DDU", "Pt. Deen Dayal Upadhyaya Junction", "Mughalsarai", "Uttar Pradesh"));
        registerStation(new Station(9L, "KOTA", "Kota Junction", "Kota", "Rajasthan"));
        registerStation(new Station(10L, "BRC", "Vadodara Junction", "Vadodara", "Gujarat"));
        registerStation(new Station(11L, "ST", "Surat", "Surat", "Gujarat"));
        registerStation(new Station(12L, "LKO", "Lucknow Charbagh", "Lucknow", "Uttar Pradesh"));
        registerStation(new Station(13L, "GZB", "Ghaziabad Junction", "Ghaziabad", "Uttar Pradesh"));
        registerStation(new Station(14L, "ALJN", "Aligarh Junction", "Aligarh", "Uttar Pradesh"));
        registerStation(new Station(15L, "MAS", "MGR Chennai Central", "Chennai", "Tamil Nadu"));
        registerStation(new Station(16L, "SBC", "KSR Bengaluru City", "Bengaluru", "Karnataka"));
        registerStation(new Station(17L, "MYS", "Mysuru Junction", "Mysuru", "Karnataka"));
        registerStation(new Station(18L, "KPD", "Katpadi Junction", "Vellore", "Tamil Nadu"));
        registerStation(new Station(19L, "JP", "Jaipur Junction", "Jaipur", "Rajasthan"));
        registerStation(new Station(20L, "ADI", "Ahmedabad Junction", "Ahmedabad", "Gujarat"));
        registerStation(new Station(21L, "PUNE", "Pune Junction", "Pune", "Maharashtra"));
        registerStation(new Station(22L, "HYB", "Hyderabad Deccan", "Hyderabad", "Telangana"));
        registerStation(new Station(23L, "SC", "Secunderabad Junction", "Hyderabad", "Telangana"));
        registerStation(new Station(24L, "BPL", "Bhopal Junction", "Bhopal", "Madhya Pradesh"));
        registerStation(new Station(25L, "CDG", "Chandigarh Junction", "Chandigarh", "Chandigarh"));
        registerStation(new Station(26L, "ASR", "Amritsar Junction", "Amritsar", "Punjab"));
        registerStation(new Station(27L, "PNBE", "Patna Junction", "Patna", "Bihar"));
        registerStation(new Station(28L, "GHY", "Guwahati", "Guwahati", "Assam"));
        registerStation(new Station(29L, "BBS", "Bhubaneswar", "Bhubaneswar", "Odisha"));
        registerStation(new Station(30L, "TVC", "Thiruvananthapuram Central", "Thiruvananthapuram", "Kerala"));
        registerStation(new Station(31L, "ERS", "Ernakulam Junction", "Kochi", "Kerala"));
        registerStation(new Station(32L, "MAO", "Madgaon Junction", "Goa", "Goa"));
        registerStation(new Station(33L, "AGC", "Agra Cantt", "Agra", "Uttar Pradesh"));
        registerStation(new Station(34L, "GWL", "Gwalior Junction", "Gwalior", "Madhya Pradesh"));
        registerStation(new Station(35L, "JAT", "Jammu Tawi", "Jammu", "Jammu & Kashmir"));
        registerStation(new Station(36L, "DDN", "Dehradun", "Dehradun", "Uttarakhand"));
        registerStation(new Station(37L, "HW", "Haridwar Junction", "Haridwar", "Uttarakhand"));
        registerStation(new Station(38L, "GKP", "Gorakhpur Junction", "Gorakhpur", "Uttar Pradesh"));
        registerStation(new Station(39L, "NGP", "Nagpur Junction", "Nagpur", "Maharashtra"));
        registerStation(new Station(40L, "VSKP", "Visakhapatnam Junction", "Visakhapatnam", "Andhra Pradesh"));
        registerStation(new Station(41L, "INDB", "Indore Junction", "Indore", "Madhya Pradesh"));
        registerStation(new Station(42L, "JBP", "Jabalpur Junction", "Jabalpur", "Madhya Pradesh"));
        registerStation(new Station(43L, "R", "Raipur Junction", "Raipur", "Chhattisgarh"));
        registerStation(new Station(44L, "RNC", "Ranchi Junction", "Ranchi", "Jharkhand"));
        registerStation(new Station(45L, "BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh"));
        registerStation(new Station(46L, "CBE", "Coimbatore Junction", "Coimbatore", "Tamil Nadu"));
        registerStation(new Station(47L, "MDU", "Madurai Junction", "Madurai", "Tamil Nadu"));
        registerStation(new Station(48L, "NZM", "Hazrat Nizamuddin", "New Delhi", "Delhi"));
        registerStation(new Station(49L, "VGLJ", "Virangana Lakshmibai Jhansi", "Jhansi", "Uttar Pradesh"));
        registerStation(new Station(50L, "PURI", "Puri", "Puri", "Odisha"));
    }

    private static void registerStation(Station s) {
        IN_MEMORY_STATIONS.put(s.getCode().toUpperCase(), s);
    }

    /**
     * Retrieves all stations known to the network.
     */
    public List<Station> getAllStations() {
        if (!DatabaseConnectionPool.isAvailable()) {
            List<Station> list = new ArrayList<>(IN_MEMORY_STATIONS.values());
            list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            return list;
        }

        String sql = "SELECT id, code, name, city, state FROM stations ORDER BY name ASC";
        try (Connection conn = DatabaseConnectionPool.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            List<Station> stations = new ArrayList<>();
            while (rs.next()) {
                stations.add(mapResultSet(rs));
            }
            if (!stations.isEmpty()) {
                return stations;
            }
        } catch (SQLException ex) {
            logger.debug("Database query for stations failed ({}), using in-memory store.", ex.getMessage());
        }

        List<Station> list = new ArrayList<>(IN_MEMORY_STATIONS.values());
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return list;
    }

    /**
     * Looks up a station by exact 3-5 letter station code (case-insensitive).
     */
    public Optional<Station> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String cleanCode = code.trim().toUpperCase();

        if (!DatabaseConnectionPool.isAvailable()) {
            return Optional.ofNullable(IN_MEMORY_STATIONS.get(cleanCode));
        }

        String sql = "SELECT id, code, name, city, state FROM stations WHERE code = ?";
        try (Connection conn = DatabaseConnectionPool.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cleanCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException ex) {
            logger.debug("Database station lookup failed ({}), checking in-memory store for code '{}'.",
                    ex.getMessage(), cleanCode);
        }

        return Optional.ofNullable(IN_MEMORY_STATIONS.get(cleanCode));
    }

    /**
     * Search stations matching code, station name, or city.
     */
    public List<Station> searchStations(String query) {
        if (query == null || query.isBlank()) {
            return getAllStations();
        }
        String clean = query.trim().toUpperCase();

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "SELECT id, code, name, city, state FROM stations " +
                    "WHERE UPPER(code) LIKE ? OR UPPER(name) LIKE ? OR UPPER(city) LIKE ? " +
                    "ORDER BY CASE WHEN UPPER(code) = ? THEN 1 WHEN UPPER(code) LIKE ? THEN 2 ELSE 3 END, name ASC";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                String wild = "%" + clean + "%";
                stmt.setString(1, wild);
                stmt.setString(2, wild);
                stmt.setString(3, wild);
                stmt.setString(4, clean);
                stmt.setString(5, clean + "%");

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Station> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapResultSet(rs));
                    }
                    if (!results.isEmpty()) {
                        return results;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database station search failed ({}), filtering in-memory store.", ex.getMessage());
            }
        }

        List<Station> matches = new ArrayList<>();
        for (Station s : IN_MEMORY_STATIONS.values()) {
            if (s.getCode().toUpperCase().contains(clean) ||
                    s.getName().toUpperCase().contains(clean) ||
                    s.getCity().toUpperCase().contains(clean)) {
                matches.add(s);
            }
        }
        matches.sort((a, b) -> {
            boolean aExact = a.getCode().equalsIgnoreCase(clean);
            boolean bExact = b.getCode().equalsIgnoreCase(clean);
            if (aExact != bExact)
                return aExact ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return matches;
    }

    private Station mapResultSet(ResultSet rs) throws SQLException {
        return new Station(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("state"));
    }
}
