package com.trainticket.service;

import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.model.service.BookingService;
import com.trainticket.model.service.CancellationRefundEngine;
import com.trainticket.model.service.CancellationRefundEngine.CancellationBreakdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CancellationRefundTest {

    @Test
    @DisplayName("Flat fee cancellation charge applied when cancelled > 48 hours before departure")
    void testFlatFeeMoreThan48Hours() {
        Booking booking = new Booking(
                101L, "PNR48HRS01", 1L, "user1",
                1L, "12951", "Mumbai Rajdhani",
                LocalDate.now().plusDays(5), "MMCT", "Mumbai Central",
                "NDLS", "New Delhi", "16:35", "08:35",
                "3A", "GENERAL", 2400.0,
                "CONFIRMED", LocalDateTime.now(),
                List.of(
                        BookingPassenger.create("Rahul Sharma", 28, "M", "LOWER", "B1", 12),
                        BookingPassenger.create("Pooja Sharma", 26, "F", "MIDDLE", "B1", 13)
                )
        );

        CancellationBreakdown bd = CancellationRefundEngine.calculateRefund(booking);

        assertTrue(bd.isEligibleForRefund());
        assertTrue(bd.hoursToDeparture() > 48);
        // 2 passengers * 180 for 3A = 360
        assertEquals(360.0, bd.cancellationCharge(), 0.01);
        assertEquals(2040.0, bd.refundAmount(), 0.01);
        assertNotNull(bd.refundReferenceId());
        assertTrue(bd.refundReferenceId().startsWith("RF-REF-"));
    }

    @Test
    @DisplayName("25% cancellation charge applied when cancelled between 12 and 48 hours")
    void testQuarterFeeBetween12And48Hours() {
        Booking booking = new Booking(
                102L, "PNR24HRS01", 1L, "user1",
                1L, "12004", "Lucknow Shatabdi",
                LocalDate.now().plusDays(1), "NDLS", "New Delhi",
                "LKO", "Lucknow", "23:59", "06:30",
                "CC", "GENERAL", 1200.0,
                "CONFIRMED", LocalDateTime.now(),
                List.of(BookingPassenger.create("Amit Verma", 35, "M", "WINDOW", "C1", 4))
        );

        CancellationBreakdown bd = CancellationRefundEngine.calculateRefund(booking);

        assertTrue(bd.isEligibleForRefund());
        // 25% of 1200 = 300, which is > min class fee (180)
        assertEquals(300.0, bd.cancellationCharge(), 0.01);
        assertEquals(900.0, bd.refundAmount(), 0.01);
    }

    @Test
    @DisplayName("Non-refundable when cancelled less than 4 hours before departure")
    void testNonRefundableUnder4Hours() {
        Booking booking = new Booking(
                103L, "PNR02HRS01", 1L, "user1",
                1L, "12002", "Bhopal Shatabdi",
                LocalDate.now(), "NDLS", "New Delhi",
                "AGC", "Agra Cantt", "00:01", "07:50",
                "EC", "GENERAL", 1500.0,
                "CONFIRMED", LocalDateTime.now(),
                List.of(BookingPassenger.create("Priya Sen", 30, "F", "AISLE", "E1", 8))
        );

        CancellationBreakdown bd = CancellationRefundEngine.calculateRefund(booking);

        assertFalse(bd.isEligibleForRefund());
        assertEquals(0.0, bd.refundAmount(), 0.01);
        assertEquals(1500.0, bd.cancellationCharge(), 0.01);
    }

    @Test
    @DisplayName("BookingService successfully cancels booking and releases inventory")
    void testBookingServiceCancelFlow() {
        BookingService service = BookingService.getInstance();
        Booking booking = new Booking(
                999L, "TESTPNR999", 1L, "testuser",
                1L, "12004", "Lucknow Shatabdi",
                LocalDate.now().plusDays(3), "NDLS", "New Delhi",
                "CNB", "Kanpur Central", "06:10", "11:20",
                "CC", "GENERAL", 850.0,
                "CONFIRMED", LocalDateTime.now(),
                List.of(BookingPassenger.create("Vikram Singh", 42, "M", "WINDOW", "C2", 15))
        );

        // Save booking into DAO first
        com.trainticket.model.dao.BookingDAO dao = new com.trainticket.model.dao.BookingDAO();
        dao.saveBooking(booking);

        assertNotNull(dao.findByPnr("TESTPNR999"));
        assertFalse(dao.findByPnr("TESTPNR999").isCancelled());

        // Cancel
        boolean cancelled = service.cancelBooking("TESTPNR999", "Change of itinerary");
        assertTrue(cancelled);

        Booking updated = dao.findByPnr("TESTPNR999");
        assertNotNull(updated);
        assertTrue(updated.isCancelled());
        assertEquals("CANCELLED", updated.getStatus());
        assertEquals("CANCELLED", updated.getPassengers().get(0).getStatus());
    }
}
