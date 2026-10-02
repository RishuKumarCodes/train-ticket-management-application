package com.trainticket.service;

import com.trainticket.view.component.seat.CoachSeatMapPanel;
import com.trainticket.view.component.seat.SeatButton;
import com.trainticket.view.dialog.CoachSeatSelectionDialog.SelectedSeat;
import com.trainticket.view.dialog.PaymentGatewayDialog.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Tier 1 Interactive Coach Seat Map and Mock Payment Gateway models.
 */
public class SeatAndPaymentTest {

    @Test
    @DisplayName("SeatButton initializes with correct berth abbreviation and state")
    void testSeatButtonCreation() {
        SeatButton lb = new SeatButton("B2", 34, "LOWER", SeatButton.SeatState.AVAILABLE);
        assertEquals("B2", lb.getCoachNumber());
        assertEquals(34, lb.getSeatNumber());
        assertEquals("LB", lb.getBerthAbbr());
        assertEquals(SeatButton.SeatState.AVAILABLE, lb.getSeatState());

        SeatButton sl = new SeatButton("B1", 12, "SIDE LOWER", SeatButton.SeatState.AVAILABLE);
        assertEquals("SL", sl.getBerthAbbr());

        SeatButton su = new SeatButton("B1", 13, "SIDE UPPER", SeatButton.SeatState.AVAILABLE);
        assertEquals("SU", su.getBerthAbbr());

        SeatButton mb = new SeatButton("B3", 20, "MIDDLE", SeatButton.SeatState.AVAILABLE);
        assertEquals("MB", mb.getBerthAbbr());

        SeatButton ub = new SeatButton("B3", 21, "UPPER", SeatButton.SeatState.AVAILABLE);
        assertEquals("UB", ub.getBerthAbbr());
    }

    @Test
    @DisplayName("SeatButton toggles states and respects BOOKED disabled status")
    void testSeatButtonStateTransitions() {
        SeatButton btn = new SeatButton("B2", 15, "LOWER", SeatButton.SeatState.AVAILABLE);
        assertTrue(btn.isEnabled());

        btn.setSeatState(SeatButton.SeatState.SELECTED);
        assertEquals(SeatButton.SeatState.SELECTED, btn.getSeatState());
        assertTrue(btn.isEnabled());

        btn.setSeatState(SeatButton.SeatState.BOOKED);
        assertEquals(SeatButton.SeatState.BOOKED, btn.getSeatState());
        assertFalse(btn.isEnabled());
    }

    @Test
    @DisplayName("CoachSeatMapPanel enforces max selection capacity constraint")
    void testCoachSeatMapCapacity() {
        int maxPax = 2;
        CoachSeatMapPanel map = new CoachSeatMapPanel("3A", "B2", maxPax, null);

        // Pre-select 2 seats
        map.preSelectSeat(1);
        map.preSelectSeat(2);

        List<SeatButton> selected = map.getSelectedSeats();
        assertEquals(2, selected.size());

        // Pre-select a 3rd seat — should roll over and keep size at 2
        map.preSelectSeat(3);
        assertEquals(2, map.getSelectedSeats().size());

        // Clear selection
        map.clearSelection();
        assertEquals(0, map.getSelectedSeats().size());
    }

    @Test
    @DisplayName("SelectedSeat record preserves coach, seat number, and berth type")
    void testSelectedSeatRecord() {
        SelectedSeat seat = new SelectedSeat("B2", 34, "LOWER");
        assertEquals("B2", seat.coachNumber());
        assertEquals(34, seat.seatNumber());
        assertEquals("LOWER", seat.berthType());
    }

    @Test
    @DisplayName("PaymentResult record retains transaction ID and payment method")
    void testPaymentResultRecord() {
        PaymentResult res = new PaymentResult("TXN-123456789", "UPI / QR", 2080.0, "rishu@okaxis");
        assertEquals("TXN-123456789", res.transactionId());
        assertEquals("UPI / QR", res.paymentMethod());
        assertEquals(2080.0, res.amountPaid(), 0.01);
        assertEquals("rishu@okaxis", res.paymentReference());
    }
}
