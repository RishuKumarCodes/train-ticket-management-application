package com.trainticket.model.dao;

import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object for Passenger Bookings and Tickets.
 * Backed by MySQL JDBC with thread-safe in-memory session persistence.
 */
public class BookingDAO {

    private static final Logger logger = LoggerFactory.getLogger(BookingDAO.class);
    private static final Map<String, Booking> IN_MEMORY_BOOKINGS = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);

    public BookingDAO() {
        seedSampleBookingsIfEmpty();
    }

    private void seedSampleBookingsIfEmpty() {
        if (IN_MEMORY_BOOKINGS.isEmpty()) {
            // 1. Confirmed Booking
            List<BookingPassenger> passengers1 = List.of(
                    BookingPassenger.create("Rishu Kumar", 26, "M", "LOWER", "B2", 34)
            );
            Booking sample1 = new Booking(
                    1L,
                    "234-8901234",
                    null,
                    "guest@railflow.com",
                    1L,
                    "12952",
                    "New Delhi Tejas Rajdhani Express",
                    LocalDate.now().plusDays(2),
                    "NDLS",
                    "New Delhi",
                    "MMCT",
                    "Mumbai Central",
                    "16:55",
                    "08:35",
                    "3A",
                    "GENERAL",
                    2080.0,
                    "CONFIRMED",
                    LocalDateTime.now().minusHours(3),
                    passengers1
            );
            IN_MEMORY_BOOKINGS.put(sample1.getPnr(), sample1);

            // 2. RAC Booking
            List<BookingPassenger> passengers2 = List.of(
                    new BookingPassenger(null, "Priya Sharma", 24, "F", "SIDE LOWER", "B1", 12, "RAC 12"),
                    new BookingPassenger(null, "Rahul Sharma", 28, "M", "SIDE LOWER", "B1", 13, "RAC 13")
            );
            Booking sample2 = new Booking(
                    2L,
                    "645-1234567",
                    null,
                    "priya@example.com",
                    12301L,
                    "12301",
                    "Howrah Rajdhani Express",
                    LocalDate.now().plusDays(3),
                    "HWH",
                    "Howrah Junction",
                    "NDLS",
                    "New Delhi",
                    "16:50",
                    "10:05",
                    "2A",
                    "GENERAL",
                    4920.0,
                    "RAC",
                    LocalDateTime.now().minusHours(12),
                    passengers2
            );
            IN_MEMORY_BOOKINGS.put(sample2.getPnr(), sample2);

            // 3. Waitlisted Booking
            List<BookingPassenger> passengers3 = List.of(
                    new BookingPassenger(null, "Amit Patel", 35, "M", "MIDDLE", "WL", 45, "WL 45")
            );
            Booking sample3 = new Booking(
                    3L,
                    "812-9876543",
                    null,
                    "amit.p@example.com",
                    11041L,
                    "11041",
                    "CSMT Chennai Express",
                    LocalDate.now().plusDays(4),
                    "CSMT",
                    "Mumbai CSMT",
                    "MAS",
                    "Chennai Central",
                    "14:00",
                    "16:45",
                    "SL",
                    "GENERAL",
                    560.0,
                    "WL",
                    LocalDateTime.now().minusHours(24),
                    passengers3
            );
            IN_MEMORY_BOOKINGS.put(sample3.getPnr(), sample3);
        }
    }

    public synchronized Booking saveBooking(Booking booking) {
        long bookingId = ID_GENERATOR.incrementAndGet();
        Booking saved = new Booking(
                bookingId,
                booking.getPnr(),
                booking.getUserId(),
                booking.getUserIdentifier(),
                booking.getTrainId(),
                booking.getTrainNumber(),
                booking.getTrainName(),
                booking.getJourneyDate(),
                booking.getFromStationCode(),
                booking.getFromStationName(),
                booking.getToStationCode(),
                booking.getToStationName(),
                booking.getDepartureTime(),
                booking.getArrivalTime(),
                booking.getClassCode(),
                booking.getQuotaCode(),
                booking.getTotalFare(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getPassengers()
        );

        IN_MEMORY_BOOKINGS.put(saved.getPnr(), saved);

        if (DatabaseConnectionPool.isAvailable()) {
            String insertSql = """
                INSERT INTO bookings (pnr, user_id, train_id, journey_date, from_station_id, to_station_id,
                                      class_code, quota_code, total_fare, status, created_at)
                VALUES (?, ?, ?, ?,
                        (SELECT id FROM stations WHERE code = ? LIMIT 1),
                        (SELECT id FROM stations WHERE code = ? LIMIT 1),
                        ?, ?, ?, ?, ?)
            """;
            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, saved.getPnr());
                    if (saved.getUserId() != null) {
                        stmt.setLong(2, saved.getUserId());
                    } else {
                        stmt.setNull(2, java.sql.Types.BIGINT);
                    }
                    stmt.setLong(3, saved.getTrainId() != null ? saved.getTrainId() : 1L);
                    stmt.setDate(4, Date.valueOf(saved.getJourneyDate()));
                    stmt.setString(5, saved.getFromStationCode());
                    stmt.setString(6, saved.getToStationCode());
                    stmt.setString(7, saved.getClassCode());
                    stmt.setString(8, saved.getQuotaCode());
                    stmt.setDouble(9, saved.getTotalFare());
                    stmt.setString(10, saved.getStatus());
                    stmt.setTimestamp(11, Timestamp.valueOf(saved.getCreatedAt()));

                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            long dbId = rs.getLong(1);
                            insertPassengers(conn, dbId, saved.getPassengers());
                        }
                    }
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    logger.warn("JDBC save booking error, using in-memory store: {}", ex.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                logger.warn("Could not obtain DB connection to save booking: {}", ex.getMessage());
            }
        }

        logger.info("Saved booking PNR {} for train {} ({})", saved.getPnr(), saved.getTrainNumber(), saved.getStatus());
        return saved;
    }

    private void insertPassengers(Connection conn, long bookingId, List<BookingPassenger> passengers) throws SQLException {
        if (passengers == null || passengers.isEmpty()) {
            return;
        }
        String sql = """
            INSERT INTO booking_passengers (booking_id, passenger_name, age, gender, berth_preference, coach_number, seat_number, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (BookingPassenger p : passengers) {
                stmt.setLong(1, bookingId);
                stmt.setString(2, p.getPassengerName());
                stmt.setInt(3, p.getAge());
                stmt.setString(4, p.getGender());
                stmt.setString(5, p.getBerthPreference());
                stmt.setString(6, p.getCoachNumber());
                stmt.setInt(7, p.getSeatNumber());
                stmt.setString(8, p.getStatus());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<Booking> findAll() {
        return new ArrayList<>(IN_MEMORY_BOOKINGS.values());
    }

    public List<Booking> findByUserIdOrIdentifier(Long userId, String identifier) {
        List<Booking> results = new ArrayList<>();
        String normId = identifier != null ? identifier.trim().toLowerCase() : null;

        for (Booking b : IN_MEMORY_BOOKINGS.values()) {
            boolean matchesUserId = userId != null && userId.equals(b.getUserId());
            boolean matchesIdentifier = normId != null && b.getUserIdentifier() != null
                    && b.getUserIdentifier().trim().equalsIgnoreCase(normId);

            if (matchesUserId || matchesIdentifier) {
                results.add(b);
            }
        }
        return results;
    }

    public Booking findByPnr(String pnr) {
        if (pnr == null || pnr.isBlank()) return null;
        String cleanInput = pnr.replaceAll("[^a-zA-Z0-9]", "").trim();
        for (Booking b : IN_MEMORY_BOOKINGS.values()) {
            String cleanStored = b.getPnr().replaceAll("[^a-zA-Z0-9]", "").trim();
            if (cleanStored.equalsIgnoreCase(cleanInput) || b.getPnr().equalsIgnoreCase(pnr.trim())) {
                return b;
            }
        }
        return null;
    }

    public boolean cancelBooking(String pnr) {
        if (pnr == null || pnr.isBlank()) return false;
        String cleanPnr = pnr.trim();

        // 1. Database persistence if connection pool is available
        if (DatabaseConnectionPool.isAvailable()) {
            String updateBookingSql = "UPDATE bookings SET status = 'CANCELLED' WHERE pnr = ?";
            String updatePassengersSql = "UPDATE booking_passengers SET status = 'CANCELLED' WHERE booking_id = (SELECT id FROM bookings WHERE pnr = ?)";
            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt1 = conn.prepareStatement(updateBookingSql);
                     PreparedStatement stmt2 = conn.prepareStatement(updatePassengersSql)) {
                    stmt1.setString(1, cleanPnr);
                    stmt1.executeUpdate();
                    stmt2.setString(1, cleanPnr);
                    stmt2.executeUpdate();
                    conn.commit();
                    logger.info("Database transaction committed: Cancelled booking PNR {}", cleanPnr);
                } catch (SQLException ex) {
                    conn.rollback();
                    logger.error("Failed to cancel booking in database for PNR {}", cleanPnr, ex);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                logger.error("Database connection error cancelling booking PNR {}", cleanPnr, ex);
            }
        }

        // 2. In-memory update fallback / sync
        Booking existing = findByPnr(cleanPnr);
        if (existing != null) {
            List<BookingPassenger> cancelledPassengers = new ArrayList<>();
            for (BookingPassenger p : existing.getPassengers()) {
                cancelledPassengers.add(new BookingPassenger(
                        p.getId(), p.getPassengerName(), p.getAge(), p.getGender(),
                        p.getBerthPreference(), p.getCoachNumber(), p.getSeatNumber(), "CANCELLED"
                ));
            }
            Booking updated = new Booking(
                    existing.getId(), existing.getPnr(), existing.getUserId(), existing.getUserIdentifier(),
                    existing.getTrainId(), existing.getTrainNumber(), existing.getTrainName(),
                    existing.getJourneyDate(), existing.getFromStationCode(), existing.getFromStationName(),
                    existing.getToStationCode(), existing.getToStationName(),
                    existing.getDepartureTime(), existing.getArrivalTime(),
                    existing.getClassCode(), existing.getQuotaCode(), existing.getTotalFare(),
                    "CANCELLED", existing.getCreatedAt(), cancelledPassengers
            );
            IN_MEMORY_BOOKINGS.put(existing.getPnr(), updated);
            logger.info("In-memory store updated: Cancelled booking PNR {}", existing.getPnr());
            return true;
        }
        return false;
    }
}
