package com.trainticket.model;

/**
 * Operational running status of a scheduled train.
 */
public enum TrainStatus {
    ON_TIME("ON TIME"),
    DEPARTED("DEPARTED"),
    DELAYED("DELAYED"),
    CANCELLED("CANCELLED");

    private final String label;

    TrainStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
