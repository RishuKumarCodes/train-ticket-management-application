package com.trainticket.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive unit test suite for domain entities and value objects.
 */
public class ModelEntitiesTest {

    @Test
    @DisplayName("Station creation, immutability, uppercase normalization and equality")
    void testStationEntity() {
        Station s1 = new Station(1L, "ndls", "New Delhi", "New Delhi", "Delhi");
        assertEquals("NDLS", s1.getCode(), "Station code should be normalized to uppercase");
        assertEquals("New Delhi", s1.getName());
        assertEquals("New Delhi (NDLS)", s1.getDisplayName());

        Station s2 = new Station(2L, "NDLS", "Different Name", "Delhi", "Delhi");
        assertEquals(s1, s2, "Stations with matching codes should be equal");
        assertEquals(s1.hashCode(), s2.hashCode(), "Matching stations should share hash code");

        assertThrows(NullPointerException.class, () -> new Station(3L, null, "Name", "City", "State"));
    }

    @Test
    @DisplayName("Train running days bitmask and schedule validation")
    void testTrainEntity() {
        Station src = new Station(1L, "NDLS", "New Delhi", "Delhi", "Delhi");
        Station dst = new Station(2L, "MMCT", "Mumbai Central", "Mumbai", "Maharashtra");

        // Runs Mon, Wed, Fri: bits at index 0, 2, 4 are '1'
        String bitmask = "1010100";
        Train train = new Train(101L, "12952", "Tejas Rajdhani", TrainType.RAJDHANI, src, dst, bitmask, TrainStatus.ON_TIME, List.of(), List.of());

        assertTrue(train.runsOn(DayOfWeek.MONDAY));
        assertFalse(train.runsOn(DayOfWeek.TUESDAY));
        assertTrue(train.runsOn(DayOfWeek.WEDNESDAY));
        assertFalse(train.runsOn(DayOfWeek.SUNDAY));
        assertEquals("Runs on: Mon, Wed, Fri", train.getFormattedRunningDays());

        Train dailyTrain = new Train(102L, "12301", "Daily Express", TrainType.EXPRESS, src, dst, "1111111", TrainStatus.ON_TIME, List.of(), List.of());
        assertEquals("Runs Daily", dailyTrain.getFormattedRunningDays());
    }

    @Test
    @DisplayName("RouteHalt metrics, timing and stop calculations")
    void testRouteHaltEntity() {
        Station stn = new Station(10L, "CNB", "Kanpur Central", "Kanpur", "UP");
        RouteHalt halt = new RouteHalt(2, stn, LocalTime.of(12, 0), LocalTime.of(12, 10), 10, 440, 1);

        assertEquals(2, halt.getStopSequence());
        assertEquals(10, halt.getHaltMinutes());
        assertEquals(440, halt.getDistanceKm());
        assertEquals(1, halt.getDayCount());
    }

    @Test
    @DisplayName("CoachAvailability status mapping and pricing calculation")
    void testCoachAvailability() {
        CoachAvailability avail = new CoachAvailability("3A", "GENERAL", 64, 18, 0, 0, 1500.0, 1500.0);
        assertEquals(CoachAvailability.AvailabilityStatus.AVAILABLE, avail.getStatus());
        assertEquals("AVAILABLE - 18", avail.getStatusBadgeText());

        CoachAvailability rac = new CoachAvailability("3A", "GENERAL", 64, 0, 8, 0, 1500.0, 1500.0);
        assertEquals(CoachAvailability.AvailabilityStatus.RAC, rac.getStatus());
        assertEquals("RAC - 8", rac.getStatusBadgeText());

        CoachAvailability wl = new CoachAvailability("3A", "GENERAL", 64, 0, 0, 15, 1500.0, 1500.0);
        assertEquals(CoachAvailability.AvailabilityStatus.WAITLIST, wl.getStatus());
        assertEquals("WL - 15", wl.getStatusBadgeText());
    }

    @Test
    @DisplayName("TravelQuota multiplier and surge pricing bounds")
    void testTravelQuota() {
        assertEquals(1.00, TravelQuota.GENERAL.getFareMultiplier());
        assertEquals(1.30, TravelQuota.TATKAL.getFareMultiplier());
        assertEquals(1.50, TravelQuota.PREMIUM_TATKAL.getFareMultiplier());
        assertEquals(TravelQuota.TATKAL, TravelQuota.fromCode("TQ"));
        assertEquals(TravelQuota.PREMIUM_TATKAL, TravelQuota.fromCode("PT"));
        assertEquals(TravelQuota.GENERAL, TravelQuota.fromCode("UNKNOWN"));
    }

    @Test
    @DisplayName("ConcessionType discount rules and statutory minimum fare")
    void testConcessionType() {
        assertEquals(2000.0, ConcessionType.NONE.applyConcession(2000.0));
        assertEquals(1000.0, ConcessionType.PERSON_WITH_DISABILITY.applyConcession(2000.0));
        assertEquals(50.0, ConcessionType.PERSON_WITH_DISABILITY.applyConcession(60.0), "Should enforce minimum statutory floor of 50");
        assertEquals(40.0, ConcessionType.RAILWAY_PASS.applyConcession(2000.0), "Should set statutory pass surcharge of 40");
    }

    @Test
    @DisplayName("BookingPassenger creation and formatting")
    void testBookingPassenger() {
        BookingPassenger p = BookingPassenger.create("Vikram Singh", 32, "M", "LOWER", "B2", 15);
        assertEquals("Vikram Singh", p.getPassengerName());
        assertEquals(32, p.getAge());
        assertEquals("M", p.getGender());
        assertEquals("LOWER", p.getBerthPreference());
        assertEquals("B2", p.getCoachNumber());
        assertEquals(15, p.getSeatNumber());
        assertEquals("CONFIRMED", p.getStatus());
        assertTrue(p.toString().contains("Vikram Singh (32, M)"));
    }

    @Test
    @DisplayName("Booking domain model validations and formatting")
    void testBookingEntity() {
        BookingPassenger p = BookingPassenger.create("Sneha Patel", 28, "F", "UPPER", "B1", 10);
        Booking b = new Booking(
                1L, "123-4567890", 50L, "sneha@example.com",
                101L, "12952", "Rajdhani Express",
                LocalDate.of(2026, 10, 15), "NDLS", "New Delhi", "MMCT", "Mumbai Central",
                "16:55", "08:35", "3A", "GENERAL", 2080.0, "CONFIRMED", null, List.of(p)
        );

        assertEquals("CONFIRMED", b.getStatus());
        assertFalse(b.isCancelled());
        assertEquals(1, b.getPassengers().size());
        assertEquals(2080.0, b.getTotalFare());
        assertEquals("123-4567890", b.getPnr());
        assertNotNull(b.getCreatedAt());
    }

    @Test
    @DisplayName("TrainSearchQuery parameter validation")
    void testTrainSearchQuery() {
        TrainSearchQuery query = new TrainSearchQuery("ndls", "mmct", LocalDate.now().plusDays(2), TravelQuota.TATKAL, ConcessionType.NONE, "3A");
        assertEquals("NDLS", query.getFromStationCode());
        assertEquals("MMCT", query.getToStationCode());
        assertEquals(TravelQuota.TATKAL, query.getQuota());
        assertEquals("3A", query.getPreferredClass());

        assertThrows(NullPointerException.class, () -> new TrainSearchQuery(null, "MMCT", LocalDate.now(), null, null));
    }

    @Test
    @DisplayName("TrainSearchResult duration, distance and day offset")
    void testTrainSearchResult() {
        Station src = new Station(1L, "NDLS", "New Delhi", "Delhi", "Delhi");
        Station dst = new Station(2L, "MMCT", "Mumbai Central", "Mumbai", "Maharashtra");
        Train train = new Train(101L, "12952", "Tejas Rajdhani", TrainType.RAJDHANI, src, dst, "1111111", TrainStatus.ON_TIME, List.of(), List.of());

        RouteHalt h1 = new RouteHalt(1, src, LocalTime.of(16, 55), LocalTime.of(16, 55), 0, 0, 1);
        RouteHalt h2 = new RouteHalt(8, dst, LocalTime.of(8, 35), LocalTime.of(8, 35), 0, 1386, 2);

        TrainSearchResult result = new TrainSearchResult(train, h1, h2, List.of());
        assertEquals(1386, result.getDistanceKm());
        assertEquals("+1 day", result.getDayOffsetLabel());
        assertEquals("15h 40m", result.getFormattedDuration());
        assertEquals(940, result.getDurationMinutes());
    }

    @Test
    @DisplayName("TrainType and TrainStatus enum properties and display names")
    void testTrainTypeAndStatusEnums() {
        assertEquals("Vande Bharat Express", TrainType.VANDE_BHARAT.getDisplayName());
        assertEquals("VB", TrainType.VANDE_BHARAT.getShortBadge());
        assertEquals("Tejas Rajdhani Express", TrainType.RAJDHANI.getDisplayName());
        assertEquals("RAJ", TrainType.RAJDHANI.getShortBadge());
        assertEquals("Shatabdi Express", TrainType.SHATABDI.getDisplayName());
        assertEquals("Superfast Express", TrainType.SUPERFAST.getDisplayName());
        assertEquals("Mail / Express", TrainType.EXPRESS.getDisplayName());

        assertEquals(TrainStatus.ON_TIME, TrainStatus.valueOf("ON_TIME"));
        assertEquals(TrainStatus.DELAYED, TrainStatus.valueOf("DELAYED"));
        assertEquals(TrainStatus.CANCELLED, TrainStatus.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("User and UserRole validation rules")
    void testUserRoleAndUserEntity() {
        assertEquals(UserRole.GUEST, UserRole.valueOf("GUEST"));
        assertEquals(UserRole.PASSENGER, UserRole.valueOf("PASSENGER"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));

        User admin = User.createAdmin(1L, "station_master", "admin@railflow.com", "Chief Station Master");
        assertTrue(admin.isAdmin());
        assertEquals(UserRole.ADMIN, admin.getRole());

        User passenger = User.createNewPassenger("rohit99", "rohit@test.com", "+919876543210", "Rohit Kumar");
        assertEquals(UserRole.PASSENGER, passenger.getRole());
        assertFalse(passenger.isAdmin());

        // Rejects user without email AND phone
        assertThrows(IllegalArgumentException.class, () -> 
                new User(null, "invalid_user", null, null, "No Contact", UserRole.PASSENGER, "ACTIVE", null));
    }

    @Test
    @DisplayName("RouteHalt origin, terminus and time formatting")
    void testRouteHaltStationFormatting() {
        Station ndls = new Station(1L, "NDLS", "New Delhi", "Delhi", "Delhi");
        RouteHalt origin = new RouteHalt(1, ndls, null, LocalTime.of(16, 55), 0, 0, 1);
        assertTrue(origin.isOrigin());
        assertFalse(origin.isTerminus());
        assertEquals("--:--", origin.getFormattedArrivalTime());
        assertEquals("16:55", origin.getFormattedDepartureTime());
        assertEquals("Origin", origin.getHaltSummary());

        Station mmct = new Station(2L, "MMCT", "Mumbai Central", "Mumbai", "Maharashtra");
        RouteHalt terminus = new RouteHalt(8, mmct, LocalTime.of(8, 35), null, 0, 1386, 2);
        assertFalse(terminus.isOrigin());
        assertTrue(terminus.isTerminus());
        assertEquals("8:35", terminus.getFormattedArrivalTime().replaceAll("^0", ""));
        assertEquals("--:--", terminus.getFormattedDepartureTime());
        assertEquals("Terminus", terminus.getHaltSummary());
    }

    @Test
    @DisplayName("Booking withStatus returns updated immutable copy")
    void testBookingWithStatus() {
        Booking b = new Booking(
                1L, "333-1112233", 10L, "test@example.com", 1L, "12952", "Rajdhani",
                LocalDate.now(), "NDLS", "New Delhi", "MMCT", "Mumbai Central",
                "16:55", "08:35", "3A", "GENERAL", 2000.0, "CONFIRMED", null, List.of()
        );
        assertEquals("CONFIRMED", b.getStatus());
        assertFalse(b.isCancelled());

        Booking cancelled = b.withStatus("CANCELLED");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertTrue(cancelled.isCancelled());
        // Original remains untouched
        assertEquals("CONFIRMED", b.getStatus());
    }
}
