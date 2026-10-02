package com.trainticket.service;

import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.dao.FeaturedDestinationDAO;
import com.trainticket.model.dao.TrainDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class FeaturedDestinationFlowTest {

    @Test
    @DisplayName("Verify default seed featured destinations in DAO")
    public void testDefaultSeedDestinations() {
        FeaturedDestinationDAO dao = FeaturedDestinationDAO.getInstance();
        List<FeaturedDestination> destinations = dao.getAllDestinations();

        assertNotNull(destinations);
        assertTrue(destinations.size() >= 9, "Should contain at least 9 initial destinations");

        boolean hasTajMahal = destinations.stream().anyMatch(d -> d.getSingleLineName().contains("TAJ MAHAL"));
        boolean hasDalLake = destinations.stream().anyMatch(d -> d.getSingleLineName().contains("DAL LAKE"));
        boolean hasMunnar = destinations.stream().anyMatch(d -> d.getSingleLineName().contains("MUNNAR"));

        assertTrue(hasTajMahal, "Should have Taj Mahal");
        assertTrue(hasDalLake, "Should have Dal Lake");
        assertTrue(hasMunnar, "Should have Munnar");
    }

    @Test
    @DisplayName("Verify admin add and remove featured destination lifecycle")
    public void testAddAndRemoveDestinationLifecycle() {
        FeaturedDestinationDAO dao = FeaturedDestinationDAO.getInstance();
        AtomicBoolean listenerFired = new AtomicBoolean(false);
        Runnable listener = () -> listenerFired.set(true);
        dao.addChangeListener(listener);

        int initialCount = dao.getAllDestinations().size();

        FeaturedDestination newDest = new FeaturedDestination(
                0L,
                "GOLDEN TEMPLE",
                "Amritsar, Punjab",
                "ASR",
                "/assets/images/taj-mahal.png",
                "staircase"
        );

        FeaturedDestination added = dao.addDestination(newDest);
        assertNotNull(added);
        assertTrue(added.getId() > 0);
        assertTrue(listenerFired.get(), "Change listener must fire on destination addition");
        assertEquals(initialCount + 1, dao.getAllDestinations().size());

        // Verify removal
        listenerFired.set(false);
        boolean removed = dao.deleteDestination(added.getId());
        assertTrue(removed, "Destination should be successfully deleted");
        assertTrue(listenerFired.get(), "Change listener must fire on destination removal");
        assertEquals(initialCount, dao.getAllDestinations().size());

        dao.removeChangeListener(listener);
    }

    @Test
    @DisplayName("Verify TrainDAO hasRoute connectivity validation")
    public void testTrainRouteConnectivity() {
        TrainDAO trainDAO = TrainDAO.getInstance();

        // NDLS -> AGC (New Delhi to Agra Cantt) is a major corridor (e.g. Rajdhani / Bhopal Express)
        boolean hasNdlsToAgc = trainDAO.hasRoute("NDLS", "AGC");
        assertTrue(hasNdlsToAgc, "Route should exist from NDLS to AGC");

        // Same station must return false
        assertFalse(trainDAO.hasRoute("NDLS", "NDLS"), "Same station route should be false");

        // Null parameters must return false
        assertFalse(trainDAO.hasRoute(null, "AGC"));
        assertFalse(trainDAO.hasRoute("NDLS", null));

        // Invalid / impossible station route
        assertFalse(trainDAO.hasRoute("INVALID1", "INVALID2"));

        // Check hasRouteOnDate
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        boolean runsTomorrow = trainDAO.hasRouteOnDate("NDLS", "AGC", tomorrow);
        // Trains like 12002 / 12952 / 12954 run daily (1111111)
        assertTrue(runsTomorrow, "Daily train route should run tomorrow");
    }

    @Test
    @DisplayName("Verify FeaturedDestination single line name formatting")
    public void testDestinationFormatting() {
        FeaturedDestination multiline = new FeaturedDestination(
                1L, "STATUE\nOF\nUNITY", "Kevadia, Gujarat", "BRC", "/assets/images/statue-of-unity.png", "right"
        );
        assertEquals("STATUE OF UNITY", multiline.getSingleLineName());
        assertEquals("BRC", multiline.getStationCode());
        assertEquals("Kevadia, Gujarat", multiline.getLocationText());
    }
}
