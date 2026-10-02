package com.trainticket.model;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable domain model representing a train entity in the RailFlow network.
 */
public final class Train {

    private final long id;
    private final String trainNumber;
    private final String name;
    private final TrainType type;
    private final Station sourceStation;
    private final Station destStation;
    private final String runsOnDays; // 7-character bitmask "1111111" (Mon..Sun)
    private final TrainStatus status;
    private final int delayMinutes;
    private final List<RouteHalt> routeHalts;
    private final List<CoachAvailability> coachClasses;

    public Train(long id, String trainNumber, String name, TrainType type, 
                 Station sourceStation, Station destStation, String runsOnDays, 
                 TrainStatus status, List<RouteHalt> routeHalts, List<CoachAvailability> coachClasses) {
        this(id, trainNumber, name, type, sourceStation, destStation, runsOnDays, status, 0, routeHalts, coachClasses);
    }

    public Train(long id, String trainNumber, String name, TrainType type, 
                 Station sourceStation, Station destStation, String runsOnDays, 
                 TrainStatus status, int delayMinutes, List<RouteHalt> routeHalts, List<CoachAvailability> coachClasses) {
        this.id = id;
        this.trainNumber = Objects.requireNonNull(trainNumber, "Train number cannot be null").trim();
        this.name = Objects.requireNonNull(name, "Train name cannot be null").trim();
        this.type = type != null ? type : TrainType.EXPRESS;
        this.sourceStation = Objects.requireNonNull(sourceStation, "Source station cannot be null");
        this.destStation = Objects.requireNonNull(destStation, "Destination station cannot be null");
        this.runsOnDays = (runsOnDays != null && runsOnDays.length() == 7) ? runsOnDays : "1111111";
        this.status = status != null ? status : TrainStatus.ON_TIME;
        this.delayMinutes = Math.max(0, delayMinutes);
        this.routeHalts = routeHalts != null ? Collections.unmodifiableList(new ArrayList<>(routeHalts)) : Collections.emptyList();
        this.coachClasses = coachClasses != null ? Collections.unmodifiableList(new ArrayList<>(coachClasses)) : Collections.emptyList();
    }

    public long getId() {
        return id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getName() {
        return name;
    }

    public TrainType getType() {
        return type;
    }

    public Station getSourceStation() {
        return sourceStation;
    }

    public Station getDestStation() {
        return destStation;
    }

    public String getRunsOnDays() {
        return runsOnDays;
    }

    public TrainStatus getStatus() {
        return status;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public String getFormattedDelay() {
        if (status == TrainStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (status == TrainStatus.DEPARTED) {
            return "DEPARTED";
        }
        if (delayMinutes > 0) {
            return "+" + delayMinutes + "m Delay";
        }
        return "On Time";
    }

    public Train withStatus(TrainStatus newStatus, int newDelayMinutes) {
        return new Train(id, trainNumber, name, type, sourceStation, destStation, runsOnDays, newStatus, newDelayMinutes, routeHalts, coachClasses);
    }

    public Train withCoachClasses(List<CoachAvailability> newClasses) {
        return new Train(id, trainNumber, name, type, sourceStation, destStation, runsOnDays, status, delayMinutes, routeHalts, newClasses);
    }

    public List<RouteHalt> getRouteHalts() {
        return routeHalts;
    }

    public List<CoachAvailability> getCoachClasses() {
        return coachClasses;
    }

    /**
     * Checks if this train runs on the specified DayOfWeek.
     * Index 0 = Monday, Index 6 = Sunday.
     */
    public boolean runsOn(DayOfWeek dayOfWeek) {
        int index = dayOfWeek.getValue() - 1; // 1 (Mon) -> 0, 7 (Sun) -> 6
        if (index >= 0 && index < runsOnDays.length()) {
            return runsOnDays.charAt(index) == '1';
        }
        return false;
    }

    public String getFormattedRunningDays() {
        if ("1111111".equals(runsOnDays)) {
            return "Runs Daily";
        }
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<String> active = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (runsOnDays.charAt(i) == '1') {
                active.add(days[i]);
            }
        }
        return "Runs on: " + String.join(", ", active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Train other)) return false;
        return trainNumber.equalsIgnoreCase(other.trainNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainNumber.toUpperCase());
    }

    @Override
    public String toString() {
        return trainNumber + " • " + name;
    }
}
