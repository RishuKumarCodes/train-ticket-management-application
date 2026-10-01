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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final StationDAO stationDAO;

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
     * Retrieves full train details including all route halts and seat classes by train number.
     */
    public Optional<Train> getTrainByNumber(String trainNumber) {
        if (trainNumber == null) return Optional.empty();
        String cleanNum = trainNumber.trim();
        return Optional.ofNullable(IN_MEMORY_TRAINS.get(cleanNum));
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

    // ─────────────────────────────────────────────────────────────────────────
    // In-Memory Seed Initialization
    // ─────────────────────────────────────────────────────────────────────────

    private synchronized void ensureSeedData() {
        if (!IN_MEMORY_TRAINS.isEmpty()) return;

        Station ndls = stationDAO.findByCode("NDLS").orElseThrow();
        Station mmct = stationDAO.findByCode("MMCT").orElseThrow();
        Station csmt = stationDAO.findByCode("CSMT").orElseThrow();
        Station bsb  = stationDAO.findByCode("BSB").orElseThrow();
        Station lko  = stationDAO.findByCode("LKO").orElseThrow();
        Station hwh  = stationDAO.findByCode("HWH").orElseThrow();
        Station mas  = stationDAO.findByCode("MAS").orElseThrow();
        Station mys  = stationDAO.findByCode("MYS").orElseThrow();
        Station sbc  = stationDAO.findByCode("SBC").orElseThrow();
        Station kota = stationDAO.findByCode("KOTA").orElseThrow();
        Station brc  = stationDAO.findByCode("BRC").orElseThrow();
        Station st   = stationDAO.findByCode("ST").orElseThrow();
        Station cnb  = stationDAO.findByCode("CNB").orElseThrow();
        Station pryj = stationDAO.findByCode("PRYJ").orElseThrow();
        Station ddu  = stationDAO.findByCode("DDU").orElseThrow();
        Station gzb  = stationDAO.findByCode("GZB").orElseThrow();
        Station aljn = stationDAO.findByCode("ALJN").orElseThrow();
        Station kpd  = stationDAO.findByCode("KPD").orElseThrow();
        Station jp   = stationDAO.findByCode("JP").orElseThrow();
        Station adi  = stationDAO.findByCode("ADI").orElseThrow();
        Station hyb  = stationDAO.findByCode("HYB").orElseThrow();
        Station sc   = stationDAO.findByCode("SC").orElseThrow();
        Station bpl  = stationDAO.findByCode("BPL").orElseThrow();
        Station cdg  = stationDAO.findByCode("CDG").orElseThrow();
        Station asr  = stationDAO.findByCode("ASR").orElseThrow();
        Station pnbe = stationDAO.findByCode("PNBE").orElseThrow();
        Station ghy  = stationDAO.findByCode("GHY").orElseThrow();
        Station tvc  = stationDAO.findByCode("TVC").orElseThrow();
        Station ers  = stationDAO.findByCode("ERS").orElseThrow();
        Station agc  = stationDAO.findByCode("AGC").orElseThrow();
        Station gwl  = stationDAO.findByCode("GWL").orElseThrow();
        Station ngp  = stationDAO.findByCode("NGP").orElseThrow();
        Station bza  = stationDAO.findByCode("BZA").orElseThrow();
        Station cbe  = stationDAO.findByCode("CBE").orElseThrow();

        // 1. 12952: New Delhi Tejas Rajdhani Express (NDLS -> MMCT)
        List<RouteHalt> halts12952 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(16, 55), 0, 0, 1),
                new RouteHalt(2, kota, LocalTime.of(21, 30), LocalTime.of(21, 40), 10, 465, 1),
                new RouteHalt(3, brc,  LocalTime.of(3, 15), LocalTime.of(3, 25), 10, 992, 2),
                new RouteHalt(4, st,   LocalTime.of(4, 50), LocalTime.of(4, 55), 5, 1122, 2),
                new RouteHalt(5, mmct, LocalTime.of(8, 35), null, 0, 1384, 2)
        );
        List<CoachAvailability> classes12952 = List.of(
                new CoachAvailability("1A", "GENERAL", 24, 8, 0, 0, 4280.0, 4280.0),
                new CoachAvailability("2A", "GENERAL", 120, 36, 12, 0, 2850.0, 2850.0),
                new CoachAvailability("3A", "GENERAL", 360, 94, 28, 0, 2080.0, 2080.0)
        );
        registerTrain(new Train(1L, "12952", "New Delhi Tejas Rajdhani Express", TrainType.RAJDHANI, 
                ndls, mmct, "1111111", TrainStatus.ON_TIME, halts12952, classes12952));

        // 2. 12951: Mumbai Tejas Rajdhani Express (MMCT -> NDLS)
        List<RouteHalt> halts12951 = List.of(
                new RouteHalt(1, mmct, null, LocalTime.of(17, 0), 0, 0, 1),
                new RouteHalt(2, st,   LocalTime.of(19, 43), LocalTime.of(19, 48), 5, 263, 1),
                new RouteHalt(3, brc,  LocalTime.of(21, 6), LocalTime.of(21, 16), 10, 392, 1),
                new RouteHalt(4, kota, LocalTime.of(3, 15), LocalTime.of(3, 20), 5, 919, 2),
                new RouteHalt(5, ndls, LocalTime.of(8, 32), null, 0, 1384, 2)
        );
        registerTrain(new Train(2L, "12951", "Mumbai Tejas Rajdhani Express", TrainType.RAJDHANI, 
                mmct, ndls, "1111111", TrainStatus.ON_TIME, halts12951, classes12952));

        // 3. 22436: Vande Bharat Express (NDLS -> BSB)
        List<RouteHalt> halts22436 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(6, 0), 0, 0, 1),
                new RouteHalt(2, cnb,  LocalTime.of(10, 10), LocalTime.of(10, 14), 4, 440, 1),
                new RouteHalt(3, pryj, LocalTime.of(12, 8), LocalTime.of(12, 10), 2, 635, 1),
                new RouteHalt(4, bsb,  LocalTime.of(14, 0), null, 0, 759, 1)
        );
        List<CoachAvailability> classes22436 = List.of(
                new CoachAvailability("CC", "GENERAL", 520, 142, 18, 0, 1750.0, 1750.0),
                new CoachAvailability("EC", "GENERAL", 52, 19, 0, 0, 3300.0, 3300.0)
        );
        registerTrain(new Train(3L, "22436", "Vande Bharat Express", TrainType.VANDE_BHARAT, 
                ndls, bsb, "0110111", TrainStatus.ON_TIME, halts22436, classes22436));

        // 4. 22435: Vande Bharat Express (BSB -> NDLS)
        List<RouteHalt> halts22435 = List.of(
                new RouteHalt(1, bsb,  null, LocalTime.of(15, 0), 0, 0, 1),
                new RouteHalt(2, pryj, LocalTime.of(16, 30), LocalTime.of(16, 32), 2, 124, 1),
                new RouteHalt(3, cnb,  LocalTime.of(18, 30), LocalTime.of(18, 34), 4, 319, 1),
                new RouteHalt(4, ndls, LocalTime.of(23, 0), null, 0, 759, 1)
        );
        registerTrain(new Train(4L, "22435", "Vande Bharat Express", TrainType.VANDE_BHARAT, 
                bsb, ndls, "0110111", TrainStatus.ON_TIME, halts22435, classes22436));

        // 5. 12004: Lucknow Shatabdi Express (NDLS -> LKO)
        List<RouteHalt> halts12004 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(6, 10), 0, 0, 1),
                new RouteHalt(2, gzb,  LocalTime.of(6, 45), LocalTime.of(6, 47), 2, 26, 1),
                new RouteHalt(3, aljn, LocalTime.of(7, 47), LocalTime.of(7, 49), 2, 131, 1),
                new RouteHalt(4, cnb,  LocalTime.of(11, 20), LocalTime.of(11, 25), 5, 440, 1),
                new RouteHalt(5, lko,  LocalTime.of(12, 40), null, 0, 512, 1)
        );
        List<CoachAvailability> classes12004 = List.of(
                new CoachAvailability("CC", "GENERAL", 450, 115, 14, 0, 1165.0, 1165.0),
                new CoachAvailability("EC", "GENERAL", 46, 12, 0, 0, 2125.0, 2125.0)
        );
        registerTrain(new Train(5L, "12004", "Lucknow Shatabdi Express", TrainType.SHATABDI, 
                ndls, lko, "1111111", TrainStatus.ON_TIME, halts12004, classes12004));

        // 6. 12302: Howrah Rajdhani Express (NDLS -> HWH)
        List<RouteHalt> halts12302 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(16, 50), 0, 0, 1),
                new RouteHalt(2, cnb,  LocalTime.of(21, 32), LocalTime.of(21, 37), 5, 440, 1),
                new RouteHalt(3, pryj, LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 635, 1),
                new RouteHalt(4, ddu,  LocalTime.of(1, 37), LocalTime.of(1, 47), 10, 788, 2),
                new RouteHalt(5, hwh,  LocalTime.of(9, 55), null, 0, 1451, 2)
        );
        List<CoachAvailability> classes12302 = List.of(
                new CoachAvailability("1A", "GENERAL", 24, 2, 0, 0, 4650.0, 4650.0),
                new CoachAvailability("2A", "GENERAL", 120, 42, 8, 0, 3050.0, 3050.0),
                new CoachAvailability("3A", "GENERAL", 360, 110, 20, 0, 2220.0, 2220.0)
        );
        registerTrain(new Train(6L, "12302", "Howrah Rajdhani Express", TrainType.RAJDHANI, 
                ndls, hwh, "1111111", TrainStatus.DEPARTED, halts12302, classes12302));

        // 7. 20608: Vande Bharat Express (MAS -> MYS)
        List<RouteHalt> halts20608 = List.of(
                new RouteHalt(1, mas, null, LocalTime.of(5, 50), 0, 0, 1),
                new RouteHalt(2, kpd, LocalTime.of(7, 13), LocalTime.of(7, 15), 2, 130, 1),
                new RouteHalt(3, sbc, LocalTime.of(10, 15), LocalTime.of(10, 20), 5, 359, 1),
                new RouteHalt(4, mys, LocalTime.of(12, 20), null, 0, 497, 1)
        );
        List<CoachAvailability> classes20608 = List.of(
                new CoachAvailability("CC", "GENERAL", 520, 168, 24, 0, 1200.0, 1200.0),
                new CoachAvailability("EC", "GENERAL", 52, 22, 0, 0, 2295.0, 2295.0)
        );
        registerTrain(new Train(7L, "20608", "Vande Bharat Express", TrainType.VANDE_BHARAT, 
                mas, mys, "1111101", TrainStatus.ON_TIME, halts20608, classes20608));

        // Standard coach classes for Express / Superfast fleet
        List<CoachAvailability> expressClasses = List.of(
                new CoachAvailability("2A", "GENERAL", 96, 28, 6, 0, 2450.0, 2450.0),
                new CoachAvailability("3A", "GENERAL", 380, 112, 24, 0, 1720.0, 1720.0),
                new CoachAvailability("SL", "GENERAL", 640, 210, 45, 0, 680.0, 680.0)
        );
        List<CoachAvailability> shatabdiClasses = List.of(
                new CoachAvailability("CC", "GENERAL", 480, 134, 16, 0, 1350.0, 1350.0),
                new CoachAvailability("EC", "GENERAL", 48, 16, 0, 0, 2400.0, 2400.0)
        );

        // 8. 12626: Kerala Superfast Express (NDLS -> TVC via AGC, GWL, BPL, NGP, BZA, MAS, KPD, ERS)
        List<RouteHalt> halts12626 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(20, 10), 0, 0, 1),
                new RouteHalt(2, agc,  LocalTime.of(22, 20), LocalTime.of(22, 25), 5, 195, 1),
                new RouteHalt(3, gwl,  LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 313, 1),
                new RouteHalt(4, bpl,  LocalTime.of(5, 20), LocalTime.of(5, 25), 5, 705, 2),
                new RouteHalt(5, ngp,  LocalTime.of(11, 45), LocalTime.of(11, 50), 5, 1090, 2),
                new RouteHalt(6, bza,  LocalTime.of(22, 15), LocalTime.of(22, 25), 10, 1754, 2),
                new RouteHalt(7, mas,  LocalTime.of(4, 30), LocalTime.of(4, 55), 25, 2185, 3),
                new RouteHalt(8, kpd,  LocalTime.of(6, 48), LocalTime.of(6, 50), 2, 2315, 3),
                new RouteHalt(9, ers,  LocalTime.of(14, 15), LocalTime.of(14, 20), 5, 2800, 3),
                new RouteHalt(10, tvc, LocalTime.of(18, 0), null, 0, 3036, 3)
        );
        registerTrain(new Train(8L, "12626", "Kerala Superfast Express", TrainType.SUPERFAST, 
                ndls, tvc, "1111111", TrainStatus.ON_TIME, halts12626, expressClasses));

        // 9. 12625: Kerala Superfast Express (TVC -> NDLS)
        List<RouteHalt> halts12625 = List.of(
                new RouteHalt(1, tvc, null, LocalTime.of(11, 15), 0, 0, 1),
                new RouteHalt(2, ers, LocalTime.of(15, 35), LocalTime.of(15, 40), 5, 236, 1),
                new RouteHalt(3, kpd, LocalTime.of(22, 58), LocalTime.of(23, 0), 2, 721, 1),
                new RouteHalt(4, mas, LocalTime.of(0, 50), LocalTime.of(1, 15), 25, 851, 2),
                new RouteHalt(5, bza, LocalTime.of(7, 10), LocalTime.of(7, 20), 10, 1282, 2),
                new RouteHalt(6, ngp, LocalTime.of(17, 40), LocalTime.of(17, 45), 5, 1946, 2),
                new RouteHalt(7, bpl, LocalTime.of(23, 55), LocalTime.of(0, 5), 10, 2331, 2),
                new RouteHalt(8, gwl, LocalTime.of(5, 28), LocalTime.of(5, 30), 2, 2723, 3),
                new RouteHalt(9, agc, LocalTime.of(7, 10), LocalTime.of(7, 15), 5, 2841, 3),
                new RouteHalt(10, ndls, LocalTime.of(10, 25), null, 0, 3036, 3)
        );
        registerTrain(new Train(9L, "12625", "Kerala Superfast Express", TrainType.SUPERFAST, 
                tvc, ndls, "1111111", TrainStatus.ON_TIME, halts12625, expressClasses));

        // 10. 12138: Punjab Mail (ASR -> CSMT via CDG, NDLS, AGC, GWL, BPL, NGP)
        List<RouteHalt> halts12138 = List.of(
                new RouteHalt(1, asr,  null, LocalTime.of(21, 45), 0, 0, 1),
                new RouteHalt(2, cdg,  LocalTime.of(1, 40), LocalTime.of(1, 50), 10, 230, 2),
                new RouteHalt(3, ndls, LocalTime.of(5, 0), LocalTime.of(5, 15), 15, 490, 2),
                new RouteHalt(4, agc,  LocalTime.of(7, 45), LocalTime.of(7, 50), 5, 685, 2),
                new RouteHalt(5, gwl,  LocalTime.of(9, 12), LocalTime.of(9, 14), 2, 803, 2),
                new RouteHalt(6, bpl,  LocalTime.of(16, 30), LocalTime.of(16, 40), 10, 1195, 2),
                new RouteHalt(7, ngp,  LocalTime.of(23, 25), LocalTime.of(23, 35), 10, 1580, 2),
                new RouteHalt(8, csmt, LocalTime.of(7, 35), null, 0, 1925, 3)
        );
        registerTrain(new Train(10L, "12138", "Punjab Mail Express", TrainType.EXPRESS, 
                asr, csmt, "1111111", TrainStatus.ON_TIME, halts12138, expressClasses));

        // 11. 12137: Punjab Mail (CSMT -> ASR)
        List<RouteHalt> halts12137 = List.of(
                new RouteHalt(1, csmt, null, LocalTime.of(19, 35), 0, 0, 1),
                new RouteHalt(2, ngp,  LocalTime.of(3, 45), LocalTime.of(3, 55), 10, 345, 2),
                new RouteHalt(3, bpl,  LocalTime.of(10, 20), LocalTime.of(10, 30), 10, 730, 2),
                new RouteHalt(4, gwl,  LocalTime.of(17, 35), LocalTime.of(17, 37), 2, 1122, 2),
                new RouteHalt(5, agc,  LocalTime.of(19, 10), LocalTime.of(19, 15), 5, 1240, 2),
                new RouteHalt(6, ndls, LocalTime.of(21, 30), LocalTime.of(21, 50), 20, 1435, 2),
                new RouteHalt(7, cdg,  LocalTime.of(1, 10), LocalTime.of(1, 20), 10, 1695, 3),
                new RouteHalt(8, asr,  LocalTime.of(5, 10), null, 0, 1925, 3)
        );
        registerTrain(new Train(11L, "12137", "Punjab Mail Express", TrainType.EXPRESS, 
                csmt, asr, "1111111", TrainStatus.ON_TIME, halts12137, expressClasses));

        // 12. 12009: Mumbai - Ahmedabad Shatabdi Express (MMCT -> ADI via ST, BRC)
        List<RouteHalt> halts12009 = List.of(
                new RouteHalt(1, mmct, null, LocalTime.of(6, 20), 0, 0, 1),
                new RouteHalt(2, st,   LocalTime.of(9, 15), LocalTime.of(9, 18), 3, 263, 1),
                new RouteHalt(3, brc,  LocalTime.of(10, 48), LocalTime.of(10, 53), 5, 392, 1),
                new RouteHalt(4, adi,  LocalTime.of(12, 45), null, 0, 492, 1)
        );
        registerTrain(new Train(12L, "12009", "Mumbai - Ahmedabad Shatabdi Express", TrainType.SHATABDI, 
                mmct, adi, "1111110", TrainStatus.ON_TIME, halts12009, shatabdiClasses));

        // 13. 12010: Ahmedabad - Mumbai Shatabdi Express (ADI -> MMCT)
        List<RouteHalt> halts12010 = List.of(
                new RouteHalt(1, adi,  null, LocalTime.of(15, 10), 0, 0, 1),
                new RouteHalt(2, brc,  LocalTime.of(16, 42), LocalTime.of(16, 47), 5, 100, 1),
                new RouteHalt(3, st,   LocalTime.of(18, 2), LocalTime.of(18, 7), 5, 229, 1),
                new RouteHalt(4, mmct, LocalTime.of(21, 45), null, 0, 492, 1)
        );
        registerTrain(new Train(13L, "12010", "Ahmedabad - Mumbai Shatabdi Express", TrainType.SHATABDI, 
                adi, mmct, "1111110", TrainStatus.ON_TIME, halts12010, shatabdiClasses));

        // 14. 20901: Mumbai - Gandhinagar Vande Bharat (MMCT -> ADI)
        List<RouteHalt> halts20901 = List.of(
                new RouteHalt(1, mmct, null, LocalTime.of(6, 0), 0, 0, 1),
                new RouteHalt(2, st,   LocalTime.of(8, 37), LocalTime.of(8, 40), 3, 263, 1),
                new RouteHalt(3, brc,  LocalTime.of(9, 56), LocalTime.of(9, 59), 3, 392, 1),
                new RouteHalt(4, adi,  LocalTime.of(11, 25), null, 0, 492, 1)
        );
        registerTrain(new Train(14L, "20901", "Vande Bharat Express", TrainType.VANDE_BHARAT, 
                mmct, adi, "1111101", TrainStatus.ON_TIME, halts20901, classes22436));

        // 15. 20902: Gandhinagar - Mumbai Vande Bharat (ADI -> MMCT)
        List<RouteHalt> halts20902 = List.of(
                new RouteHalt(1, adi,  null, LocalTime.of(15, 0), 0, 0, 1),
                new RouteHalt(2, brc,  LocalTime.of(16, 13), LocalTime.of(16, 15), 2, 100, 1),
                new RouteHalt(3, st,   LocalTime.of(17, 33), LocalTime.of(17, 36), 3, 229, 1),
                new RouteHalt(4, mmct, LocalTime.of(20, 25), null, 0, 492, 1)
        );
        registerTrain(new Train(15L, "20902", "Vande Bharat Express", TrainType.VANDE_BHARAT, 
                adi, mmct, "1111101", TrainStatus.ON_TIME, halts20902, classes22436));

        // 16. 12015: Ajmer Shatabdi Express (NDLS -> JP)
        List<RouteHalt> halts12015 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(6, 10), 0, 0, 1),
                new RouteHalt(2, gzb,  LocalTime.of(6, 48), LocalTime.of(6, 50), 2, 26, 1),
                new RouteHalt(3, jp,   LocalTime.of(10, 40), null, 0, 308, 1)
        );
        registerTrain(new Train(16L, "12015", "Ajmer Shatabdi Express", TrainType.SHATABDI, 
                ndls, jp, "1111111", TrainStatus.ON_TIME, halts12015, shatabdiClasses));

        // 17. 12016: Ajmer - New Delhi Shatabdi Express (JP -> NDLS)
        List<RouteHalt> halts12016 = List.of(
                new RouteHalt(1, jp,   null, LocalTime.of(17, 50), 0, 0, 1),
                new RouteHalt(2, gzb,  LocalTime.of(21, 40), LocalTime.of(21, 42), 2, 282, 1),
                new RouteHalt(3, ndls, LocalTime.of(22, 40), null, 0, 308, 1)
        );
        registerTrain(new Train(17L, "12016", "Ajmer - New Delhi Shatabdi Express", TrainType.SHATABDI, 
                jp, ndls, "1111111", TrainStatus.ON_TIME, halts12016, shatabdiClasses));

        // 18. 12393: Sampoorna Kranti Express (PNBE -> NDLS via DDU, CNB)
        List<RouteHalt> halts12393 = List.of(
                new RouteHalt(1, pnbe, null, LocalTime.of(19, 25), 0, 0, 1),
                new RouteHalt(2, ddu,  LocalTime.of(22, 20), LocalTime.of(22, 30), 10, 212, 1),
                new RouteHalt(3, cnb,  LocalTime.of(2, 25), LocalTime.of(2, 30), 5, 558, 2),
                new RouteHalt(4, ndls, LocalTime.of(7, 55), null, 0, 998, 2)
        );
        registerTrain(new Train(18L, "12393", "Sampoorna Kranti Superfast Express", TrainType.SUPERFAST, 
                pnbe, ndls, "1111111", TrainStatus.ON_TIME, halts12393, expressClasses));

        // 19. 12394: Sampoorna Kranti Express (NDLS -> PNBE)
        List<RouteHalt> halts12394 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(17, 30), 0, 0, 1),
                new RouteHalt(2, cnb,  LocalTime.of(22, 22), LocalTime.of(22, 30), 8, 440, 1),
                new RouteHalt(3, ddu,  LocalTime.of(2, 25), LocalTime.of(2, 35), 10, 786, 2),
                new RouteHalt(4, pnbe, LocalTime.of(6, 50), null, 0, 998, 2)
        );
        registerTrain(new Train(19L, "12394", "Sampoorna Kranti Superfast Express", TrainType.SUPERFAST, 
                ndls, pnbe, "1111111", TrainStatus.ON_TIME, halts12394, expressClasses));

        // 20. 12046: Chandigarh Shatabdi Express (CDG -> NDLS)
        List<RouteHalt> halts12046 = List.of(
                new RouteHalt(1, cdg,  null, LocalTime.of(12, 5), 0, 0, 1),
                new RouteHalt(2, ndls, LocalTime.of(15, 20), null, 0, 244, 1)
        );
        registerTrain(new Train(20L, "12046", "Chandigarh Shatabdi Express", TrainType.SHATABDI, 
                cdg, ndls, "1111110", TrainStatus.ON_TIME, halts12046, shatabdiClasses));

        // 21. 12424: Dibrugarh Rajdhani Express (NDLS -> GHY via CNB, DDU, PNBE)
        List<RouteHalt> halts12424 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(16, 20), 0, 0, 1),
                new RouteHalt(2, cnb,  LocalTime.of(21, 2), LocalTime.of(21, 7), 5, 440, 1),
                new RouteHalt(3, ddu,  LocalTime.of(1, 23), LocalTime.of(1, 33), 10, 788, 2),
                new RouteHalt(4, pnbe, LocalTime.of(4, 10), LocalTime.of(4, 20), 10, 1000, 2),
                new RouteHalt(5, ghy,  LocalTime.of(19, 30), null, 0, 1880, 2)
        );
        registerTrain(new Train(21L, "12424", "Dibrugarh Rajdhani Express", TrainType.RAJDHANI, 
                ndls, ghy, "1111111", TrainStatus.ON_TIME, halts12424, classes12302));

        // 22. 12724: Telangana Superfast Express (NDLS -> HYB via AGC, GWL, BPL, NGP, SC)
        List<RouteHalt> halts12724 = List.of(
                new RouteHalt(1, ndls, null, LocalTime.of(16, 0), 0, 0, 1),
                new RouteHalt(2, agc,  LocalTime.of(18, 5), LocalTime.of(18, 7), 2, 195, 1),
                new RouteHalt(3, gwl,  LocalTime.of(19, 28), LocalTime.of(19, 30), 2, 313, 1),
                new RouteHalt(4, bpl,  LocalTime.of(1, 20), LocalTime.of(1, 30), 10, 705, 2),
                new RouteHalt(5, ngp,  LocalTime.of(7, 10), LocalTime.of(7, 15), 5, 1090, 2),
                new RouteHalt(6, sc,   LocalTime.of(16, 15), LocalTime.of(16, 20), 5, 1665, 2),
                new RouteHalt(7, hyb,  LocalTime.of(17, 10), null, 0, 1675, 2)
        );
        registerTrain(new Train(22L, "12724", "Telangana Superfast Express", TrainType.SUPERFAST, 
                ndls, hyb, "1111111", TrainStatus.ON_TIME, halts12724, expressClasses));
    }

    private static void registerTrain(Train t) {
        IN_MEMORY_TRAINS.put(t.getTrainNumber(), t);
    }
}
