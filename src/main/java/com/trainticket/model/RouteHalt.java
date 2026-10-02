package com.trainticket.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable entity representing a single scheduled station halt along a train's route.
 */
public final class RouteHalt {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final int stopSequence;
    private final Station station;
    private final LocalTime arrivalTime;   // null for origin station
    private final LocalTime departureTime; // null for terminus station
    private final int haltMinutes;
    private final int distanceKm;
    private final int dayCount;
    private final String platformNumber;

    public RouteHalt(int stopSequence, Station station, LocalTime arrivalTime, 
                     LocalTime departureTime, int haltMinutes, int distanceKm, int dayCount) {
        this(stopSequence, station, arrivalTime, departureTime, haltMinutes, distanceKm, dayCount, 
             String.valueOf(((stopSequence * 3 + station.getCode().hashCode()) & 0x7FFFFFFF) % 12 + 1));
    }

    public RouteHalt(int stopSequence, Station station, LocalTime arrivalTime, 
                     LocalTime departureTime, int haltMinutes, int distanceKm, int dayCount, String platformNumber) {
        this.stopSequence = stopSequence;
        this.station = Objects.requireNonNull(station, "Halt station cannot be null");
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.haltMinutes = haltMinutes;
        this.distanceKm = distanceKm;
        this.dayCount = dayCount;
        this.platformNumber = (platformNumber != null && !platformNumber.isBlank()) ? platformNumber.trim() : "1";
    }

    public String getPlatformNumber() {
        return platformNumber;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public Station getStation() {
        return station;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public int getHaltMinutes() {
        return haltMinutes;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public int getDayCount() {
        return dayCount;
    }

    public boolean isOrigin() {
        return arrivalTime == null;
    }

    public boolean isTerminus() {
        return departureTime == null;
    }

    public String getFormattedArrivalTime() {
        return arrivalTime != null ? arrivalTime.format(TIME_FMT) : "--:--";
    }

    public String getFormattedDepartureTime() {
        return departureTime != null ? departureTime.format(TIME_FMT) : "--:--";
    }

    public String getHaltSummary() {
        if (isOrigin()) return "Origin";
        if (isTerminus()) return "Terminus";
        return haltMinutes + " min" + (haltMinutes > 1 ? "s" : "");
    }
}
