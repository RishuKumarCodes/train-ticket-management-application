package com.trainticket.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing a confirmed or cancelled train ticket reservation.
 * Pure model with zero UI dependencies.
 */
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String pnr;
    private final Long userId;
    private final String userIdentifier;
    private final Long trainId;
    private final String trainNumber;
    private final String trainName;
    private final LocalDate journeyDate;
    private final String fromStationCode;
    private final String fromStationName;
    private final String toStationCode;
    private final String toStationName;
    private final String departureTime;
    private final String arrivalTime;
    private final String classCode;
    private final String quotaCode;
    private final double totalFare;
    private final String status; // CONFIRMED, CANCELLED
    private final LocalDateTime createdAt;
    private final List<BookingPassenger> passengers;

    public Booking(Long id, String pnr, Long userId, String userIdentifier,
                   Long trainId, String trainNumber, String trainName,
                   LocalDate journeyDate, String fromStationCode, String fromStationName,
                   String toStationCode, String toStationName,
                   String departureTime, String arrivalTime,
                   String classCode, String quotaCode, double totalFare,
                   String status, LocalDateTime createdAt, List<BookingPassenger> passengers) {
        this.id = id;
        this.pnr = Objects.requireNonNull(pnr, "PNR cannot be null").trim();
        this.userId = userId;
        this.userIdentifier = userIdentifier != null ? userIdentifier.trim() : null;
        this.trainId = trainId;
        this.trainNumber = trainNumber != null ? trainNumber.trim() : "";
        this.trainName = trainName != null ? trainName.trim() : "";
        this.journeyDate = journeyDate != null ? journeyDate : LocalDate.now().plusDays(1);
        this.fromStationCode = fromStationCode != null ? fromStationCode.trim() : "";
        this.fromStationName = fromStationName != null ? fromStationName.trim() : fromStationCode;
        this.toStationCode = toStationCode != null ? toStationCode.trim() : "";
        this.toStationName = toStationName != null ? toStationName.trim() : toStationCode;
        this.departureTime = departureTime != null ? departureTime.trim() : "--:--";
        this.arrivalTime = arrivalTime != null ? arrivalTime.trim() : "--:--";
        this.classCode = classCode != null ? classCode.trim() : "3A";
        this.quotaCode = quotaCode != null ? quotaCode.trim() : "GENERAL";
        this.totalFare = Math.max(0.0, totalFare);
        this.status = status != null ? status.trim().toUpperCase() : "CONFIRMED";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public String getPnr() {
        return pnr;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public Long getTrainId() {
        return trainId;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public String getFromStationCode() {
        return fromStationCode;
    }

    public String getFromStationName() {
        return fromStationName;
    }

    public String getToStationCode() {
        return toStationCode;
    }

    public String getToStationName() {
        return toStationName;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getQuotaCode() {
        return quotaCode;
    }

    public double getTotalFare() {
        return totalFare;
    }

    public String getStatus() {
        return status;
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<BookingPassenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public Booking withStatus(String newStatus) {
        return new Booking(id, pnr, userId, userIdentifier, trainId, trainNumber, trainName,
                journeyDate, fromStationCode, fromStationName, toStationCode, toStationName,
                departureTime, arrivalTime, classCode, quotaCode, totalFare, newStatus, createdAt, passengers);
    }
}
