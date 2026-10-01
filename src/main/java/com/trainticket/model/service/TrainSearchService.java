package com.trainticket.model.service;

import com.trainticket.model.RouteHalt;
import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.model.dao.TrainDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating business rules, station validation, and route search algorithms.
 * Pure Java domain layer with zero UI dependencies.
 */
public class TrainSearchService {

    private static final Logger logger = LoggerFactory.getLogger(TrainSearchService.class);

    private final StationDAO stationDAO;
    private final TrainDAO trainDAO;

    public TrainSearchService() {
        this(new StationDAO(), new TrainDAO());
    }

    public TrainSearchService(StationDAO stationDAO, TrainDAO trainDAO) {
        this.stationDAO = stationDAO;
        this.trainDAO = trainDAO;
    }

    /**
     * Validates input parameters and performs multi-criteria train route matching.
     *
     * @param query Multi-criteria search query
     * @return List of matching train results
     * @throws IllegalArgumentException on invalid query parameters
     */
    public List<TrainSearchResult> search(TrainSearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Search query cannot be null.");
        }

        String from = query.getFromStationCode().trim().toUpperCase();
        String to = query.getToStationCode().trim().toUpperCase();

        if (from.isEmpty() || to.isEmpty()) {
            throw new IllegalArgumentException("Please select both Origin and Destination stations.");
        }

        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Origin and Destination stations cannot be the same.");
        }

        if (query.getJourneyDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Journey date cannot be in the past.");
        }

        // Verify that stations exist in network
        if (stationDAO.findByCode(from).isEmpty()) {
            throw new IllegalArgumentException("Origin station code '" + from + "' is not recognized.");
        }

        if (stationDAO.findByCode(to).isEmpty()) {
            throw new IllegalArgumentException("Destination station code '" + to + "' is not recognized.");
        }

        logger.info("Executing train search: {} -> {} on {} [Quota={}, Concession={}]",
                from, to, query.getJourneyDate(), query.getQuota(), query.getConcession());

        List<TrainSearchResult> results = trainDAO.searchTrains(query);
        logger.info("Found {} trains matching search {} -> {}.", results.size(), from, to);
        return results;
    }

    /**
     * Auto-completes station entries based on user text query.
     */
    public List<Station> searchStations(String userQuery) {
        return stationDAO.searchStations(userQuery);
    }

    /**
     * Retrieves all network stations.
     */
    public List<Station> getAllStations() {
        return stationDAO.getAllStations();
    }

    /**
     * Retrieves route halts and timetable for a train.
     */
    public List<RouteHalt> getTrainRoute(String trainNumber) {
        return trainDAO.getTrainRoute(trainNumber);
    }

    /**
     * Retrieves full train details.
     */
    public Optional<Train> getTrain(String trainNumber) {
        return trainDAO.getTrainByNumber(trainNumber);
    }
}
