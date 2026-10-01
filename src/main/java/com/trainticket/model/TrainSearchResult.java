package com.trainticket.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the evaluation of a matching train between two stations,
 * including boarding/alighting halts, journey duration, seat availability, and fares.
 */
public final class TrainSearchResult {

    private final Train train;
    private final RouteHalt originHalt;
    private final RouteHalt destinationHalt;
    private final List<CoachAvailability> coachAvailabilities;

    public TrainSearchResult(Train train, RouteHalt originHalt, RouteHalt destinationHalt, 
                             List<CoachAvailability> coachAvailabilities) {
        this.train = Objects.requireNonNull(train, "Train cannot be null");
        this.originHalt = Objects.requireNonNull(originHalt, "Origin halt cannot be null");
        this.destinationHalt = Objects.requireNonNull(destinationHalt, "Destination halt cannot be null");
        this.coachAvailabilities = coachAvailabilities != null 
                ? Collections.unmodifiableList(new ArrayList<>(coachAvailabilities)) 
                : Collections.emptyList();
    }

    public Train getTrain() {
        return train;
    }

    public RouteHalt getOriginHalt() {
        return originHalt;
    }

    public RouteHalt getDestinationHalt() {
        return destinationHalt;
    }

    public List<CoachAvailability> getCoachAvailabilities() {
        return coachAvailabilities;
    }

    public LocalTime getDepartureTime() {
        return originHalt.getDepartureTime();
    }

    public LocalTime getArrivalTime() {
        return destinationHalt.getArrivalTime();
    }

    public int getDistanceKm() {
        return Math.max(0, destinationHalt.getDistanceKm() - originHalt.getDistanceKm());
    }

    public Duration calculateDuration() {
        LocalTime dep = getDepartureTime();
        LocalTime arr = getArrivalTime();
        if (dep == null || arr == null) {
            return Duration.ZERO;
        }

        int dayDiff = Math.max(0, destinationHalt.getDayCount() - originHalt.getDayCount());
        long depSeconds = dep.toSecondOfDay();
        long arrSeconds = arr.toSecondOfDay() + (dayDiff * 86400L);
        long diffSeconds = arrSeconds - depSeconds;

        if (diffSeconds < 0) {
            diffSeconds += 86400L;
        }
        return Duration.ofSeconds(diffSeconds);
    }

    public String getFormattedDuration() {
        Duration d = calculateDuration();
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        return hours + "h " + (minutes > 0 ? minutes + "m" : "00m");
    }

    public long getDurationMinutes() {
        return calculateDuration().toMinutes();
    }

    public String getDayOffsetLabel() {
        int diff = destinationHalt.getDayCount() - originHalt.getDayCount();
        if (diff == 1) return "+1 day";
        if (diff > 1) return "+" + diff + " days";
        return "";
    }
}
