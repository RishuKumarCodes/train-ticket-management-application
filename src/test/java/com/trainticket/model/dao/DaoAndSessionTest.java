package com.trainticket.model.dao;

import com.trainticket.model.AuthSession;
import com.trainticket.model.AuthStateListener;
import com.trainticket.model.ConcessionType;
import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.TravelQuota;
import com.trainticket.model.User;
import com.trainticket.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying DAOs and AuthSession state management.
 */
public class DaoAndSessionTest {

    private final StationDAO stationDAO = new StationDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final UserDAO userDAO = new UserDAO();

    @Test
    @DisplayName("StationDAO retrieves master stations and autocomplete suggestions")
    void testStationDAO() {
        List<Station> all = stationDAO.getAllStations();
        assertNotNull(all);
        assertTrue(all.size() >= 18, "Master station catalog should have at least 18 stations");

        Optional<Station> ndls = stationDAO.findByCode("NDLS");
        assertTrue(ndls.isPresent(), "Should retrieve New Delhi by station code");
        assertEquals("New Delhi", ndls.get().getName());

        List<Station> results = stationDAO.searchStations("Mum");
        assertFalse(results.isEmpty(), "Fuzzy search for 'Mum' should return Mumbai stations");
        assertTrue(results.stream().anyMatch(s -> s.getCode().equals("MMCT")));
    }

    @Test
    @DisplayName("TrainDAO retrieves train fleet and searches direct routes")
    void testTrainDAO() {
        List<Train> fleet = trainDAO.getAllTrains();
        assertNotNull(fleet);
        assertTrue(fleet.size() >= 7, "Master fleet should contain at least 7 trains");

        Optional<Train> rajdhani = trainDAO.getTrainByNumber("12952");
        assertTrue(rajdhani.isPresent(), "Should find train 12952");
        assertTrue(rajdhani.get().getName().contains("Rajdhani"));

        TrainSearchQuery query = new TrainSearchQuery("NDLS", "MMCT", LocalDate.now().plusDays(1), TravelQuota.GENERAL, ConcessionType.NONE);
        List<TrainSearchResult> matching = trainDAO.searchTrains(query);
        assertFalse(matching.isEmpty(), "Should match trains between NDLS and MMCT");
    }

    @Test
    @DisplayName("UserDAO persistence and lookup by email or username")
    void testUserDAO() throws Exception {
        Optional<UserRecord> admin = userDAO.findByUsernameOrEmail("admin");
        assertTrue(admin.isPresent(), "Default admin user should exist in repository");
        assertEquals(UserRole.ADMIN, admin.get().user().getRole());

        String uniqueEmail = "test_" + System.currentTimeMillis() + "@railflow.test";
        User newUser = User.createNewPassenger("user_" + System.currentTimeMillis(), uniqueEmail, "+919876543210", "Test Passenger");
        User saved = userDAO.createUser(newUser, "dummyhash123", "dummysalt123");
        assertNotNull(saved);

        Optional<UserRecord> found = userDAO.findByUsernameOrEmail(uniqueEmail);
        assertTrue(found.isPresent(), "Newly saved user should be retrievable by email");
    }

    @Test
    @DisplayName("AuthSession login, role transition, logout and listener notification")
    void testAuthSession() {
        AuthSession session = AuthSession.getInstance();
        session.logout();
        assertTrue(session.isGuest());
        assertFalse(session.isAuthenticated());
        assertFalse(session.isAdmin());

        AtomicBoolean listenerFired = new AtomicBoolean(false);
        AuthStateListener listener = (user, role) -> listenerFired.set(true);
        session.addAuthStateListener(listener);

        try {
            User passenger = User.createNewPassenger("passenger1", "p1@example.com", "+919999999999", "Passenger One");
            session.login(passenger);

            assertTrue(session.isAuthenticated());
            assertTrue(session.isPassenger());
            assertFalse(session.isAdmin());
            assertEquals("passenger1", session.getCurrentUser().getUsername());
            assertTrue(listenerFired.get(), "Listener should be notified on login");

            listenerFired.set(false);
            session.logout();
            assertTrue(session.isGuest());
            assertFalse(session.isAuthenticated());
            assertTrue(listenerFired.get(), "Listener should be notified on logout");
        } finally {
            session.removeAuthStateListener(listener);
            session.logout();
        }
    }

    @Test
    @DisplayName("StationDAO fuzzy search by city name matches expected station hubs")
    void testStationDaoFuzzyCitySearch() {
        List<Station> delhiStations = stationDAO.searchStations("Delhi");
        assertFalse(delhiStations.isEmpty(), "Searching 'Delhi' should return Delhi stations");
        assertTrue(delhiStations.stream().anyMatch(s -> s.getCode().equals("NDLS")));

        List<Station> bengaluruStations = stationDAO.searchStations("Bengaluru");
        assertFalse(bengaluruStations.isEmpty(), "Searching 'Bengaluru' should return SBC");
        assertTrue(bengaluruStations.stream().anyMatch(s -> s.getCode().equals("SBC")));
    }

    @Test
    @DisplayName("TrainDAO lookup returns empty optional for non-existent train")
    void testTrainDaoNonExistentLookup() {
        Optional<Train> invalid = trainDAO.getTrainByNumber("99999");
        assertTrue(invalid.isEmpty(), "Non-existent train number should return Optional.empty()");
    }

    @Test
    @DisplayName("UserDAO existence checks by username and email")
    void testUserDaoDuplicateChecks() {
        assertTrue(userDAO.existsByUsername("admin"), "Admin username should exist");
        assertFalse(userDAO.existsByUsername("random_ghost_user_12345"), "Random username should not exist");
        assertTrue(userDAO.existsByEmail("admin@railflow.internal"), "Admin email should exist");
        assertFalse(userDAO.existsByEmail("ghost@notfound.test"), "Random email should not exist");
    }
}
