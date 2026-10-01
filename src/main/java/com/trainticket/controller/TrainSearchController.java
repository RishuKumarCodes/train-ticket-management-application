package com.trainticket.controller;

import com.trainticket.model.RouteHalt;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.service.TrainSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller bridging Search Views and TrainSearchService.
 * Executes queries on background worker threads with guaranteed EDT callback dispatch,
 * preventing any UI freeze or frame drops.
 */
public class TrainSearchController {

    private static final Logger logger = LoggerFactory.getLogger(TrainSearchController.class);

    private final TrainSearchService searchService;

    public TrainSearchController() {
        this(new TrainSearchService());
    }

    public TrainSearchController(TrainSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Executes asynchronous train search.
     *
     * @param query     Search criteria
     * @param onSuccess Callback receiving matching results on the EDT
     * @param onError   Callback receiving error message on the EDT
     */
    public void searchTrains(TrainSearchQuery query, 
                             Consumer<List<TrainSearchResult>> onSuccess, 
                             Consumer<String> onError) {
        new SwingWorker<List<TrainSearchResult>, Void>() {
            @Override
            protected List<TrainSearchResult> doInBackground() throws Exception {
                return searchService.search(query);
            }

            @Override
            protected void done() {
                try {
                    List<TrainSearchResult> results = get();
                    SwingUtilities.invokeLater(() -> {
                        if (onSuccess != null) {
                            onSuccess.accept(results);
                        }
                    });
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() != null ? cause.getMessage() : "Search failed. Please try again.";
                    logger.error("Error executing train search query: {}", message, cause);
                    SwingUtilities.invokeLater(() -> {
                        if (onError != null) {
                            onError.accept(message);
                        }
                    });
                }
            }
        }.execute();
    }

    /**
     * Asynchronously loads route halts for a specific train.
     */
    public void fetchRoute(String trainNumber, 
                           Consumer<List<RouteHalt>> onSuccess, 
                           Consumer<String> onError) {
        new SwingWorker<List<RouteHalt>, Void>() {
            @Override
            protected List<RouteHalt> doInBackground() throws Exception {
                return searchService.getTrainRoute(trainNumber);
            }

            @Override
            protected void done() {
                try {
                    List<RouteHalt> halts = get();
                    SwingUtilities.invokeLater(() -> {
                        if (onSuccess != null) {
                            onSuccess.accept(halts);
                        }
                    });
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() != null ? cause.getMessage() : "Failed to load route.";
                    logger.error("Error fetching route for train {}: {}", trainNumber, message, cause);
                    SwingUtilities.invokeLater(() -> {
                        if (onError != null) {
                            onError.accept(message);
                        }
                    });
                }
            }
        }.execute();
    }

    public TrainSearchService getSearchService() {
        return searchService;
    }
}
