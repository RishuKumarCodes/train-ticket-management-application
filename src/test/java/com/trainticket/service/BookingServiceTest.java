package com.trainticket.service;

import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.model.User;
import com.trainticket.model.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for BookingService and BookingDAO.
 */
public class BookingServiceTest {

    private final BookingService bookingService = BookingService.getInstance();

    @Test
    @DisplayName("Booking creation and PNR retrieval returns correct passenger and train details")
    void testCreateAndRetrieveBooking() {
        String pnr = "999-1234567";
        BookingPassenger passenger = BookingPassenger.create("Ananya Sharma", 24, "F", "LOWER", "B1", 12);
        Booking booking = new Booking(
                null,
                pnr,
                201L,
                "ananya@example.com",
                1L,
                "12952",
                "New Delhi Tejas Rajdhani Express",
                LocalDate.now().plusDays(3),
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
                LocalDateTime.now(),
                List.of(passenger)
        );

        Booking saved = bookingService.createBooking(booking);
        assertNotNull(saved, "Saved booking should not be null");
        assertEquals(pnr, saved.getPnr(), "PNR should match");

        Booking retrieved = bookingService.getBookingByPnr(pnr);
        assertNotNull(retrieved, "Retrieved booking should not be null");
        assertEquals("CONFIRMED", retrieved.getStatus(), "Status should be CONFIRMED");
        assertEquals(1, retrieved.getPassengers().size(), "Should have 1 passenger");
    }

    @Test
    @DisplayName("Booking cancellation updates status and marks reservation as cancelled")
    void testCancelBooking() {
        String pnr = "888-7654321";
        BookingPassenger passenger = BookingPassenger.create("Rohan Verma", 30, "M", "UPPER", "B3", 45);
        Booking booking = new Booking(
                null,
                pnr,
                202L,
                "rohan@example.com",
                3L,
                "22436",
                "Vande Bharat Express",
                LocalDate.now().plusDays(2),
                "NDLS",
                "New Delhi",
                "BSB",
                "Varanasi",
                "06:00",
                "14:00",
                "CC",
                "GENERAL",
                1750.0,
                "CONFIRMED",
                LocalDateTime.now(),
                List.of(passenger)
        );

        bookingService.createBooking(booking);
        boolean cancelled = bookingService.cancelBooking(pnr);
        assertTrue(cancelled, "cancelBooking should return true for existing booking");

        Booking retrieved = bookingService.getBookingByPnr(pnr);
        assertTrue(retrieved.isCancelled(), "Booking should be marked as CANCELLED");
        assertEquals("CANCELLED", retrieved.getStatus(), "Status should be CANCELLED");
    }

    @Test
    @DisplayName("Retrieve bookings filtered by user account")
    void testBookingsForUserFilter() {
        User testUser = User.createNewPassenger("vikram_test", "vikram@example.com", "9876543210", "Vikram Singh");
        List<Booking> userBookings = bookingService.getBookingsForUser(testUser);
        assertNotNull(userBookings, "Bookings list should not be null");
    }

    @Test
    @DisplayName("Attempting to cancel non-existent PNR returns false")
    void testCancelNonExistentBooking() {
        boolean cancelled = bookingService.cancelBooking("NON-EXISTENT-PNR-999");
        org.junit.jupiter.api.Assertions.assertFalse(cancelled, "Cancelling non-existent PNR should return false");
    }

    @Test
    @DisplayName("Looking up non-existent PNR returns null")
    void testGetNonExistentBooking() {
        Booking b = bookingService.getBookingByPnr("NON-EXISTENT-PNR-999");
        org.junit.jupiter.api.Assertions.assertNull(b, "Non-existent PNR lookup should return null");
    }
}
