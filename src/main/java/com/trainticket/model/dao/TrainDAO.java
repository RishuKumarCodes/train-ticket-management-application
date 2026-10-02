package com.trainticket.model.dao;

import com.trainticket.model.CoachAvailability;
import com.trainticket.model.ConcessionType;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.TrainStatus;
import com.trainticket.model.TrainType;
import com.trainticket.model.TravelQuota;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Trains, Routes, and Coach Inventory.
 * Provides route matching across intermediate halts, schedule verification,
 * and fare calculation with HikariCP JDBC and in-memory development store.
 */
public class TrainDAO {

    private static final Logger logger = LoggerFactory.getLogger(TrainDAO.class);
    private static final Map<String, Train> IN_MEMORY_TRAINS = new ConcurrentHashMap<>();
    private static final TrainDAO INSTANCE = new TrainDAO();

    private final StationDAO stationDAO;

    public static TrainDAO getInstance() {
        return INSTANCE;
    }

    public TrainDAO() {
        this.stationDAO = new StationDAO();
        ensureSeedData();
    }

    /**
     * Executes multi-criteria search finding trains running between two stations.
     */
    public List<TrainSearchResult> searchTrains(TrainSearchQuery query) {
        String fromCode = query.getFromStationCode().toUpperCase();
        String toCode = query.getToStationCode().toUpperCase();
        DayOfWeek searchDay = query.getJourneyDate().getDayOfWeek();

        // 1. Attempt JDBC Route Search
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = """
                SELECT t.id, t.train_number, t.name, t.type, t.runs_on, t.status,
                       r1.stop_sequence AS dep_seq, r1.arrival_time AS dep_arr, r1.departure_time AS dep_time,
                       r1.halt_minutes AS dep_halt, r1.distance_km AS dep_dist, r1.day_count AS dep_day,
                       r2.stop_sequence AS arr_seq, r2.arrival_time AS arr_time, r2.departure_time AS arr_dep,
                       r2.halt_minutes AS arr_halt, r2.distance_km AS arr_dist, r2.day_count AS arr_day
                FROM trains t
                JOIN train_routes r1 ON t.id = r1.train_id
                JOIN stations s1 ON r1.station_id = s1.id
                JOIN train_routes r2 ON t.id = r2.train_id
                JOIN stations s2 ON r2.station_id = s2.id
                WHERE s1.code = ? AND s2.code = ? AND r1.stop_sequence < r2.stop_sequence
                ORDER BY r1.departure_time ASC
            """;

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, fromCode);
                stmt.setString(2, toCode);

                try (ResultSet rs = stmt.executeQuery()) {
                    List<TrainSearchResult> results = new ArrayList<>();
                    while (rs.next()) {
                        String runsOn = rs.getString("runs_on");
                        int dayIdx = searchDay.getValue() - 1;
                        if (runsOn != null && runsOn.length() == 7 && runsOn.charAt(dayIdx) != '1') {
                            continue; // Train does not run on this day of week
                        }

                        String trainNumber = rs.getString("train_number");
                        Optional<Train> fullTrain = getTrainByNumber(trainNumber);
                        if (fullTrain.isPresent()) {
                            Train t = fullTrain.get();
                            RouteHalt originHalt = findHaltByStationCode(t.getRouteHalts(), fromCode);
                            RouteHalt destHalt = findHaltByStationCode(t.getRouteHalts(), toCode);
                            if (originHalt != null && destHalt != null) {
                                List<CoachAvailability> adjustedClasses = 
                                        evaluateCoachAvailability(t.getCoachClasses(), query.getQuota(), query.getConcession());
                                results.add(new TrainSearchResult(t, originHalt, destHalt, adjustedClasses));
                            }
                        }
                    }
                    if (!results.isEmpty()) {
                        return results;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database train search failed ({}), querying in-memory repository.", ex.getMessage());
            }
        }

        // 2. In-Memory Route Evaluation Fallback
        List<TrainSearchResult> fallbackResults = new ArrayList<>();
        for (Train train : IN_MEMORY_TRAINS.values()) {
            if (!train.runsOn(searchDay)) {
                continue;
            }

            RouteHalt originHalt = findHaltByStationCode(train.getRouteHalts(), fromCode);
            RouteHalt destHalt = findHaltByStationCode(train.getRouteHalts(), toCode);

            if (originHalt != null && destHalt != null && originHalt.getStopSequence() < destHalt.getStopSequence()) {
                List<CoachAvailability> adjustedClasses = 
                        evaluateCoachAvailability(train.getCoachClasses(), query.getQuota(), query.getConcession());
                
                // If a preferred class was requested (and not 'All Classes'), filter accordingly
                if (!"All Classes".equalsIgnoreCase(query.getPreferredClass())) {
                    adjustedClasses = adjustedClasses.stream()
                            .filter(c -> c.getClassCode().equalsIgnoreCase(query.getPreferredClass()))
                            .toList();
                }

                fallbackResults.add(new TrainSearchResult(train, originHalt, destHalt, adjustedClasses));
            }
        }

        fallbackResults.sort(Comparator.comparing(TrainSearchResult::getDepartureTime, 
                Comparator.nullsLast(Comparator.naturalOrder())));
        return fallbackResults;
    }

    /**
     * Checks if at least one direct train route connects the given origin and destination station codes.
     *
     * @param fromStationCode origin station code (e.g. "NDLS")
     * @param toStationCode   destination station code (e.g. "AGC")
     * @return true if at least one direct train route exists
     */
    public boolean hasRoute(String fromStationCode, String toStationCode) {
        if (fromStationCode == null || toStationCode == null) {
            return false;
        }
        String fromCode = fromStationCode.trim().toUpperCase();
        String toCode = toStationCode.trim().toUpperCase();
        if (fromCode.isEmpty() || toCode.isEmpty() || fromCode.equals(toCode)) {
            return false;
        }

        // 1. Check JDBC if available
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = """
                SELECT 1
                FROM train_routes r1
                JOIN stations s1 ON r1.station_id = s1.id
                JOIN train_routes r2 ON r1.train_id = r2.train_id
                JOIN stations s2 ON r2.station_id = s2.id
                WHERE s1.code = ? AND s2.code = ? AND r1.stop_sequence < r2.stop_sequence
                LIMIT 1
            """;
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fromCode);
                stmt.setString(2, toCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database hasRoute query failed ({}), checking in-memory fallback.", ex.getMessage());
            }
        }

        // 2. In-memory fallback
        for (Train train : IN_MEMORY_TRAINS.values()) {
            RouteHalt originHalt = findHaltByStationCode(train.getRouteHalts(), fromCode);
            RouteHalt destHalt = findHaltByStationCode(train.getRouteHalts(), toCode);
            if (originHalt != null && destHalt != null && originHalt.getStopSequence() < destHalt.getStopSequence()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if at least one train route connects the stations on the given journey date.
     */
    public boolean hasRouteOnDate(String fromStationCode, String toStationCode, LocalDate journeyDate) {
        if (fromStationCode == null || toStationCode == null || journeyDate == null) {
            return false;
        }
        String fromCode = fromStationCode.trim().toUpperCase();
        String toCode = toStationCode.trim().toUpperCase();
        if (fromCode.isEmpty() || toCode.isEmpty() || fromCode.equals(toCode)) {
            return false;
        }
        DayOfWeek searchDay = journeyDate.getDayOfWeek();

        // 1. Check JDBC if available
        if (DatabaseConnectionPool.isAvailable()) {
            String sql = """
                SELECT t.runs_on
                FROM trains t
                JOIN train_routes r1 ON t.id = r1.train_id
                JOIN stations s1 ON r1.station_id = s1.id
                JOIN train_routes r2 ON t.id = r2.train_id
                JOIN stations s2 ON r2.station_id = s2.id
                WHERE s1.code = ? AND s2.code = ? AND r1.stop_sequence < r2.stop_sequence
            """;
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fromCode);
                stmt.setString(2, toCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String runsOn = rs.getString("runs_on");
                        int dayIdx = searchDay.getValue() - 1;
                        if (runsOn == null || runsOn.length() < 7 || runsOn.charAt(dayIdx) == '1') {
                            return true;
                        }
                    }
                }
            } catch (SQLException ex) {
                logger.debug("Database hasRouteOnDate query failed ({}), checking in-memory fallback.", ex.getMessage());
            }
        }

        // 2. In-memory fallback
        for (Train train : IN_MEMORY_TRAINS.values()) {
            if (!train.runsOn(searchDay)) {
                continue;
            }
            RouteHalt originHalt = findHaltByStationCode(train.getRouteHalts(), fromCode);
            RouteHalt destHalt = findHaltByStationCode(train.getRouteHalts(), toCode);
            if (originHalt != null && destHalt != null && originHalt.getStopSequence() < destHalt.getStopSequence()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves full train details including all route halts and seat classes by train number.
     */
    public Optional<Train> getTrainByNumber(String trainNumber) {
        if (trainNumber == null) return Optional.empty();
        String cleanNum = trainNumber.trim();
        return Optional.ofNullable(IN_MEMORY_TRAINS.get(cleanNum));
    }

    /**
     * Searches trains by train number (exact or prefix) or train name (case-insensitive substring).
     * Returns matching Train entities sorted with exact number matches first.
     */
    public List<Train> searchByTrainNumberOrName(String query) {
        if (query == null || query.isBlank()) {
            return getAllTrains();
        }
        String clean = query.trim().toLowerCase();
        List<Train> exactNumberMatches = new ArrayList<>();
        List<Train> nameOrPrefixMatches = new ArrayList<>();

        for (Train train : IN_MEMORY_TRAINS.values()) {
            String tNum = train.getTrainNumber().toLowerCase();
            String tName = train.getName().toLowerCase();

            if (tNum.equals(clean)) {
                exactNumberMatches.add(train);
            } else if (tNum.contains(clean) || tName.contains(clean)) {
                nameOrPrefixMatches.add(train);
            }
        }

        exactNumberMatches.sort(Comparator.comparing(Train::getTrainNumber));
        nameOrPrefixMatches.sort(Comparator.comparing(Train::getName));

        List<Train> results = new ArrayList<>(exactNumberMatches);
        results.addAll(nameOrPrefixMatches);
        return results;
    }

    /**
     * Creates a TrainSearchResult representing the full journey of this train from origin to terminus.
     */
    public TrainSearchResult createFullRouteResult(Train train) {
        if (train == null || train.getRouteHalts().isEmpty()) {
            return null;
        }
        List<RouteHalt> halts = train.getRouteHalts();
        RouteHalt originHalt = halts.get(0);
        RouteHalt destHalt = halts.get(halts.size() - 1);
        List<CoachAvailability> coachClasses = train.getCoachClasses();
        return new TrainSearchResult(train, originHalt, destHalt, coachClasses);
    }

    /**
     * Retrieves ordered station-by-station route halts for live tracking.
     */
    public List<RouteHalt> getTrainRoute(String trainNumber) {
        return getTrainByNumber(trainNumber)
                .map(Train::getRouteHalts)
                .orElse(Collections.emptyList());
    }

    private RouteHalt findHaltByStationCode(List<RouteHalt> halts, String stationCode) {
        for (RouteHalt h : halts) {
            if (h.getStation().getCode().equalsIgnoreCase(stationCode)) {
                return h;
            }
        }
        return null;
    }

    private List<CoachAvailability> evaluateCoachAvailability(List<CoachAvailability> baseList, 
                                                              TravelQuota quota, 
                                                              ConcessionType concession) {
        List<CoachAvailability> evaluated = new ArrayList<>();
        for (CoachAvailability base : baseList) {
            double multiplier = quota.getFareMultiplier();
            double adjustedBaseFare = Math.round(base.getBaseFare() * multiplier);
            double finalFare = concession.applyConcession(adjustedBaseFare);

            int avail = base.getAvailableSeats();
            int rac = base.getRacSeats();
            int wl = base.getWaitlistSeats();

            if (quota == TravelQuota.ALL_AC) {
                String cc = base.getClassCode().toUpperCase();
                if (cc.equals("SL") || cc.equals("2S") || cc.equals("GEN") || cc.equals("UR")) {
                    continue;
                }
            }

            if (quota == TravelQuota.TATKAL || quota == TravelQuota.PREMIUM_TATKAL) {
                // Tatkal has dedicated quota pool
                avail = Math.max(0, Math.min(avail, (int) Math.round(base.getTotalSeats() * 0.25)));
            }

            evaluated.add(new CoachAvailability(
                    base.getClassCode(),
                    quota.getCode(),
                    base.getTotalSeats(),
                    avail,
                    rac,
                    wl,
                    adjustedBaseFare,
                    finalFare
            ));
        }
        return evaluated;
    }

    public List<Train> getAllTrains() {
        return new ArrayList<>(IN_MEMORY_TRAINS.values());
    }

    public Optional<Train> findByTrainNumber(String trainNumber) {
        if (trainNumber == null) return Optional.empty();
        return Optional.ofNullable(IN_MEMORY_TRAINS.get(trainNumber.trim()));
    }

    public synchronized void addTrain(Train train) {
        if (train == null) return;
        IN_MEMORY_TRAINS.put(train.getTrainNumber(), train);

        if (DatabaseConnectionPool.isAvailable()) {
            String insertTrain = "INSERT INTO trains (train_number, name, type, source_station_id, dest_station_id, runs_on, status, delay_minutes) " +
                    "VALUES (?, ?, ?, (SELECT id FROM stations WHERE code = ? LIMIT 1), (SELECT id FROM stations WHERE code = ? LIMIT 1), ?, ?, ?)";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertTrain)) {
                stmt.setString(1, train.getTrainNumber());
                stmt.setString(2, train.getName());
                stmt.setString(3, train.getType() != null ? train.getType().name() : "EXPRESS");
                stmt.setString(4, train.getSourceStation().getCode());
                stmt.setString(5, train.getDestStation().getCode());
                stmt.setString(6, train.getRunsOnDays());
                stmt.setString(7, train.getStatus() != null ? train.getStatus().name() : "ON_TIME");
                stmt.setInt(8, train.getDelayMinutes());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                logger.warn("Could not insert train into database: {}", ex.getMessage());
            }
        }
        logger.info("Added train {} ({}) to fleet", train.getTrainNumber(), train.getName());
    }

    public synchronized boolean updateTrainStatus(String trainNumber, TrainStatus status, int delayMinutes) {
        if (trainNumber == null) return false;
        Train existing = IN_MEMORY_TRAINS.get(trainNumber.trim());
        if (existing == null) return false;

        Train updated = existing.withStatus(status, delayMinutes);
        IN_MEMORY_TRAINS.put(trainNumber.trim(), updated);

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "UPDATE trains SET status = ?, delay_minutes = ? WHERE train_number = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                stmt.setInt(2, delayMinutes);
                stmt.setString(3, trainNumber.trim());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                logger.warn("Could not update train status in DB: {}", ex.getMessage());
            }
        }
        logger.info("Updated train {} status to {} (Delay: {}m)", trainNumber, status, delayMinutes);
        return true;
    }

    public synchronized boolean deleteTrain(String trainNumber) {
        if (trainNumber == null) return false;
        Train removed = IN_MEMORY_TRAINS.remove(trainNumber.trim());
        if (removed != null) {
            if (DatabaseConnectionPool.isAvailable()) {
                String sql = "DELETE FROM trains WHERE train_number = ?";
                try (Connection conn = DatabaseConnectionPool.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, trainNumber.trim());
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    logger.warn("Could not delete train from DB: {}", ex.getMessage());
                }
            }
            logger.info("Retired train {} from fleet", trainNumber);
            return true;
        }
        return false;
    }

    public synchronized boolean decrementSeatInventory(String trainNumber, String classCode, int count) {
        if (trainNumber == null || classCode == null || count <= 0) return false;
        Train train = IN_MEMORY_TRAINS.get(trainNumber.trim());
        if (train == null) return false;

        List<CoachAvailability> updatedClasses = new ArrayList<>();
        boolean found = false;
        for (CoachAvailability ca : train.getCoachClasses()) {
            if (ca.getClassCode().equalsIgnoreCase(classCode)) {
                found = true;
                int newAvail = Math.max(0, ca.getAvailableSeats() - count);
                int newRac = ca.getRacSeats();
                int newWl = ca.getWaitlistSeats();
                if (ca.getAvailableSeats() < count) {
                    int remaining = count - ca.getAvailableSeats();
                    newRac = Math.max(0, newRac - remaining);
                    if (ca.getRacSeats() < remaining) {
                        newWl += (remaining - ca.getRacSeats());
                    }
                }
                updatedClasses.add(ca.withAdjustedSeats(newAvail, newRac, newWl));
            } else {
                updatedClasses.add(ca);
            }
        }

        if (found) {
            Train updatedTrain = train.withCoachClasses(updatedClasses);
            IN_MEMORY_TRAINS.put(trainNumber.trim(), updatedTrain);

            if (DatabaseConnectionPool.isAvailable()) {
                String sql = "UPDATE train_classes SET available_seats = GREATEST(0, available_seats - ?) " +
                        "WHERE train_id = (SELECT id FROM trains WHERE train_number = ? LIMIT 1) AND class_code = ?";
                try (Connection conn = DatabaseConnectionPool.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, count);
                    stmt.setString(2, trainNumber.trim());
                    stmt.setString(3, classCode.toUpperCase());
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    logger.warn("Could not decrement DB seat inventory: {}", ex.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    public synchronized void incrementSeatInventory(String trainNumber, String classCode, int count) {
        if (trainNumber == null || classCode == null || count <= 0) return;
        Train train = IN_MEMORY_TRAINS.get(trainNumber.trim());
        if (train == null) return;

        List<CoachAvailability> updatedClasses = new ArrayList<>();
        for (CoachAvailability ca : train.getCoachClasses()) {
            if (ca.getClassCode().equalsIgnoreCase(classCode)) {
                int newAvail = Math.min(ca.getTotalSeats(), ca.getAvailableSeats() + count);
                updatedClasses.add(ca.withAdjustedSeats(newAvail, ca.getRacSeats(), ca.getWaitlistSeats()));
            } else {
                updatedClasses.add(ca);
            }
        }
        Train updatedTrain = train.withCoachClasses(updatedClasses);
        IN_MEMORY_TRAINS.put(trainNumber.trim(), updatedTrain);

        if (DatabaseConnectionPool.isAvailable()) {
            String sql = "UPDATE train_classes SET available_seats = LEAST(total_seats, available_seats + ?) " +
                    "WHERE train_id = (SELECT id FROM trains WHERE train_number = ? LIMIT 1) AND class_code = ?";
            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, count);
                stmt.setString(2, trainNumber.trim());
                stmt.setString(3, classCode.toUpperCase());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                logger.warn("Could not increment DB seat inventory: {}", ex.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Master In-Memory Seed Initialization (101 Iconic Trains across 17 Corridors)
    // ─────────────────────────────────────────────────────────────────────────
    private synchronized void ensureSeedData() {
        if (!IN_MEMORY_TRAINS.isEmpty()) return;

        Map<String, Station> s = new HashMap<>();
        for (Station st : stationDAO.getAllStations()) {
            s.put(st.getCode().toUpperCase(), st);
        }

        seedFleetBatch1(s);
        seedFleetBatch2(s);
        seedFleetBatch3(s);
        seedFleetBatch4(s);
        seedFleetBatch5(s);
        seedFleetBatch6(s);
        seedFleetBatch7(s);
        seedFleetBatch8(s);
        seedFleetBatch9(s);
        seedFleetBatch10(s);
        seedFleetBatch11(s);
    }

    private void seedFleetBatch1(Map<String, Station> s) {
        // 1. 12952 - New Delhi Tejas Rajdhani Express (NDLS -> MMCT)
        registerTrain(new Train(1L, "12952", "New Delhi Tejas Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 55), 0, 0, 1),
                        new RouteHalt(2, s.get("KOTA"), LocalTime.of(21, 30), LocalTime.of(21, 40), 10, 465, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(3, 15), LocalTime.of(3, 25), 10, 992, 2),
                        new RouteHalt(4, s.get("ST"), LocalTime.of(4, 50), LocalTime.of(4, 55), 5, 1122, 2),
                        new RouteHalt(5, s.get("MMCT"), LocalTime.of(8, 35), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 4280.0, 4280.0),
                        new CoachAvailability("2A", "GENERAL", 120, 36, 12, 0, 2850.0, 2850.0),
                        new CoachAvailability("3A", "GENERAL", 360, 94, 28, 0, 2080.0, 2080.0)
                )));

        // 2. 12951 - Mumbai Tejas Rajdhani Express (MMCT -> NDLS)
        registerTrain(new Train(2L, "12951", "Mumbai Tejas Rajdhani Express", TrainType.RAJDHANI,
                s.get("MMCT"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(17, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(19, 43), LocalTime.of(19, 48), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(21, 6), LocalTime.of(21, 16), 10, 392, 1),
                        new RouteHalt(4, s.get("KOTA"), LocalTime.of(3, 15), LocalTime.of(3, 20), 5, 919, 2),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(8, 32), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 4280.0, 4280.0),
                        new CoachAvailability("2A", "GENERAL", 120, 30, 12, 0, 2850.0, 2850.0),
                        new CoachAvailability("3A", "GENERAL", 360, 88, 28, 0, 2080.0, 2080.0)
                )));

        // 3. 12954 - August Kranti Tejas Rajdhani (NDLS -> MMCT)
        registerTrain(new Train(3L, "12954", "August Kranti Tejas Rajdhani", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(17, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(19, 50), LocalTime.of(19, 52), 2, 195, 1),
                        new RouteHalt(3, s.get("KOTA"), LocalTime.of(23, 35), LocalTime.of(23, 45), 10, 465, 1),
                        new RouteHalt(4, s.get("BRC"), LocalTime.of(5, 10), LocalTime.of(5, 20), 10, 992, 2),
                        new RouteHalt(5, s.get("ST"), LocalTime.of(6, 55), LocalTime.of(7, 0), 5, 1122, 2),
                        new RouteHalt(6, s.get("MMCT"), LocalTime.of(10, 5), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 4280.0, 4280.0),
                        new CoachAvailability("2A", "GENERAL", 120, 28, 12, 0, 2850.0, 2850.0),
                        new CoachAvailability("3A", "GENERAL", 360, 76, 28, 0, 2080.0, 2080.0)
                )));

        // 4. 12953 - August Kranti Tejas Rajdhani (MMCT -> NDLS)
        registerTrain(new Train(4L, "12953", "August Kranti Tejas Rajdhani", TrainType.RAJDHANI,
                s.get("MMCT"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(17, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(19, 50), LocalTime.of(19, 55), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(21, 20), LocalTime.of(21, 30), 10, 392, 1),
                        new RouteHalt(4, s.get("KOTA"), LocalTime.of(4, 10), LocalTime.of(4, 20), 10, 919, 2),
                        new RouteHalt(5, s.get("AGC"), LocalTime.of(7, 53), LocalTime.of(7, 55), 2, 1189, 2),
                        new RouteHalt(6, s.get("NDLS"), LocalTime.of(9, 43), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 4280.0, 4280.0),
                        new CoachAvailability("2A", "GENERAL", 120, 34, 12, 0, 2850.0, 2850.0),
                        new CoachAvailability("3A", "GENERAL", 360, 82, 28, 0, 2080.0, 2080.0)
                )));

        // 5. 12926 - Paschim Superfast Express (NDLS -> MMCT)
        registerTrain(new Train(5L, "12926", "Paschim Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("KOTA"), LocalTime.of(23, 30), LocalTime.of(23, 40), 10, 465, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(9, 10), LocalTime.of(9, 20), 10, 992, 2),
                        new RouteHalt(4, s.get("ST"), LocalTime.of(10, 50), LocalTime.of(10, 55), 5, 1122, 2),
                        new RouteHalt(5, s.get("MMCT"), LocalTime.of(14, 55), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 19, 8, 0, 2340.0, 2340.0),
                        new CoachAvailability("3A", "GENERAL", 380, 68, 26, 0, 1620.0, 1620.0),
                        new CoachAvailability("SL", "GENERAL", 640, 145, 48, 0, 610.0, 610.0)
                )));

        // 6. 12925 - Paschim Superfast Express (MMCT -> NDLS)
        registerTrain(new Train(6L, "12925", "Paschim Superfast Express", TrainType.SUPERFAST,
                s.get("MMCT"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(11, 25), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(15, 52), LocalTime.of(15, 57), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(17, 40), LocalTime.of(17, 50), 10, 392, 1),
                        new RouteHalt(4, s.get("KOTA"), LocalTime.of(1, 50), LocalTime.of(2, 0), 10, 919, 2),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(11, 5), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 16, 8, 0, 2340.0, 2340.0),
                        new CoachAvailability("3A", "GENERAL", 380, 62, 26, 0, 1620.0, 1620.0),
                        new CoachAvailability("SL", "GENERAL", 640, 130, 48, 0, 610.0, 610.0)
                )));

        // 7. 12910 - Bandra Garib Rath Express (NDLS -> MMCT)
        registerTrain(new Train(7L, "12910", "Bandra Garib Rath Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(15, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("KOTA"), LocalTime.of(21, 15), LocalTime.of(21, 20), 5, 465, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(3, 30), LocalTime.of(3, 40), 10, 992, 2),
                        new RouteHalt(4, s.get("ST"), LocalTime.of(5, 15), LocalTime.of(5, 20), 5, 1122, 2),
                        new RouteHalt(5, s.get("MMCT"), LocalTime.of(7, 35), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("3A", "GENERAL", 720, 165, 50, 0, 1050.0, 1050.0)
                )));

        // 8. 12909 - Bandra Garib Rath Express (MMCT -> NDLS)
        registerTrain(new Train(8L, "12909", "Bandra Garib Rath Express", TrainType.SUPERFAST,
                s.get("MMCT"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(17, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(20, 47), LocalTime.of(20, 52), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(22, 17), LocalTime.of(22, 27), 10, 392, 1),
                        new RouteHalt(4, s.get("KOTA"), LocalTime.of(4, 35), LocalTime.of(4, 40), 5, 919, 2),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(9, 40), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("3A", "GENERAL", 720, 150, 50, 0, 1050.0, 1050.0)
                )));

        // 9. 12904 - Golden Temple Mail (NDLS -> MMCT)
        registerTrain(new Train(9L, "12904", "Golden Temple Mail", TrainType.EXPRESS,
                s.get("NDLS"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(7, 20), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(10, 15), LocalTime.of(10, 20), 5, 195, 1),
                        new RouteHalt(3, s.get("KOTA"), LocalTime.of(15, 0), LocalTime.of(15, 10), 10, 465, 1),
                        new RouteHalt(4, s.get("BRC"), LocalTime.of(23, 10), LocalTime.of(23, 20), 10, 992, 1),
                        new RouteHalt(5, s.get("ST"), LocalTime.of(1, 5), LocalTime.of(1, 10), 5, 1122, 2),
                        new RouteHalt(6, s.get("MMCT"), LocalTime.of(5, 20), null, 0, 1384, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 3850.0, 3850.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 2260.0, 2260.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 1580.0, 1580.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 580.0, 580.0)
                )));

        // 10. 22222 - Mumbai CSMT Rajdhani Express (NDLS -> CSMT)
        registerTrain(new Train(10L, "22222", "Mumbai CSMT Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("CSMT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 55), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(18, 45), LocalTime.of(18, 47), 2, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(20, 8), LocalTime.of(20, 10), 2, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(0, 35), LocalTime.of(0, 40), 5, 705, 2),
                        new RouteHalt(5, s.get("NGP"), LocalTime.of(5, 10), LocalTime.of(5, 15), 5, 1090, 2),
                        new RouteHalt(6, s.get("CSMT"), LocalTime.of(11, 15), null, 0, 1540, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 9, 0, 0, 4420.0, 4420.0),
                        new CoachAvailability("2A", "GENERAL", 120, 40, 12, 0, 2980.0, 2980.0),
                        new CoachAvailability("3A", "GENERAL", 360, 110, 28, 0, 2150.0, 2150.0)
                )));

    }

    private void seedFleetBatch2(Map<String, Station> s) {
        // 11. 22221 - Mumbai CSMT Rajdhani Express (CSMT -> NDLS)
        registerTrain(new Train(11L, "22221", "Mumbai CSMT Rajdhani Express", TrainType.RAJDHANI,
                s.get("CSMT"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(16, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("NGP"), LocalTime.of(21, 55), LocalTime.of(22, 0), 5, 450, 1),
                        new RouteHalt(3, s.get("BPL"), LocalTime.of(2, 15), LocalTime.of(2, 20), 5, 835, 2),
                        new RouteHalt(4, s.get("GWL"), LocalTime.of(6, 28), LocalTime.of(6, 30), 2, 1227, 2),
                        new RouteHalt(5, s.get("AGC"), LocalTime.of(7, 50), LocalTime.of(7, 52), 2, 1345, 2),
                        new RouteHalt(6, s.get("NDLS"), LocalTime.of(9, 55), null, 0, 1540, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 4420.0, 4420.0),
                        new CoachAvailability("2A", "GENERAL", 120, 38, 12, 0, 2980.0, 2980.0),
                        new CoachAvailability("3A", "GENERAL", 360, 105, 28, 0, 2150.0, 2150.0)
                )));

        // 12. 22436 - Vande Bharat Express (NDLS -> BSB)
        registerTrain(new Train(12L, "22436", "Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("BSB"), "0110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(10, 10), LocalTime.of(10, 14), 4, 440, 1),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(12, 8), LocalTime.of(12, 10), 2, 635, 1),
                        new RouteHalt(4, s.get("BSB"), LocalTime.of(14, 0), null, 0, 759, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 142, 20, 0, 1750.0, 1750.0),
                        new CoachAvailability("EC", "GENERAL", 52, 19, 4, 0, 3300.0, 3300.0)
                )));

        // 13. 22435 - Vande Bharat Express (BSB -> NDLS)
        registerTrain(new Train(13L, "22435", "Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("BSB"), s.get("NDLS"), "0110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("BSB"), null, LocalTime.of(15, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("PRYJ"), LocalTime.of(16, 30), LocalTime.of(16, 32), 2, 124, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(18, 30), LocalTime.of(18, 34), 4, 319, 1),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(23, 0), null, 0, 759, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 135, 20, 0, 1750.0, 1750.0),
                        new CoachAvailability("EC", "GENERAL", 52, 17, 4, 0, 3300.0, 3300.0)
                )));

        // 14. 22416 - Vande Bharat Express (Evening) (NDLS -> BSB)
        registerTrain(new Train(14L, "22416", "Vande Bharat Express (Evening)", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("BSB"), "1110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(15, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(19, 8), LocalTime.of(19, 12), 4, 440, 1),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(21, 11), LocalTime.of(21, 15), 4, 635, 1),
                        new RouteHalt(4, s.get("BSB"), LocalTime.of(23, 5), null, 0, 759, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 160, 20, 0, 1750.0, 1750.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 3300.0, 3300.0)
                )));

        // 15. 22415 - Vande Bharat Express (Morning) (BSB -> NDLS)
        registerTrain(new Train(15L, "22415", "Vande Bharat Express (Morning)", TrainType.VANDE_BHARAT,
                s.get("BSB"), s.get("NDLS"), "1110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("BSB"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("PRYJ"), LocalTime.of(7, 30), LocalTime.of(7, 34), 4, 124, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(9, 26), LocalTime.of(9, 30), 4, 319, 1),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(14, 5), null, 0, 759, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 155, 20, 0, 1750.0, 1750.0),
                        new CoachAvailability("EC", "GENERAL", 52, 20, 4, 0, 3300.0, 3300.0)
                )));

        // 16. 12560 - Shiv Ganga Superfast Express (NDLS -> BSB)
        registerTrain(new Train(16L, "12560", "Shiv Ganga Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("BSB"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(20, 5), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(1, 0), LocalTime.of(1, 5), 5, 440, 2),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(3, 45), LocalTime.of(3, 55), 10, 635, 2),
                        new RouteHalt(4, s.get("BSB"), LocalTime.of(6, 10), null, 0, 759, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 2760.0, 2760.0),
                        new CoachAvailability("2A", "GENERAL", 96, 28, 8, 0, 1640.0, 1640.0),
                        new CoachAvailability("3A", "GENERAL", 360, 95, 24, 0, 1165.0, 1165.0),
                        new CoachAvailability("SL", "GENERAL", 600, 210, 45, 0, 435.0, 435.0)
                )));

        // 17. 12559 - Shiv Ganga Superfast Express (BSB -> NDLS)
        registerTrain(new Train(17L, "12559", "Shiv Ganga Superfast Express", TrainType.SUPERFAST,
                s.get("BSB"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("BSB"), null, LocalTime.of(22, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("PRYJ"), LocalTime.of(0, 30), LocalTime.of(0, 35), 5, 124, 2),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(2, 45), LocalTime.of(2, 50), 5, 319, 2),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(8, 30), null, 0, 759, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 2760.0, 2760.0),
                        new CoachAvailability("2A", "GENERAL", 96, 25, 8, 0, 1640.0, 1640.0),
                        new CoachAvailability("3A", "GENERAL", 360, 88, 24, 0, 1165.0, 1165.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 435.0, 435.0)
                )));

        // 18. 12582 - Banaras Superfast Express (NDLS -> BSB)
        registerTrain(new Train(18L, "12582", "Banaras Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("BSB"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(22, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(23, 20), LocalTime.of(23, 22), 2, 26, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(4, 55), LocalTime.of(5, 0), 5, 440, 2),
                        new RouteHalt(4, s.get("PRYJ"), LocalTime.of(7, 35), LocalTime.of(7, 45), 10, 635, 2),
                        new RouteHalt(5, s.get("BSB"), LocalTime.of(10, 0), null, 0, 759, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 20, 8, 0, 1640.0, 1640.0),
                        new CoachAvailability("3A", "GENERAL", 380, 72, 26, 0, 1165.0, 1165.0),
                        new CoachAvailability("SL", "GENERAL", 640, 175, 48, 0, 435.0, 435.0)
                )));

        // 19. 15128 - Kashi Vishwanath Express (NDLS -> BSB)
        registerTrain(new Train(19L, "15128", "Kashi Vishwanath Express", TrainType.EXPRESS,
                s.get("NDLS"), s.get("BSB"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(11, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(12, 15), LocalTime.of(12, 17), 2, 26, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(13, 30), LocalTime.of(13, 35), 5, 131, 1),
                        new RouteHalt(4, s.get("LKO"), LocalTime.of(21, 10), LocalTime.of(21, 20), 10, 512, 1),
                        new RouteHalt(5, s.get("BSB"), LocalTime.of(4, 50), null, 0, 795, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1570.0, 1570.0),
                        new CoachAvailability("3A", "GENERAL", 380, 85, 26, 0, 1110.0, 1110.0),
                        new CoachAvailability("SL", "GENERAL", 640, 230, 48, 0, 410.0, 410.0)
                )));

        // 20. 12004 - Lucknow Swarna Shatabdi Express (NDLS -> LKO)
        registerTrain(new Train(20L, "12004", "Lucknow Swarna Shatabdi Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("LKO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(6, 45), LocalTime.of(6, 47), 2, 26, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(7, 47), LocalTime.of(7, 49), 2, 131, 1),
                        new RouteHalt(4, s.get("CNB"), LocalTime.of(11, 20), LocalTime.of(11, 25), 5, 440, 1),
                        new RouteHalt(5, s.get("LKO"), LocalTime.of(12, 40), null, 0, 512, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 115, 18, 0, 1165.0, 1165.0),
                        new CoachAvailability("EC", "GENERAL", 48, 12, 4, 0, 2125.0, 2125.0)
                )));

    }

    private void seedFleetBatch3(Map<String, Station> s) {
        // 21. 12003 - Lucknow Shatabdi Express (LKO -> NDLS)
        registerTrain(new Train(21L, "12003", "Lucknow Shatabdi Express", TrainType.SHATABDI,
                s.get("LKO"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("LKO"), null, LocalTime.of(15, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(16, 50), LocalTime.of(16, 55), 5, 72, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(20, 10), LocalTime.of(20, 12), 2, 381, 1),
                        new RouteHalt(4, s.get("GZB"), LocalTime.of(21, 33), LocalTime.of(21, 35), 2, 486, 1),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(22, 20), null, 0, 512, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 120, 18, 0, 1165.0, 1165.0),
                        new CoachAvailability("EC", "GENERAL", 48, 14, 4, 0, 2125.0, 2125.0)
                )));

        // 22. 82502 - IRCTC Tejas Express (NDLS -> LKO)
        registerTrain(new Train(22L, "82502", "IRCTC Tejas Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("LKO"), "1011111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(15, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(16, 11), LocalTime.of(16, 13), 2, 26, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(20, 35), LocalTime.of(20, 40), 5, 440, 1),
                        new RouteHalt(4, s.get("LKO"), LocalTime.of(22, 5), null, 0, 512, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 130, 18, 0, 1280.0, 1280.0),
                        new CoachAvailability("EC", "GENERAL", 48, 16, 4, 0, 2350.0, 2350.0)
                )));

        // 23. 82501 - IRCTC Tejas Express (LKO -> NDLS)
        registerTrain(new Train(23L, "82501", "IRCTC Tejas Express", TrainType.SHATABDI,
                s.get("LKO"), s.get("NDLS"), "1011111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("LKO"), null, LocalTime.of(6, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(7, 20), LocalTime.of(7, 25), 5, 72, 1),
                        new RouteHalt(3, s.get("GZB"), LocalTime.of(11, 43), LocalTime.of(11, 45), 2, 486, 1),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(12, 25), null, 0, 512, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 125, 18, 0, 1280.0, 1280.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 2350.0, 2350.0)
                )));

        // 24. 12230 - Lucknow Mail (NDLS -> LKO)
        registerTrain(new Train(24L, "12230", "Lucknow Mail", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("LKO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(22, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(22, 43), LocalTime.of(22, 45), 2, 26, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(0, 3), LocalTime.of(0, 5), 2, 131, 2),
                        new RouteHalt(4, s.get("LKO"), LocalTime.of(6, 50), null, 0, 492, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 2040.0, 2040.0),
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1220.0, 1220.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 860.0, 860.0),
                        new CoachAvailability("SL", "GENERAL", 600, 180, 45, 0, 325.0, 325.0)
                )));

        // 25. 12229 - Lucknow Mail (LKO -> NDLS)
        registerTrain(new Train(25L, "12229", "Lucknow Mail", TrainType.SUPERFAST,
                s.get("LKO"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("LKO"), null, LocalTime.of(22, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("ALJN"), LocalTime.of(4, 30), LocalTime.of(4, 32), 2, 361, 2),
                        new RouteHalt(3, s.get("GZB"), LocalTime.of(6, 5), LocalTime.of(6, 7), 2, 466, 2),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(6, 55), null, 0, 492, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 2040.0, 2040.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1220.0, 1220.0),
                        new CoachAvailability("3A", "GENERAL", 360, 78, 24, 0, 860.0, 860.0),
                        new CoachAvailability("SL", "GENERAL", 600, 175, 45, 0, 325.0, 325.0)
                )));

        // 26. 12430 - New Delhi - Lucknow AC Superfast (NDLS -> LKO)
        registerTrain(new Train(26L, "12430", "New Delhi - Lucknow AC Superfast", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("LKO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(23, 25), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(0, 3), LocalTime.of(0, 5), 2, 26, 2),
                        new RouteHalt(3, s.get("LKO"), LocalTime.of(7, 10), null, 0, 492, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 2180.0, 2180.0),
                        new CoachAvailability("2A", "GENERAL", 120, 36, 12, 0, 1310.0, 1310.0),
                        new CoachAvailability("3A", "GENERAL", 360, 105, 28, 0, 920.0, 920.0)
                )));

        // 27. 12420 - Gomti Express (NDLS -> LKO)
        registerTrain(new Train(27L, "12420", "Gomti Express", TrainType.EXPRESS,
                s.get("NDLS"), s.get("LKO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(12, 20), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(12, 54), LocalTime.of(12, 56), 2, 26, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(14, 15), LocalTime.of(14, 17), 2, 131, 1),
                        new RouteHalt(4, s.get("CNB"), LocalTime.of(19, 45), LocalTime.of(19, 50), 5, 440, 1),
                        new RouteHalt(5, s.get("LKO"), LocalTime.of(21, 30), null, 0, 512, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 85, 15, 0, 680.0, 680.0),
                        new CoachAvailability("2S", "GENERAL", 600, 280, 40, 0, 195.0, 195.0)
                )));

        // 28. 12302 - Howrah Rajdhani Express (via Gaya) (NDLS -> HWH)
        registerTrain(new Train(28L, "12302", "Howrah Rajdhani Express (via Gaya)", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("HWH"), "1111111", TrainStatus.DEPARTED,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(21, 32), LocalTime.of(21, 37), 5, 440, 1),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 635, 1),
                        new RouteHalt(4, s.get("DDU"), LocalTime.of(1, 37), LocalTime.of(1, 47), 10, 788, 2),
                        new RouteHalt(5, s.get("HWH"), LocalTime.of(9, 55), null, 0, 1451, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 2, 0, 0, 4650.0, 4650.0),
                        new CoachAvailability("2A", "GENERAL", 120, 42, 12, 0, 3050.0, 3050.0),
                        new CoachAvailability("3A", "GENERAL", 360, 110, 28, 0, 2220.0, 2220.0)
                )));

        // 29. 12301 - Howrah Rajdhani Express (via Gaya) (HWH -> NDLS)
        registerTrain(new Train(29L, "12301", "Howrah Rajdhani Express (via Gaya)", TrainType.RAJDHANI,
                s.get("HWH"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(16, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("DDU"), LocalTime.of(0, 45), LocalTime.of(0, 55), 10, 663, 2),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(2, 33), LocalTime.of(2, 35), 2, 816, 2),
                        new RouteHalt(4, s.get("CNB"), LocalTime.of(4, 40), LocalTime.of(4, 45), 5, 1011, 2),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(10, 5), null, 0, 1451, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 4650.0, 4650.0),
                        new CoachAvailability("2A", "GENERAL", 120, 38, 12, 0, 3050.0, 3050.0),
                        new CoachAvailability("3A", "GENERAL", 360, 98, 28, 0, 2220.0, 2220.0)
                )));

        // 30. 12306 - Howrah Rajdhani Express (via Patna) (NDLS -> HWH)
        registerTrain(new Train(30L, "12306", "Howrah Rajdhani Express (via Patna)", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("HWH"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(21, 32), LocalTime.of(21, 37), 5, 440, 1),
                        new RouteHalt(3, s.get("PRYJ"), LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 635, 1),
                        new RouteHalt(4, s.get("DDU"), LocalTime.of(1, 37), LocalTime.of(1, 47), 10, 788, 2),
                        new RouteHalt(5, s.get("PNBE"), LocalTime.of(4, 10), LocalTime.of(4, 20), 10, 1000, 2),
                        new RouteHalt(6, s.get("HWH"), LocalTime.of(12, 25), null, 0, 1530, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 4, 0, 0, 4780.0, 4780.0),
                        new CoachAvailability("2A", "GENERAL", 120, 32, 12, 0, 3120.0, 3120.0),
                        new CoachAvailability("3A", "GENERAL", 360, 90, 28, 0, 2280.0, 2280.0)
                )));

    }

    private void seedFleetBatch4(Map<String, Station> s) {
        // 31. 12304 - Poorva Superfast Express (NDLS -> HWH)
        registerTrain(new Train(31L, "12304", "Poorva Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("HWH"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(17, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("ALJN"), LocalTime.of(19, 13), LocalTime.of(19, 15), 2, 131, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(22, 55), LocalTime.of(23, 5), 10, 440, 1),
                        new RouteHalt(4, s.get("PRYJ"), LocalTime.of(1, 15), LocalTime.of(1, 20), 5, 635, 2),
                        new RouteHalt(5, s.get("DDU"), LocalTime.of(3, 50), LocalTime.of(4, 0), 10, 788, 2),
                        new RouteHalt(6, s.get("PNBE"), LocalTime.of(6, 50), LocalTime.of(7, 0), 10, 1000, 2),
                        new RouteHalt(7, s.get("HWH"), LocalTime.of(17, 0), null, 0, 1530, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 4120.0, 4120.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 2450.0, 2450.0),
                        new CoachAvailability("3A", "GENERAL", 360, 75, 24, 0, 1690.0, 1690.0),
                        new CoachAvailability("SL", "GENERAL", 600, 185, 45, 0, 640.0, 640.0)
                )));

        // 32. 12303 - Poorva Superfast Express (HWH -> NDLS)
        registerTrain(new Train(32L, "12303", "Poorva Superfast Express", TrainType.SUPERFAST,
                s.get("HWH"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(8, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("PNBE"), LocalTime.of(16, 0), LocalTime.of(16, 10), 10, 530, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(19, 30), LocalTime.of(19, 40), 10, 742, 1),
                        new RouteHalt(4, s.get("PRYJ"), LocalTime.of(22, 0), LocalTime.of(22, 5), 5, 895, 1),
                        new RouteHalt(5, s.get("CNB"), LocalTime.of(0, 2), LocalTime.of(0, 7), 5, 1090, 2),
                        new RouteHalt(6, s.get("NDLS"), LocalTime.of(6, 0), null, 0, 1530, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 4120.0, 4120.0),
                        new CoachAvailability("2A", "GENERAL", 96, 19, 8, 0, 2450.0, 2450.0),
                        new CoachAvailability("3A", "GENERAL", 360, 70, 24, 0, 1690.0, 1690.0),
                        new CoachAvailability("SL", "GENERAL", 600, 170, 45, 0, 640.0, 640.0)
                )));

        // 33. 12314 - Sealdah Rajdhani Express (NDLS -> HWH)
        registerTrain(new Train(33L, "12314", "Sealdah Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("HWH"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(21, 12), LocalTime.of(21, 17), 5, 440, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(1, 27), LocalTime.of(1, 37), 10, 788, 2),
                        new RouteHalt(4, s.get("HWH"), LocalTime.of(10, 10), null, 0, 1458, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 4, 0, 0, 4690.0, 4690.0),
                        new CoachAvailability("2A", "GENERAL", 120, 28, 12, 0, 3080.0, 3080.0),
                        new CoachAvailability("3A", "GENERAL", 360, 85, 28, 0, 2240.0, 2240.0)
                )));

        // 34. 12312 - Netaji Express (Kalka Mail) (NDLS -> HWH)
        registerTrain(new Train(34L, "12312", "Netaji Express (Kalka Mail)", TrainType.EXPRESS,
                s.get("NDLS"), s.get("HWH"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(7, 0), LocalTime.of(7, 2), 2, 26, 1),
                        new RouteHalt(3, s.get("ALJN"), LocalTime.of(8, 30), LocalTime.of(8, 35), 5, 131, 1),
                        new RouteHalt(4, s.get("CNB"), LocalTime.of(13, 45), LocalTime.of(13, 55), 10, 440, 1),
                        new RouteHalt(5, s.get("PRYJ"), LocalTime.of(17, 5), LocalTime.of(17, 10), 5, 635, 1),
                        new RouteHalt(6, s.get("DDU"), LocalTime.of(20, 30), LocalTime.of(20, 40), 10, 788, 1),
                        new RouteHalt(7, s.get("HWH"), LocalTime.of(8, 5), null, 0, 1451, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 2370.0, 2370.0),
                        new CoachAvailability("3A", "GENERAL", 380, 90, 26, 0, 1630.0, 1630.0),
                        new CoachAvailability("SL", "GENERAL", 640, 240, 48, 0, 610.0, 610.0)
                )));

        // 35. 12015 - Ajmer Shatabdi Express (NDLS -> JP)
        registerTrain(new Train(35L, "12015", "Ajmer Shatabdi Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("JP"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(6, 48), LocalTime.of(6, 50), 2, 26, 1),
                        new RouteHalt(3, s.get("JP"), LocalTime.of(10, 40), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 110, 18, 0, 860.0, 860.0),
                        new CoachAvailability("EC", "GENERAL", 48, 12, 4, 0, 1610.0, 1610.0)
                )));

        // 36. 12016 - Ajmer - New Delhi Shatabdi Express (JP -> NDLS)
        registerTrain(new Train(36L, "12016", "Ajmer - New Delhi Shatabdi Express", TrainType.SHATABDI,
                s.get("JP"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("JP"), null, LocalTime.of(17, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(21, 40), LocalTime.of(21, 42), 2, 282, 1),
                        new RouteHalt(3, s.get("NDLS"), LocalTime.of(22, 40), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 115, 18, 0, 860.0, 860.0),
                        new CoachAvailability("EC", "GENERAL", 48, 14, 4, 0, 1610.0, 1610.0)
                )));

        // 37. 20978 - Delhi - Ajmer Vande Bharat Express (NDLS -> JP)
        registerTrain(new Train(37L, "20978", "Delhi - Ajmer Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("JP"), "1101111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(18, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(19, 5), LocalTime.of(19, 7), 2, 26, 1),
                        new RouteHalt(3, s.get("JP"), LocalTime.of(22, 5), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 160, 20, 0, 1050.0, 1050.0),
                        new CoachAvailability("EC", "GENERAL", 52, 25, 4, 0, 2050.0, 2050.0)
                )));

        // 38. 20977 - Ajmer - Delhi Vande Bharat Express (JP -> NDLS)
        registerTrain(new Train(38L, "20977", "Ajmer - Delhi Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("JP"), s.get("NDLS"), "1101111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("JP"), null, LocalTime.of(7, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(11, 5), LocalTime.of(11, 7), 2, 282, 1),
                        new RouteHalt(3, s.get("NDLS"), LocalTime.of(11, 35), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 150, 20, 0, 1050.0, 1050.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2050.0, 2050.0)
                )));

        // 39. 12986 - Delhi - Jaipur Double Decker (NDLS -> JP)
        registerTrain(new Train(39L, "12986", "Delhi - Jaipur Double Decker", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("JP"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(17, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("JP"), LocalTime.of(22, 0), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 840, 240, 35, 0, 520.0, 520.0)
                )));

        // 40. 12985 - Jaipur - Delhi Double Decker (JP -> NDLS)
        registerTrain(new Train(40L, "12985", "Jaipur - Delhi Double Decker", TrainType.SUPERFAST,
                s.get("JP"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("JP"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("NDLS"), LocalTime.of(10, 25), null, 0, 308, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 840, 230, 35, 0, 520.0, 520.0)
                )));

    }

    private void seedFleetBatch5(Map<String, Station> s) {
        // 41. 12958 - Swarna Jayanti Rajdhani Express (NDLS -> JP)
        registerTrain(new Train(41L, "12958", "Swarna Jayanti Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("JP"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(19, 55), 0, 0, 1),
                        new RouteHalt(2, s.get("JP"), LocalTime.of(0, 5), null, 0, 308, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 2150.0, 2150.0),
                        new CoachAvailability("2A", "GENERAL", 120, 24, 12, 0, 1380.0, 1380.0),
                        new CoachAvailability("3A", "GENERAL", 360, 65, 28, 0, 960.0, 960.0)
                )));

        // 42. 14660 - Mandore Express (NDLS -> JP)
        registerTrain(new Train(42L, "14660", "Mandore Express", TrainType.EXPRESS,
                s.get("NDLS"), s.get("JP"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(21, 45), 0, 0, 1),
                        new RouteHalt(2, s.get("JP"), LocalTime.of(2, 40), null, 0, 308, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 1490.0, 1490.0),
                        new CoachAvailability("2A", "GENERAL", 96, 20, 8, 0, 880.0, 880.0),
                        new CoachAvailability("3A", "GENERAL", 360, 75, 24, 0, 620.0, 620.0),
                        new CoachAvailability("SL", "GENERAL", 600, 190, 45, 0, 235.0, 235.0)
                )));

        // 43. 20608 - Mysuru - Chennai Vande Bharat Express (MAS -> MYS)
        registerTrain(new Train(43L, "20608", "Mysuru - Chennai Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("MAS"), s.get("MYS"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(5, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(7, 13), LocalTime.of(7, 15), 2, 130, 1),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(10, 15), LocalTime.of(10, 20), 5, 359, 1),
                        new RouteHalt(4, s.get("MYS"), LocalTime.of(12, 20), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 168, 20, 0, 1200.0, 1200.0),
                        new CoachAvailability("EC", "GENERAL", 52, 24, 4, 0, 2295.0, 2295.0)
                )));

        // 44. 20607 - Chennai Vande Bharat Express (MYS -> MAS)
        registerTrain(new Train(44L, "20607", "Chennai Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("MYS"), s.get("MAS"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MYS"), null, LocalTime.of(13, 5), 0, 0, 1),
                        new RouteHalt(2, s.get("SBC"), LocalTime.of(14, 50), LocalTime.of(14, 55), 5, 138, 1),
                        new RouteHalt(3, s.get("KPD"), LocalTime.of(17, 33), LocalTime.of(17, 35), 2, 367, 1),
                        new RouteHalt(4, s.get("MAS"), LocalTime.of(19, 20), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 160, 20, 0, 1200.0, 1200.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2295.0, 2295.0)
                )));

        // 45. 12007 - Chennai - Mysuru Shatabdi Express (MAS -> MYS)
        registerTrain(new Train(45L, "12007", "Chennai - Mysuru Shatabdi Express", TrainType.SHATABDI,
                s.get("MAS"), s.get("MYS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(7, 35), LocalTime.of(7, 37), 2, 130, 1),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(10, 45), LocalTime.of(10, 50), 5, 359, 1),
                        new RouteHalt(4, s.get("MYS"), LocalTime.of(13, 0), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 135, 18, 0, 1080.0, 1080.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 2040.0, 2040.0)
                )));

        // 46. 12008 - Mysuru - Chennai Shatabdi Express (MYS -> MAS)
        registerTrain(new Train(46L, "12008", "Mysuru - Chennai Shatabdi Express", TrainType.SHATABDI,
                s.get("MYS"), s.get("MAS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MYS"), null, LocalTime.of(14, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("SBC"), LocalTime.of(16, 15), LocalTime.of(16, 20), 5, 138, 1),
                        new RouteHalt(3, s.get("KPD"), LocalTime.of(19, 23), LocalTime.of(19, 25), 2, 367, 1),
                        new RouteHalt(4, s.get("MAS"), LocalTime.of(21, 30), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 130, 18, 0, 1080.0, 1080.0),
                        new CoachAvailability("EC", "GENERAL", 48, 14, 4, 0, 2040.0, 2040.0)
                )));

        // 47. 20664 - MGR Chennai - Mysuru Vande Bharat (MAS -> MYS)
        registerTrain(new Train(47L, "20664", "MGR Chennai - Mysuru Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("MAS"), s.get("MYS"), "1101111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(17, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(18, 18), LocalTime.of(18, 20), 2, 130, 1),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(21, 25), LocalTime.of(21, 30), 5, 359, 1),
                        new RouteHalt(4, s.get("MYS"), LocalTime.of(23, 20), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 145, 20, 0, 1200.0, 1200.0),
                        new CoachAvailability("EC", "GENERAL", 52, 19, 4, 0, 2295.0, 2295.0)
                )));

        // 48. 12027 - Chennai - Bengaluru Shatabdi Express (MAS -> SBC)
        registerTrain(new Train(48L, "12027", "Chennai - Bengaluru Shatabdi Express", TrainType.SHATABDI,
                s.get("MAS"), s.get("SBC"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(17, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(19, 8), LocalTime.of(19, 10), 2, 130, 1),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(22, 25), null, 0, 359, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 115, 18, 0, 940.0, 940.0),
                        new CoachAvailability("EC", "GENERAL", 48, 12, 4, 0, 1820.0, 1820.0)
                )));

        // 49. 12657 - Chennai - Bengaluru Mail (MAS -> SBC)
        registerTrain(new Train(49L, "12657", "Chennai - Bengaluru Mail", TrainType.SUPERFAST,
                s.get("MAS"), s.get("SBC"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(23, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(0, 58), LocalTime.of(1, 0), 2, 130, 2),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(4, 30), null, 0, 359, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 1680.0, 1680.0),
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1020.0, 1020.0),
                        new CoachAvailability("3A", "GENERAL", 360, 85, 24, 0, 720.0, 720.0),
                        new CoachAvailability("SL", "GENERAL", 600, 210, 45, 0, 270.0, 270.0)
                )));

        // 50. 12658 - Bengaluru - Chennai Mail (SBC -> MAS)
        registerTrain(new Train(50L, "12658", "Bengaluru - Chennai Mail", TrainType.SUPERFAST,
                s.get("SBC"), s.get("MAS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("SBC"), null, LocalTime.of(22, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(2, 18), LocalTime.of(2, 20), 2, 229, 2),
                        new RouteHalt(3, s.get("MAS"), LocalTime.of(4, 15), null, 0, 359, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 1680.0, 1680.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1020.0, 1020.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 720.0, 720.0),
                        new CoachAvailability("SL", "GENERAL", 600, 205, 45, 0, 270.0, 270.0)
                )));

    }

    private void seedFleetBatch6(Map<String, Station> s) {
        // 51. 12609 - MGR Chennai - Mysuru Express (MAS -> MYS)
        registerTrain(new Train(51L, "12609", "MGR Chennai - Mysuru Express", TrainType.EXPRESS,
                s.get("MAS"), s.get("MYS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAS"), null, LocalTime.of(13, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("KPD"), LocalTime.of(15, 28), LocalTime.of(15, 30), 2, 130, 1),
                        new RouteHalt(3, s.get("SBC"), LocalTime.of(19, 50), LocalTime.of(20, 0), 10, 359, 1),
                        new RouteHalt(4, s.get("MYS"), LocalTime.of(22, 50), null, 0, 497, 1)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 25, 8, 0, 1150.0, 1150.0),
                        new CoachAvailability("3A", "GENERAL", 380, 92, 26, 0, 810.0, 810.0),
                        new CoachAvailability("SL", "GENERAL", 640, 260, 48, 0, 310.0, 310.0)
                )));

        // 52. 20901 - Mumbai - Gandhinagar Vande Bharat (MMCT -> ADI)
        registerTrain(new Train(52L, "20901", "Mumbai - Gandhinagar Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("MMCT"), s.get("ADI"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(8, 37), LocalTime.of(8, 40), 3, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(9, 56), LocalTime.of(9, 59), 3, 392, 1),
                        new RouteHalt(4, s.get("ADI"), LocalTime.of(11, 25), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 150, 20, 0, 1365.0, 1365.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2485.0, 2485.0)
                )));

        // 53. 20902 - Gandhinagar - Mumbai Vande Bharat (ADI -> MMCT)
        registerTrain(new Train(53L, "20902", "Gandhinagar - Mumbai Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("ADI"), s.get("MMCT"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("ADI"), null, LocalTime.of(15, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("BRC"), LocalTime.of(16, 13), LocalTime.of(16, 15), 2, 100, 1),
                        new RouteHalt(3, s.get("ST"), LocalTime.of(17, 33), LocalTime.of(17, 36), 3, 229, 1),
                        new RouteHalt(4, s.get("MMCT"), LocalTime.of(20, 25), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 145, 20, 0, 1365.0, 1365.0),
                        new CoachAvailability("EC", "GENERAL", 52, 20, 4, 0, 2485.0, 2485.0)
                )));

        // 54. 12009 - Mumbai - Ahmedabad Shatabdi Express (MMCT -> ADI)
        registerTrain(new Train(54L, "12009", "Mumbai - Ahmedabad Shatabdi Express", TrainType.SHATABDI,
                s.get("MMCT"), s.get("ADI"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(6, 20), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(9, 15), LocalTime.of(9, 18), 3, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(10, 48), LocalTime.of(10, 53), 5, 392, 1),
                        new RouteHalt(4, s.get("ADI"), LocalTime.of(12, 45), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 134, 18, 0, 1110.0, 1110.0),
                        new CoachAvailability("EC", "GENERAL", 48, 16, 4, 0, 2140.0, 2140.0)
                )));

        // 55. 12010 - Ahmedabad - Mumbai Shatabdi Express (ADI -> MMCT)
        registerTrain(new Train(55L, "12010", "Ahmedabad - Mumbai Shatabdi Express", TrainType.SHATABDI,
                s.get("ADI"), s.get("MMCT"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("ADI"), null, LocalTime.of(15, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("BRC"), LocalTime.of(16, 42), LocalTime.of(16, 47), 5, 100, 1),
                        new RouteHalt(3, s.get("ST"), LocalTime.of(18, 2), LocalTime.of(18, 7), 5, 229, 1),
                        new RouteHalt(4, s.get("MMCT"), LocalTime.of(21, 45), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 128, 18, 0, 1110.0, 1110.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 2140.0, 2140.0)
                )));

        // 56. 12931 - Mumbai - Ahmedabad Double Decker (MMCT -> ADI)
        registerTrain(new Train(56L, "12931", "Mumbai - Ahmedabad Double Decker", TrainType.SUPERFAST,
                s.get("MMCT"), s.get("ADI"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(14, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(17, 52), LocalTime.of(17, 57), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(19, 26), LocalTime.of(19, 31), 5, 392, 1),
                        new RouteHalt(4, s.get("ADI"), LocalTime.of(21, 25), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 840, 240, 35, 0, 620.0, 620.0)
                )));

        // 57. 12932 - Ahmedabad - Mumbai Double Decker (ADI -> MMCT)
        registerTrain(new Train(57L, "12932", "Ahmedabad - Mumbai Double Decker", TrainType.SUPERFAST,
                s.get("ADI"), s.get("MMCT"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("ADI"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("BRC"), LocalTime.of(7, 25), LocalTime.of(7, 30), 5, 100, 1),
                        new RouteHalt(3, s.get("ST"), LocalTime.of(9, 0), LocalTime.of(9, 5), 5, 229, 1),
                        new RouteHalt(4, s.get("MMCT"), LocalTime.of(13, 5), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 840, 230, 35, 0, 620.0, 620.0)
                )));

        // 58. 12901 - Gujarat Mail (MMCT -> ADI)
        registerTrain(new Train(58L, "12901", "Gujarat Mail", TrainType.SUPERFAST,
                s.get("MMCT"), s.get("ADI"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(21, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(1, 13), LocalTime.of(1, 18), 5, 263, 2),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(3, 0), LocalTime.of(3, 5), 5, 392, 2),
                        new RouteHalt(4, s.get("ADI"), LocalTime.of(5, 50), null, 0, 492, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 2060.0, 2060.0),
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1240.0, 1240.0),
                        new CoachAvailability("3A", "GENERAL", 360, 82, 24, 0, 880.0, 880.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 330.0, 330.0)
                )));

        // 59. 12902 - Gujarat Mail (ADI -> MMCT)
        registerTrain(new Train(59L, "12902", "Gujarat Mail", TrainType.SUPERFAST,
                s.get("ADI"), s.get("MMCT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("ADI"), null, LocalTime.of(22, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("BRC"), LocalTime.of(0, 30), LocalTime.of(0, 35), 5, 100, 2),
                        new RouteHalt(3, s.get("ST"), LocalTime.of(2, 10), LocalTime.of(2, 15), 5, 229, 2),
                        new RouteHalt(4, s.get("MMCT"), LocalTime.of(6, 15), null, 0, 492, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 2060.0, 2060.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1240.0, 1240.0),
                        new CoachAvailability("3A", "GENERAL", 360, 78, 24, 0, 880.0, 880.0),
                        new CoachAvailability("SL", "GENERAL", 600, 185, 45, 0, 330.0, 330.0)
                )));

        // 60. 22953 - Gujarat Superfast Express (MMCT -> ADI)
        registerTrain(new Train(60L, "22953", "Gujarat Superfast Express", TrainType.SUPERFAST,
                s.get("MMCT"), s.get("ADI"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MMCT"), null, LocalTime.of(5, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("ST"), LocalTime.of(9, 42), LocalTime.of(9, 47), 5, 263, 1),
                        new RouteHalt(3, s.get("BRC"), LocalTime.of(11, 40), LocalTime.of(11, 45), 5, 392, 1),
                        new RouteHalt(4, s.get("ADI"), LocalTime.of(14, 30), null, 0, 492, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 95, 15, 0, 640.0, 640.0),
                        new CoachAvailability("2S", "GENERAL", 600, 260, 40, 0, 180.0, 180.0)
                )));

    }

    private void seedFleetBatch7(Map<String, Station> s) {
        // 61. 22229 - Mumbai CSMT - Madgaon Vande Bharat (CSMT -> MAO)
        registerTrain(new Train(61L, "22229", "Mumbai CSMT - Madgaon Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("CSMT"), s.get("MAO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(5, 25), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(8, 30), LocalTime.of(8, 35), 5, 192, 1),
                        new RouteHalt(3, s.get("MAO"), LocalTime.of(13, 10), null, 0, 580, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 155, 20, 0, 1435.0, 1435.0),
                        new CoachAvailability("EC", "GENERAL", 52, 24, 4, 0, 2680.0, 2680.0)
                )));

        // 62. 22230 - Madgaon - Mumbai CSMT Vande Bharat (MAO -> CSMT)
        registerTrain(new Train(62L, "22230", "Madgaon - Mumbai CSMT Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("MAO"), s.get("CSMT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAO"), null, LocalTime.of(14, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(19, 25), LocalTime.of(19, 30), 5, 388, 1),
                        new RouteHalt(3, s.get("CSMT"), LocalTime.of(22, 25), null, 0, 580, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 150, 20, 0, 1435.0, 1435.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2680.0, 2680.0)
                )));

        // 63. 12051 - Mumbai CSMT - Madgaon Jan Shatabdi (CSMT -> MAO)
        registerTrain(new Train(63L, "12051", "Mumbai CSMT - Madgaon Jan Shatabdi", TrainType.SHATABDI,
                s.get("CSMT"), s.get("MAO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(5, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(8, 15), LocalTime.of(8, 20), 5, 192, 1),
                        new RouteHalt(3, s.get("MAO"), LocalTime.of(14, 10), null, 0, 580, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 110, 15, 0, 980.0, 980.0),
                        new CoachAvailability("2S", "GENERAL", 600, 310, 40, 0, 295.0, 295.0)
                )));

        // 64. 12052 - Madgaon - Mumbai CSMT Jan Shatabdi (MAO -> CSMT)
        registerTrain(new Train(64L, "12052", "Madgaon - Mumbai CSMT Jan Shatabdi", TrainType.SHATABDI,
                s.get("MAO"), s.get("CSMT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("MAO"), null, LocalTime.of(15, 5), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(21, 5), LocalTime.of(21, 10), 5, 388, 1),
                        new RouteHalt(3, s.get("CSMT"), LocalTime.of(23, 55), null, 0, 580, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 105, 15, 0, 980.0, 980.0),
                        new CoachAvailability("2S", "GENERAL", 600, 300, 40, 0, 295.0, 295.0)
                )));

        // 65. 10103 - Mandovi Express (CSMT -> MAO)
        registerTrain(new Train(65L, "10103", "Mandovi Express", TrainType.EXPRESS,
                s.get("CSMT"), s.get("MAO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(7, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(10, 20), LocalTime.of(10, 25), 5, 192, 1),
                        new RouteHalt(3, s.get("MAO"), LocalTime.of(19, 10), null, 0, 580, 1)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1370.0, 1370.0),
                        new CoachAvailability("3A", "GENERAL", 380, 85, 26, 0, 960.0, 960.0),
                        new CoachAvailability("SL", "GENERAL", 640, 220, 48, 0, 360.0, 360.0)
                )));

        // 66. 12133 - Mumbai CSMT - Mangaluru Superfast (CSMT -> MAO)
        registerTrain(new Train(66L, "12133", "Mumbai CSMT - Mangaluru Superfast", TrainType.SUPERFAST,
                s.get("CSMT"), s.get("MAO"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(22, 2), 0, 0, 1),
                        new RouteHalt(2, s.get("PUNE"), LocalTime.of(1, 5), LocalTime.of(1, 10), 5, 192, 2),
                        new RouteHalt(3, s.get("MAO"), LocalTime.of(7, 0), null, 0, 580, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 20, 8, 0, 1370.0, 1370.0),
                        new CoachAvailability("3A", "GENERAL", 380, 78, 26, 0, 960.0, 960.0),
                        new CoachAvailability("SL", "GENERAL", 640, 195, 48, 0, 360.0, 360.0)
                )));

        // 67. 12394 - Sampoorna Kranti Superfast Express (NDLS -> PNBE)
        registerTrain(new Train(67L, "12394", "Sampoorna Kranti Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("PNBE"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(17, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(22, 22), LocalTime.of(22, 30), 8, 440, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(2, 25), LocalTime.of(2, 35), 10, 786, 2),
                        new RouteHalt(4, s.get("PNBE"), LocalTime.of(6, 50), null, 0, 998, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 3150.0, 3150.0),
                        new CoachAvailability("2A", "GENERAL", 96, 26, 8, 0, 1870.0, 1870.0),
                        new CoachAvailability("3A", "GENERAL", 360, 84, 24, 0, 1310.0, 1310.0),
                        new CoachAvailability("SL", "GENERAL", 600, 190, 45, 0, 490.0, 490.0)
                )));

        // 68. 12393 - Sampoorna Kranti Superfast Express (PNBE -> NDLS)
        registerTrain(new Train(68L, "12393", "Sampoorna Kranti Superfast Express", TrainType.SUPERFAST,
                s.get("PNBE"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("PNBE"), null, LocalTime.of(19, 25), 0, 0, 1),
                        new RouteHalt(2, s.get("DDU"), LocalTime.of(22, 20), LocalTime.of(22, 30), 10, 212, 1),
                        new RouteHalt(3, s.get("CNB"), LocalTime.of(2, 25), LocalTime.of(2, 30), 5, 558, 2),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(7, 55), null, 0, 998, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 3150.0, 3150.0),
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1870.0, 1870.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 1310.0, 1310.0),
                        new CoachAvailability("SL", "GENERAL", 600, 185, 45, 0, 490.0, 490.0)
                )));

        // 69. 12424 - Dibrugarh Rajdhani Express (NDLS -> GHY)
        registerTrain(new Train(69L, "12424", "Dibrugarh Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("GHY"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 20), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(21, 2), LocalTime.of(21, 7), 5, 440, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(1, 23), LocalTime.of(1, 33), 10, 788, 2),
                        new RouteHalt(4, s.get("PNBE"), LocalTime.of(4, 10), LocalTime.of(4, 20), 10, 1000, 2),
                        new RouteHalt(5, s.get("GHY"), LocalTime.of(19, 30), null, 0, 1880, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 5150.0, 5150.0),
                        new CoachAvailability("2A", "GENERAL", 120, 35, 12, 0, 3420.0, 3420.0),
                        new CoachAvailability("3A", "GENERAL", 360, 110, 28, 0, 2480.0, 2480.0)
                )));

        // 70. 12423 - Dibrugarh Rajdhani Express (GHY -> NDLS)
        registerTrain(new Train(70L, "12423", "Dibrugarh Rajdhani Express", TrainType.RAJDHANI,
                s.get("GHY"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("GHY"), null, LocalTime.of(6, 45), 0, 0, 1),
                        new RouteHalt(2, s.get("PNBE"), LocalTime.of(21, 40), LocalTime.of(21, 50), 10, 880, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(0, 45), LocalTime.of(0, 55), 10, 1092, 2),
                        new RouteHalt(4, s.get("CNB"), LocalTime.of(5, 0), LocalTime.of(5, 5), 5, 1440, 2),
                        new RouteHalt(5, s.get("NDLS"), LocalTime.of(10, 30), null, 0, 1880, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 5150.0, 5150.0),
                        new CoachAvailability("2A", "GENERAL", 120, 32, 12, 0, 3420.0, 3420.0),
                        new CoachAvailability("3A", "GENERAL", 360, 105, 28, 0, 2480.0, 2480.0)
                )));

    }

    private void seedFleetBatch8(Map<String, Station> s) {
        // 71. 12392 - Shramjeevi Superfast Express (NDLS -> PNBE)
        registerTrain(new Train(71L, "12392", "Shramjeevi Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("PNBE"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(13, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("GZB"), LocalTime.of(13, 51), LocalTime.of(13, 53), 2, 26, 1),
                        new RouteHalt(3, s.get("LKO"), LocalTime.of(21, 20), LocalTime.of(21, 30), 10, 512, 1),
                        new RouteHalt(4, s.get("BSB"), LocalTime.of(2, 35), LocalTime.of(2, 45), 10, 795, 2),
                        new RouteHalt(5, s.get("DDU"), LocalTime.of(3, 35), LocalTime.of(3, 45), 10, 813, 2),
                        new RouteHalt(6, s.get("PNBE"), LocalTime.of(7, 5), null, 0, 1025, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1870.0, 1870.0),
                        new CoachAvailability("3A", "GENERAL", 380, 88, 26, 0, 1310.0, 1310.0),
                        new CoachAvailability("SL", "GENERAL", 640, 210, 48, 0, 490.0, 490.0)
                )));

        // 72. 12566 - Bihar Sampark Kranti Express (NDLS -> PNBE)
        registerTrain(new Train(72L, "12566", "Bihar Sampark Kranti Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("PNBE"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(13, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("CNB"), LocalTime.of(18, 10), LocalTime.of(18, 15), 5, 440, 1),
                        new RouteHalt(3, s.get("DDU"), LocalTime.of(23, 25), LocalTime.of(23, 35), 10, 788, 1),
                        new RouteHalt(4, s.get("PNBE"), LocalTime.of(5, 20), null, 0, 1000, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 20, 8, 0, 1870.0, 1870.0),
                        new CoachAvailability("3A", "GENERAL", 380, 82, 26, 0, 1310.0, 1310.0),
                        new CoachAvailability("SL", "GENERAL", 640, 195, 48, 0, 490.0, 490.0)
                )));

        // 73. 12046 - Chandigarh Shatabdi Express (CDG -> NDLS)
        registerTrain(new Train(73L, "12046", "Chandigarh Shatabdi Express", TrainType.SHATABDI,
                s.get("CDG"), s.get("NDLS"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CDG"), null, LocalTime.of(12, 5), 0, 0, 1),
                        new RouteHalt(2, s.get("NDLS"), LocalTime.of(15, 20), null, 0, 244, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 130, 18, 0, 740.0, 740.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 1420.0, 1420.0)
                )));

        // 74. 12045 - Chandigarh Shatabdi Express (NDLS -> CDG)
        registerTrain(new Train(74L, "12045", "Chandigarh Shatabdi Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("CDG"), "1111110", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(19, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(22, 35), null, 0, 244, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 125, 18, 0, 740.0, 740.0),
                        new CoachAvailability("EC", "GENERAL", 48, 14, 4, 0, 1420.0, 1420.0)
                )));

        // 75. 12011 - Kalka Shatabdi Express (NDLS -> CDG)
        registerTrain(new Train(75L, "12011", "Kalka Shatabdi Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("CDG"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(7, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(10, 59), null, 0, 244, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 140, 18, 0, 740.0, 740.0),
                        new CoachAvailability("EC", "GENERAL", 48, 16, 4, 0, 1420.0, 1420.0)
                )));

        // 76. 12012 - Kalka Shatabdi Express (CDG -> NDLS)
        registerTrain(new Train(76L, "12012", "Kalka Shatabdi Express", TrainType.SHATABDI,
                s.get("CDG"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CDG"), null, LocalTime.of(18, 23), 0, 0, 1),
                        new RouteHalt(2, s.get("NDLS"), LocalTime.of(21, 50), null, 0, 244, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 135, 18, 0, 740.0, 740.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 1420.0, 1420.0)
                )));

        // 77. 22447 - Amb Andaura Vande Bharat (NDLS -> CDG)
        registerTrain(new Train(77L, "22447", "Amb Andaura Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("CDG"), "1111011", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(5, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(8, 38), null, 0, 244, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 165, 20, 0, 920.0, 920.0),
                        new CoachAvailability("EC", "GENERAL", 52, 26, 4, 0, 1780.0, 1780.0)
                )));

        // 78. 12029 - Swarna Shatabdi Express (NDLS -> ASR)
        registerTrain(new Train(78L, "12029", "Swarna Shatabdi Express", TrainType.SHATABDI,
                s.get("NDLS"), s.get("ASR"), "1110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(7, 20), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(10, 20), LocalTime.of(10, 25), 5, 244, 1),
                        new RouteHalt(3, s.get("ASR"), LocalTime.of(13, 30), null, 0, 448, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 120, 18, 0, 1020.0, 1020.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 1950.0, 1950.0)
                )));

        // 79. 12497 - Shan-e-Punjab Express (NDLS -> ASR)
        registerTrain(new Train(79L, "12497", "Shan-e-Punjab Express", TrainType.EXPRESS,
                s.get("NDLS"), s.get("ASR"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(9, 55), LocalTime.of(10, 5), 10, 244, 1),
                        new RouteHalt(3, s.get("ASR"), LocalTime.of(14, 15), null, 0, 448, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 95, 15, 0, 610.0, 610.0),
                        new CoachAvailability("2S", "GENERAL", 600, 290, 40, 0, 185.0, 185.0)
                )));

        // 80. 22439 - Vande Bharat Express (Katra) (NDLS -> JAT)
        registerTrain(new Train(80L, "22439", "Vande Bharat Express (Katra)", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("JAT"), "1011111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(8, 20), LocalTime.of(8, 22), 2, 244, 1),
                        new RouteHalt(3, s.get("JAT"), LocalTime.of(12, 38), null, 0, 577, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 160, 20, 0, 1550.0, 1550.0),
                        new CoachAvailability("EC", "GENERAL", 52, 25, 4, 0, 2980.0, 2980.0)
                )));

    }

    private void seedFleetBatch9(Map<String, Station> s) {
        // 81. 22440 - Vande Bharat Express (JAT -> NDLS)
        registerTrain(new Train(81L, "22440", "Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("JAT"), s.get("NDLS"), "1011111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("JAT"), null, LocalTime.of(15, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(19, 15), LocalTime.of(19, 17), 2, 333, 1),
                        new RouteHalt(3, s.get("NDLS"), LocalTime.of(23, 0), null, 0, 577, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 155, 20, 0, 1550.0, 1550.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2980.0, 2980.0)
                )));

        // 82. 12425 - Jammu Tawi Rajdhani Express (NDLS -> JAT)
        registerTrain(new Train(82L, "12425", "Jammu Tawi Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("JAT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(20, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("JAT"), LocalTime.of(5, 0), null, 0, 577, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 3250.0, 3250.0),
                        new CoachAvailability("2A", "GENERAL", 120, 30, 12, 0, 2120.0, 2120.0),
                        new CoachAvailability("3A", "GENERAL", 360, 85, 28, 0, 1480.0, 1480.0)
                )));

        // 83. 12445 - Uttar Sampark Kranti Express (NDLS -> JAT)
        registerTrain(new Train(83L, "12445", "Uttar Sampark Kranti Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("JAT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(20, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(0, 5), LocalTime.of(0, 15), 10, 244, 2),
                        new RouteHalt(3, s.get("JAT"), LocalTime.of(5, 55), null, 0, 577, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 2280.0, 2280.0),
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 1370.0, 1370.0),
                        new CoachAvailability("3A", "GENERAL", 360, 82, 24, 0, 960.0, 960.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 365.0, 365.0)
                )));

        // 84. 20172 - Rani Kamlapati Vande Bharat (NDLS -> BPL)
        registerTrain(new Train(84L, "20172", "Rani Kamlapati Vande Bharat", TrainType.VANDE_BHARAT,
                s.get("NDLS"), s.get("BPL"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(14, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(16, 20), LocalTime.of(16, 22), 2, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(17, 28), LocalTime.of(17, 30), 2, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(22, 10), null, 0, 705, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 155, 20, 0, 1665.0, 1665.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 3120.0, 3120.0)
                )));

        // 85. 20171 - Vande Bharat Express (BPL -> NDLS)
        registerTrain(new Train(85L, "20171", "Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("BPL"), s.get("NDLS"), "1111101", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("BPL"), null, LocalTime.of(5, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("GWL"), LocalTime.of(10, 15), LocalTime.of(10, 17), 2, 392, 1),
                        new RouteHalt(3, s.get("AGC"), LocalTime.of(11, 23), LocalTime.of(11, 25), 2, 510, 1),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(13, 10), null, 0, 705, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 150, 20, 0, 1665.0, 1665.0),
                        new CoachAvailability("EC", "GENERAL", 52, 20, 4, 0, 3120.0, 3120.0)
                )));

        // 86. 12002 - New Delhi - Bhopal Shatabdi (NDLS -> BPL)
        registerTrain(new Train(86L, "12002", "New Delhi - Bhopal Shatabdi", TrainType.SHATABDI,
                s.get("NDLS"), s.get("BPL"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(7, 50), LocalTime.of(7, 55), 5, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(9, 23), LocalTime.of(9, 25), 2, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(14, 40), null, 0, 705, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 134, 18, 0, 1350.0, 1350.0),
                        new CoachAvailability("EC", "GENERAL", 48, 16, 4, 0, 2400.0, 2400.0)
                )));

        // 87. 12001 - Bhopal Shatabdi Express (BPL -> NDLS)
        registerTrain(new Train(87L, "12001", "Bhopal Shatabdi Express", TrainType.SHATABDI,
                s.get("BPL"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("BPL"), null, LocalTime.of(15, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("GWL"), LocalTime.of(20, 0), LocalTime.of(20, 2), 2, 392, 1),
                        new RouteHalt(3, s.get("AGC"), LocalTime.of(21, 15), LocalTime.of(21, 20), 5, 510, 1),
                        new RouteHalt(4, s.get("NDLS"), LocalTime.of(23, 50), null, 0, 705, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 128, 18, 0, 1350.0, 1350.0),
                        new CoachAvailability("EC", "GENERAL", 48, 15, 4, 0, 2400.0, 2400.0)
                )));

        // 88. 12920 - Malwa Superfast Express (NDLS -> INDB)
        registerTrain(new Train(88L, "12920", "Malwa Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("INDB"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(20, 30), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(23, 15), LocalTime.of(23, 20), 5, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(0, 56), LocalTime.of(0, 58), 2, 313, 2),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(7, 45), LocalTime.of(7, 55), 10, 705, 2),
                        new RouteHalt(5, s.get("INDB"), LocalTime.of(12, 50), null, 0, 930, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 3080.0, 3080.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 1820.0, 1820.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 1280.0, 1280.0),
                        new CoachAvailability("SL", "GENERAL", 600, 190, 45, 0, 480.0, 480.0)
                )));

        // 89. 22895 - Howrah - Puri Vande Bharat Express (HWH -> PURI)
        registerTrain(new Train(89L, "22895", "Howrah - Puri Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("HWH"), s.get("PURI"), "1110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(6, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("BBS"), LocalTime.of(11, 20), LocalTime.of(11, 24), 4, 437, 1),
                        new RouteHalt(3, s.get("PURI"), LocalTime.of(12, 35), null, 0, 500, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 160, 20, 0, 1265.0, 1265.0),
                        new CoachAvailability("EC", "GENERAL", 52, 24, 4, 0, 2420.0, 2420.0)
                )));

        // 90. 22896 - Puri - Howrah Vande Bharat Express (PURI -> HWH)
        registerTrain(new Train(90L, "22896", "Puri - Howrah Vande Bharat Express", TrainType.VANDE_BHARAT,
                s.get("PURI"), s.get("HWH"), "1110111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("PURI"), null, LocalTime.of(13, 50), 0, 0, 1),
                        new RouteHalt(2, s.get("BBS"), LocalTime.of(14, 45), LocalTime.of(14, 49), 4, 63, 1),
                        new RouteHalt(3, s.get("HWH"), LocalTime.of(20, 30), null, 0, 500, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 520, 155, 20, 0, 1265.0, 1265.0),
                        new CoachAvailability("EC", "GENERAL", 52, 22, 4, 0, 2420.0, 2420.0)
                )));

    }

    private void seedFleetBatch10(Map<String, Station> s) {
        // 91. 12837 - Howrah - Puri Superfast Express (HWH -> PURI)
        registerTrain(new Train(91L, "12837", "Howrah - Puri Superfast Express", TrainType.SUPERFAST,
                s.get("HWH"), s.get("PURI"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(22, 40), 0, 0, 1),
                        new RouteHalt(2, s.get("BBS"), LocalTime.of(5, 45), LocalTime.of(5, 50), 5, 437, 2),
                        new RouteHalt(3, s.get("PURI"), LocalTime.of(7, 10), null, 0, 500, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 2040.0, 2040.0),
                        new CoachAvailability("2A", "GENERAL", 96, 25, 8, 0, 1220.0, 1220.0),
                        new CoachAvailability("3A", "GENERAL", 360, 84, 24, 0, 860.0, 860.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 325.0, 325.0)
                )));

        // 92. 12277 - Howrah - Puri Shatabdi Express (HWH -> PURI)
        registerTrain(new Train(92L, "12277", "Howrah - Puri Shatabdi Express", TrainType.SHATABDI,
                s.get("HWH"), s.get("PURI"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(14, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("BBS"), LocalTime.of(20, 10), LocalTime.of(20, 15), 5, 437, 1),
                        new RouteHalt(3, s.get("PURI"), LocalTime.of(21, 50), null, 0, 500, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 480, 125, 18, 0, 1020.0, 1020.0),
                        new CoachAvailability("EC", "GENERAL", 48, 14, 4, 0, 1940.0, 1940.0)
                )));

        // 93. 12821 - Dhauli Superfast Express (HWH -> PURI)
        registerTrain(new Train(93L, "12821", "Dhauli Superfast Express", TrainType.SUPERFAST,
                s.get("HWH"), s.get("PURI"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("HWH"), null, LocalTime.of(9, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("BBS"), LocalTime.of(16, 0), LocalTime.of(16, 5), 5, 437, 1),
                        new RouteHalt(3, s.get("PURI"), LocalTime.of(18, 0), null, 0, 500, 1)
                ),
                List.of(
                        new CoachAvailability("CC", "GENERAL", 380, 90, 15, 0, 680.0, 680.0),
                        new CoachAvailability("2S", "GENERAL", 600, 275, 40, 0, 195.0, 195.0)
                )));

        // 94. 12724 - Telangana Superfast Express (NDLS -> HYB)
        registerTrain(new Train(94L, "12724", "Telangana Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("HYB"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 0), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(18, 5), LocalTime.of(18, 7), 2, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(19, 28), LocalTime.of(19, 30), 2, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(1, 20), LocalTime.of(1, 30), 10, 705, 2),
                        new RouteHalt(5, s.get("NGP"), LocalTime.of(7, 10), LocalTime.of(7, 15), 5, 1090, 2),
                        new RouteHalt(6, s.get("SC"), LocalTime.of(16, 15), LocalTime.of(16, 20), 5, 1665, 2),
                        new RouteHalt(7, s.get("HYB"), LocalTime.of(17, 10), null, 0, 1675, 2)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 24, 8, 0, 2680.0, 2680.0),
                        new CoachAvailability("3A", "GENERAL", 380, 85, 26, 0, 1840.0, 1840.0),
                        new CoachAvailability("SL", "GENERAL", 640, 210, 48, 0, 690.0, 690.0)
                )));

        // 95. 12438 - Secunderabad Rajdhani Express (NDLS -> SC)
        registerTrain(new Train(95L, "12438", "Secunderabad Rajdhani Express", TrainType.RAJDHANI,
                s.get("NDLS"), s.get("SC"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(6, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(7, 55), LocalTime.of(7, 57), 2, 195, 1),
                        new RouteHalt(3, s.get("BPL"), LocalTime.of(12, 55), LocalTime.of(13, 0), 5, 705, 1),
                        new RouteHalt(4, s.get("NGP"), LocalTime.of(18, 20), LocalTime.of(18, 25), 5, 1090, 1),
                        new RouteHalt(5, s.get("SC"), LocalTime.of(6, 50), null, 0, 1665, 2)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 7, 0, 0, 5100.0, 5100.0),
                        new CoachAvailability("2A", "GENERAL", 120, 34, 12, 0, 3350.0, 3350.0),
                        new CoachAvailability("3A", "GENERAL", 360, 95, 28, 0, 2420.0, 2420.0)
                )));

        // 96. 12616 - Grand Trunk (GT) Express (NDLS -> MAS)
        registerTrain(new Train(96L, "12616", "Grand Trunk (GT) Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("MAS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(16, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(18, 30), LocalTime.of(18, 35), 5, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(20, 0), LocalTime.of(20, 5), 5, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(3, 15), LocalTime.of(3, 25), 10, 705, 2),
                        new RouteHalt(5, s.get("NGP"), LocalTime.of(10, 25), LocalTime.of(10, 30), 5, 1090, 2),
                        new RouteHalt(6, s.get("BZA"), LocalTime.of(21, 40), LocalTime.of(21, 50), 10, 1754, 2),
                        new RouteHalt(7, s.get("MAS"), LocalTime.of(4, 30), null, 0, 2185, 3)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 6, 0, 0, 5180.0, 5180.0),
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 3050.0, 3050.0),
                        new CoachAvailability("3A", "GENERAL", 360, 80, 24, 0, 2090.0, 2090.0),
                        new CoachAvailability("SL", "GENERAL", 600, 195, 45, 0, 790.0, 790.0)
                )));

        // 97. 12622 - Tamil Nadu Superfast Express (NDLS -> MAS)
        registerTrain(new Train(97L, "12622", "Tamil Nadu Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("MAS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(21, 5), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(23, 25), LocalTime.of(23, 30), 5, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(1, 13), LocalTime.of(1, 15), 2, 313, 2),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(6, 45), LocalTime.of(6, 55), 10, 705, 2),
                        new RouteHalt(5, s.get("NGP"), LocalTime.of(13, 5), LocalTime.of(13, 10), 5, 1090, 2),
                        new RouteHalt(6, s.get("BZA"), LocalTime.of(23, 15), LocalTime.of(23, 25), 10, 1754, 2),
                        new RouteHalt(7, s.get("MAS"), LocalTime.of(6, 35), null, 0, 2185, 3)
                ),
                List.of(
                        new CoachAvailability("1A", "GENERAL", 24, 5, 0, 0, 5180.0, 5180.0),
                        new CoachAvailability("2A", "GENERAL", 96, 20, 8, 0, 3050.0, 3050.0),
                        new CoachAvailability("3A", "GENERAL", 360, 76, 24, 0, 2090.0, 2090.0),
                        new CoachAvailability("SL", "GENERAL", 600, 185, 45, 0, 790.0, 790.0)
                )));

        // 98. 12626 - Kerala Superfast Express (NDLS -> TVC)
        registerTrain(new Train(98L, "12626", "Kerala Superfast Express", TrainType.SUPERFAST,
                s.get("NDLS"), s.get("TVC"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("NDLS"), null, LocalTime.of(20, 10), 0, 0, 1),
                        new RouteHalt(2, s.get("AGC"), LocalTime.of(22, 20), LocalTime.of(22, 25), 5, 195, 1),
                        new RouteHalt(3, s.get("GWL"), LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 313, 1),
                        new RouteHalt(4, s.get("BPL"), LocalTime.of(5, 20), LocalTime.of(5, 25), 5, 705, 2),
                        new RouteHalt(5, s.get("NGP"), LocalTime.of(11, 45), LocalTime.of(11, 50), 5, 1090, 2),
                        new RouteHalt(6, s.get("BZA"), LocalTime.of(22, 15), LocalTime.of(22, 25), 10, 1754, 2),
                        new RouteHalt(7, s.get("MAS"), LocalTime.of(4, 30), LocalTime.of(4, 55), 25, 2185, 3),
                        new RouteHalt(8, s.get("KPD"), LocalTime.of(6, 48), LocalTime.of(6, 50), 2, 2315, 3),
                        new RouteHalt(9, s.get("ERS"), LocalTime.of(14, 15), LocalTime.of(14, 20), 5, 2800, 3),
                        new RouteHalt(10, s.get("TVC"), LocalTime.of(18, 0), null, 0, 3036, 3)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 28, 8, 0, 3680.0, 3680.0),
                        new CoachAvailability("3A", "GENERAL", 380, 95, 26, 0, 2520.0, 2520.0),
                        new CoachAvailability("SL", "GENERAL", 640, 210, 48, 0, 960.0, 960.0)
                )));

        // 99. 12625 - Kerala Superfast Express (TVC -> NDLS)
        registerTrain(new Train(99L, "12625", "Kerala Superfast Express", TrainType.SUPERFAST,
                s.get("TVC"), s.get("NDLS"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("TVC"), null, LocalTime.of(11, 15), 0, 0, 1),
                        new RouteHalt(2, s.get("ERS"), LocalTime.of(15, 35), LocalTime.of(15, 40), 5, 236, 1),
                        new RouteHalt(3, s.get("KPD"), LocalTime.of(22, 58), LocalTime.of(23, 0), 2, 721, 1),
                        new RouteHalt(4, s.get("MAS"), LocalTime.of(0, 50), LocalTime.of(1, 15), 25, 851, 2),
                        new RouteHalt(5, s.get("BZA"), LocalTime.of(7, 10), LocalTime.of(7, 20), 10, 1282, 2),
                        new RouteHalt(6, s.get("NGP"), LocalTime.of(17, 40), LocalTime.of(17, 45), 5, 1946, 2),
                        new RouteHalt(7, s.get("BPL"), LocalTime.of(23, 55), LocalTime.of(0, 5), 10, 2331, 2),
                        new RouteHalt(8, s.get("GWL"), LocalTime.of(5, 28), LocalTime.of(5, 30), 2, 2723, 3),
                        new RouteHalt(9, s.get("AGC"), LocalTime.of(7, 10), LocalTime.of(7, 15), 5, 2841, 3),
                        new RouteHalt(10, s.get("NDLS"), LocalTime.of(10, 25), null, 0, 3036, 3)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 26, 8, 0, 3680.0, 3680.0),
                        new CoachAvailability("3A", "GENERAL", 380, 90, 26, 0, 2520.0, 2520.0),
                        new CoachAvailability("SL", "GENERAL", 640, 205, 48, 0, 960.0, 960.0)
                )));

        // 100. 12138 - Punjab Mail Express (ASR -> CSMT)
        registerTrain(new Train(100L, "12138", "Punjab Mail Express", TrainType.EXPRESS,
                s.get("ASR"), s.get("CSMT"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("ASR"), null, LocalTime.of(21, 45), 0, 0, 1),
                        new RouteHalt(2, s.get("CDG"), LocalTime.of(1, 40), LocalTime.of(1, 50), 10, 230, 2),
                        new RouteHalt(3, s.get("NDLS"), LocalTime.of(5, 0), LocalTime.of(5, 15), 15, 490, 2),
                        new RouteHalt(4, s.get("AGC"), LocalTime.of(7, 45), LocalTime.of(7, 50), 5, 685, 2),
                        new RouteHalt(5, s.get("GWL"), LocalTime.of(9, 12), LocalTime.of(9, 14), 2, 803, 2),
                        new RouteHalt(6, s.get("BPL"), LocalTime.of(16, 30), LocalTime.of(16, 40), 10, 1195, 2),
                        new RouteHalt(7, s.get("NGP"), LocalTime.of(23, 25), LocalTime.of(23, 35), 10, 1580, 2),
                        new RouteHalt(8, s.get("CSMT"), LocalTime.of(7, 35), null, 0, 1925, 3)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 25, 8, 0, 2790.0, 2790.0),
                        new CoachAvailability("3A", "GENERAL", 380, 92, 26, 0, 1920.0, 1920.0),
                        new CoachAvailability("SL", "GENERAL", 640, 230, 48, 0, 720.0, 720.0)
                )));

    }

    private void seedFleetBatch11(Map<String, Station> s) {
        // 101. 12137 - Punjab Mail Express (CSMT -> ASR)
        registerTrain(new Train(101L, "12137", "Punjab Mail Express", TrainType.EXPRESS,
                s.get("CSMT"), s.get("ASR"), "1111111", TrainStatus.ON_TIME,
                List.of(
                        new RouteHalt(1, s.get("CSMT"), null, LocalTime.of(19, 35), 0, 0, 1),
                        new RouteHalt(2, s.get("NGP"), LocalTime.of(3, 45), LocalTime.of(3, 55), 10, 345, 2),
                        new RouteHalt(3, s.get("BPL"), LocalTime.of(10, 20), LocalTime.of(10, 30), 10, 730, 2),
                        new RouteHalt(4, s.get("GWL"), LocalTime.of(17, 35), LocalTime.of(17, 37), 2, 1122, 2),
                        new RouteHalt(5, s.get("AGC"), LocalTime.of(19, 10), LocalTime.of(19, 15), 5, 1240, 2),
                        new RouteHalt(6, s.get("NDLS"), LocalTime.of(21, 30), LocalTime.of(21, 50), 20, 1435, 2),
                        new RouteHalt(7, s.get("CDG"), LocalTime.of(1, 10), LocalTime.of(1, 20), 10, 1695, 3),
                        new RouteHalt(8, s.get("ASR"), LocalTime.of(5, 10), null, 0, 1925, 3)
                ),
                List.of(
                        new CoachAvailability("2A", "GENERAL", 96, 22, 8, 0, 2790.0, 2790.0),
                        new CoachAvailability("3A", "GENERAL", 380, 88, 26, 0, 1920.0, 1920.0),
                        new CoachAvailability("SL", "GENERAL", 640, 220, 48, 0, 720.0, 720.0)
                )));

    }


    private static void registerTrain(Train t) {
        IN_MEMORY_TRAINS.put(t.getTrainNumber(), t);
    }
}
