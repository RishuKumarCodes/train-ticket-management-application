package com.trainticket.model.service;

import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.User;
import com.trainticket.model.dao.BookingDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing ticket reservations, passenger bookings, and cancellations.
 */
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private static final BookingService INSTANCE = new BookingService();

    private final BookingDAO bookingDAO;
    private final com.trainticket.model.dao.TrainDAO trainDAO;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.trainDAO = new com.trainticket.model.dao.TrainDAO();
    }

    public static BookingService getInstance() {
        return INSTANCE;
    }

    public Booking createBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        Booking saved = bookingDAO.saveBooking(booking);
        int passengerCount = (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) 
                ? booking.getPassengers().size() : 1;
        trainDAO.decrementSeatInventory(booking.getTrainNumber(), booking.getClassCode(), passengerCount);
        return saved;
    }

    public List<Booking> getBookingsForUser(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        return bookingDAO.findByUserIdOrIdentifier(user.getId(),
                user.getEmail() != null ? user.getEmail() : user.getPhone());
    }

    public List<Booking> getBookingsForCurrentSession() {
        AuthSession session = AuthSession.getInstance();
        if (session.isGuest()) {
            return new ArrayList<>();
        }
        return getBookingsForUser(session.getCurrentUser());
    }

    public List<Booking> getAllBookings() {
        return bookingDAO.findAll();
    }

    public Booking getBookingByPnr(String pnr) {
        return bookingDAO.findByPnr(pnr);
    }

    public CancellationRefundEngine.CancellationBreakdown getCancellationBreakdown(String pnr) {
        Booking booking = bookingDAO.findByPnr(pnr);
        return CancellationRefundEngine.calculateRefund(booking);
    }

    public boolean cancelBooking(String pnr) {
        return cancelBooking(pnr, "User requested cancellation");
    }

    public boolean cancelBooking(String pnr, String reason) {
        Booking booking = bookingDAO.findByPnr(pnr);
        if (booking == null || booking.isCancelled()) {
            return false;
        }

        boolean success = bookingDAO.cancelBooking(pnr);
        if (success) {
            int count = (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) 
                    ? booking.getPassengers().size() : 1;
            trainDAO.incrementSeatInventory(booking.getTrainNumber(), booking.getClassCode(), count);
        }
        return success;
    }
}
