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

    public BookingService() {
        this.bookingDAO = new BookingDAO();
    }

    public static BookingService getInstance() {
        return INSTANCE;
    }

    public Booking createBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        return bookingDAO.saveBooking(booking);
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

    public boolean cancelBooking(String pnr) {
        return bookingDAO.cancelBooking(pnr);
    }
}
