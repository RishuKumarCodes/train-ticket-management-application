package com.trainticket.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Encapsulates the multi-criteria search parameters submitted by the passenger.
 */
public final class TrainSearchQuery {

    private final String fromStationCode;
    private final String toStationCode;
    private final LocalDate journeyDate;
    private final TravelQuota quota;
    private final ConcessionType concession;
    private final String preferredClass;

    public TrainSearchQuery(String fromStationCode, String toStationCode, LocalDate journeyDate, 
                            TravelQuota quota, ConcessionType concession, String preferredClass) {
        this.fromStationCode = Objects.requireNonNull(fromStationCode, "Origin station code cannot be null").toUpperCase().trim();
        this.toStationCode = Objects.requireNonNull(toStationCode, "Destination station code cannot be null").toUpperCase().trim();
        this.journeyDate = Objects.requireNonNull(journeyDate, "Journey date cannot be null");
        this.quota = quota != null ? quota : TravelQuota.GENERAL;
        this.concession = concession != null ? concession : ConcessionType.NONE;
        this.preferredClass = (preferredClass != null && !preferredClass.isBlank()) ? preferredClass.trim() : "All Classes";
    }

    public TrainSearchQuery(String fromStationCode, String toStationCode, LocalDate journeyDate, 
                            TravelQuota quota, ConcessionType concession) {
        this(fromStationCode, toStationCode, journeyDate, quota, concession, "All Classes");
    }

    public String getFromStationCode() {
        return fromStationCode;
    }

    public String getToStationCode() {
        return toStationCode;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public TravelQuota getQuota() {
        return quota;
    }

    public ConcessionType getConcession() {
        return concession;
    }

    public String getPreferredClass() {
        return preferredClass;
    }

    @Override
    public String toString() {
        return "TrainSearchQuery{" +
                "from='" + fromStationCode + '\'' +
                ", to='" + toStationCode + '\'' +
                ", date=" + journeyDate +
                ", quota=" + quota +
                ", concession=" + concession +
                ", class='" + preferredClass + '\'' +
                '}';
    }
}
