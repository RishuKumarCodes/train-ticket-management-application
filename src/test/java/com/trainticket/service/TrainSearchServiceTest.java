package com.trainticket.service;

import com.trainticket.model.CoachAvailability;
import com.trainticket.model.ConcessionType;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.Station;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.TravelQuota;
import com.trainticket.model.service.TrainSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TrainSearchService} validating route evaluation,
 * schedule matching, quota surge calculations, and concession discounts.
 */
public class TrainSearchServiceTest {

    private TrainSearchService searchService;

    @BeforeEach
    public void setUp() {
        searchService = new TrainSearchService();
    }

    @Test
    @DisplayName("Search trains on NDLS to MMCT returns iconic Rajdhani express trains")
    public void testSearchTrainsDirectRoute() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);

        assertNotNull(results, "Results list should not be null");
        assertFalse(results.isEmpty(), "Expected at least 1 train between NDLS and MMCT");

        boolean hasMumbaiRajdhani = results.stream()
                .anyMatch(r -> r.getTrain().getTrainNumber().equals("12952"));
        assertTrue(hasMumbaiRajdhani, "Expected 12952 Mumbai Rajdhani in results");

        TrainSearchResult rajdhani = results.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952"))
                .findFirst().orElseThrow();

        assertEquals("NDLS", rajdhani.getOriginHalt().getStation().getCode());
        assertEquals("MMCT", rajdhani.getDestinationHalt().getStation().getCode());
        assertTrue(rajdhani.getDistanceKm() > 1000, "Distance should be > 1000 km");
        assertNotNull(rajdhani.getFormattedDuration());
    }

    @Test
    @DisplayName("Search trains on NDLS to BSB returns Vande Bharat Express")
    public void testSearchVandeBharatRoute() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("NDLS", "BSB", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);

        assertNotNull(results);
        boolean hasVandeBharat = results.stream()
                .anyMatch(r -> r.getTrain().getTrainNumber().equals("22436"));
        assertTrue(hasVandeBharat, "Expected 22436 Vande Bharat Express in results");
    }

    @Test
    @DisplayName("Tatkal quota applies +30% surge on base fare")
    public void testTatkalQuotaSurgePricing() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery generalQuery = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.NONE, "All Classes");
        TrainSearchQuery tatkalQuery = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.TATKAL, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> generalResults = searchService.search(generalQuery);
        List<TrainSearchResult> tatkalResults = searchService.search(tatkalQuery);

        TrainSearchResult generalRajdhani = generalResults.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();
        TrainSearchResult tatkalRajdhani = tatkalResults.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();

        CoachAvailability general3A = generalRajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();
        CoachAvailability tatkal3A = tatkalRajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();

        double expectedTatkalFare = Math.round(general3A.getBaseFare() * 1.30);
        assertEquals(expectedTatkalFare, tatkal3A.getFinalFare(), 1.0, 
                "Tatkal fare should be 30% higher than base fare");
    }

    @Test
    @DisplayName("Premium Tatkal quota applies +50% surge on base fare")
    public void testPremiumTatkalQuotaSurgePricing() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery generalQuery = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.NONE, "All Classes");
        TrainSearchQuery ptQuery = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.PREMIUM_TATKAL, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> generalResults = searchService.search(generalQuery);
        List<TrainSearchResult> ptResults = searchService.search(ptQuery);

        TrainSearchResult generalRajdhani = generalResults.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();
        TrainSearchResult ptRajdhani = ptResults.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();

        CoachAvailability general3A = generalRajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();
        CoachAvailability pt3A = ptRajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();

        double expectedPtFare = Math.round(general3A.getBaseFare() * 1.50);
        assertEquals(expectedPtFare, pt3A.getFinalFare(), 1.0, 
                "Premium Tatkal fare should be 50% higher than base fare");
    }

    @Test
    @DisplayName("All AC quota filters out non-AC classes")
    public void testAllAcQuotaFiltersOnlyAcCoaches() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("NDLS", "HWH", tomorrow, 
                TravelQuota.ALL_AC, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);
        for (TrainSearchResult result : results) {
            for (CoachAvailability coach : result.getCoachAvailabilities()) {
                assertFalse(coach.getClassCode().equalsIgnoreCase("SL"), 
                        "Sleeper (SL) should not be present in ALL_AC quota results");
                assertFalse(coach.getClassCode().equalsIgnoreCase("2S"), 
                        "Second Sitting (2S) should not be present in ALL_AC quota results");
            }
        }
    }

    @Test
    @DisplayName("Person with Disability concession applies 50% discount")
    public void testPersonWithDisabilityConcession() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.PERSON_WITH_DISABILITY, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);
        TrainSearchResult rajdhani = results.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();

        CoachAvailability coach3A = rajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();

        double expectedDiscountFare = Math.round(coach3A.getBaseFare() * 0.50);
        assertEquals(expectedDiscountFare, coach3A.getFinalFare(), 1.0, 
                "Divyangjan concession should provide a 50% discount on base fare");
    }

    @Test
    @DisplayName("Railway Pass Concession sets statutory fee of 40")
    public void testRailwayPassConcession() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("NDLS", "MMCT", tomorrow, 
                TravelQuota.GENERAL, ConcessionType.RAILWAY_PASS, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);
        TrainSearchResult rajdhani = results.stream()
                .filter(r -> r.getTrain().getTrainNumber().equals("12952")).findFirst().orElseThrow();

        CoachAvailability coach3A = rajdhani.getCoachAvailabilities().stream()
                .filter(c -> c.getClassCode().equals("3A")).findFirst().orElseThrow();

        assertEquals(40.0, coach3A.getFinalFare(), 0.01, 
                "Railway Pass concession should set statutory fee to ₹40");
    }

    @Test
    @DisplayName("Validation throws IllegalArgumentException on null query or blank stations")
    public void testInvalidStationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> searchService.search(null));
        assertThrows(IllegalArgumentException.class, () -> 
                searchService.search(new TrainSearchQuery("", "MMCT", LocalDate.now().plusDays(1), 
                        TravelQuota.GENERAL, ConcessionType.NONE, "All Classes")));
        assertThrows(IllegalArgumentException.class, () -> 
                searchService.search(new TrainSearchQuery("NDLS", "NDLS", LocalDate.now().plusDays(1), 
                        TravelQuota.GENERAL, ConcessionType.NONE, "All Classes")));
    }

    @Test
    @DisplayName("Get train route halts retrieves ordered halt sequence with valid metrics")
    public void testGetTrainRouteHaltSequence() {
        List<RouteHalt> halts = searchService.getTrainRoute("12952");

        assertNotNull(halts);
        assertFalse(halts.isEmpty(), "Route halts should not be empty for 12952");
        assertEquals(1, halts.get(0).getStopSequence(), "First halt should have sequence 1");
        assertEquals("NDLS", halts.get(0).getStation().getCode(), "First station should be NDLS");
        assertEquals("MMCT", halts.get(halts.size() - 1).getStation().getCode(), "Last station should be MMCT");

        // Verify distance strictly increases
        for (int i = 1; i < halts.size(); i++) {
            assertTrue(halts.get(i).getDistanceKm() >= halts.get(i - 1).getDistanceKm(), 
                    "Cumulative distance must be non-decreasing along route");
        }
    }

    @Test
    @DisplayName("Station autocomplete search suggestions match prefix or city")
    public void testAutoCompleteStationSuggestions() {
        List<Station> delhiMatches = searchService.searchStations("Del");
        assertFalse(delhiMatches.isEmpty(), "Expected matches for 'Del'");
        assertTrue(delhiMatches.stream().anyMatch(s -> s.getCode().equals("NDLS")));

        List<Station> codeMatches = searchService.searchStations("MMCT");
        assertFalse(codeMatches.isEmpty(), "Expected matches for 'MMCT'");
        assertEquals("MMCT", codeMatches.get(0).getCode());
    }

    @Test
    @DisplayName("Search trains on intermediate halts (AGC to BPL) returns multiple connecting trains")
    public void testSearchIntermediateHaltsRoute() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainSearchQuery query = new TrainSearchQuery("AGC", "BPL", tomorrow,
                TravelQuota.GENERAL, ConcessionType.NONE, "All Classes");

        List<TrainSearchResult> results = searchService.search(query);

        assertNotNull(results, "Results list should not be null");
        assertFalse(results.isEmpty(), "Expected connecting trains between Agra Cantt and Bhopal");

        // Verify connecting trains like Kerala Express 12626 or Bhopal Shatabdi 12002 are found
        boolean hasConnectingTrain = results.stream()
                .anyMatch(r -> r.getTrain().getTrainNumber().equals("12626") || 
                               r.getTrain().getTrainNumber().equals("12002") ||
                               r.getTrain().getTrainNumber().equals("12138"));
        assertTrue(hasConnectingTrain, "Expected at least one express train stopping at both AGC and BPL");

        for (TrainSearchResult res : results) {
            assertEquals("AGC", res.getOriginHalt().getStation().getCode());
            assertEquals("BPL", res.getDestinationHalt().getStation().getCode());
            assertTrue(res.getDestinationHalt().getStopSequence() > res.getOriginHalt().getStopSequence());
        }
    }
}
