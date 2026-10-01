package com.trainticket.model;

import java.util.Objects;

/**
 * Immutable value object representing seating inventory, status, and pricing
 * for a specific coach class under an active quota and concession.
 */
public final class CoachAvailability {

    private final String classCode; // 1A, 2A, 3A, SL, CC, EC
    private final String quotaCode; // GENERAL, TATKAL, etc.
    private final int totalSeats;
    private final int availableSeats;
    private final int racSeats;
    private final int waitlistSeats;
    private final double baseFare;
    private final double finalFare;

    public CoachAvailability(String classCode, String quotaCode, int totalSeats, 
                             int availableSeats, int racSeats, int waitlistSeats, 
                             double baseFare, double finalFare) {
        this.classCode = Objects.requireNonNull(classCode, "Class code cannot be null").toUpperCase().trim();
        this.quotaCode = quotaCode != null ? quotaCode : "GENERAL";
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.racSeats = racSeats;
        this.waitlistSeats = waitlistSeats;
        this.baseFare = baseFare;
        this.finalFare = finalFare;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getQuotaCode() {
        return quotaCode;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public int getRacSeats() {
        return racSeats;
    }

    public int getWaitlistSeats() {
        return waitlistSeats;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public double getFinalFare() {
        return finalFare;
    }

    /**
     * Determines whether seats are available, in RAC, or waitlisted.
     */
    public AvailabilityStatus getStatus() {
        if (availableSeats > 0) {
            return AvailabilityStatus.AVAILABLE;
        } else if (racSeats > 0) {
            return AvailabilityStatus.RAC;
        } else {
            return AvailabilityStatus.WAITLIST;
        }
    }

    /**
     * Returns a human-friendly status badge label (e.g. "AVAILABLE 42", "RAC 12", "WL 24").
     */
    public String getStatusBadgeText() {
        return switch (getStatus()) {
            case AVAILABLE -> "AVAILABLE - " + availableSeats;
            case RAC -> "RAC - " + racSeats;
            case WAITLIST -> "WL - " + (waitlistSeats > 0 ? waitlistSeats : 1);
        };
    }

    public String getStatusFormatted() {
        return getStatusBadgeText();
    }

    public String getFullClassName() {
        return switch (classCode) {
            case "1A" -> "AC First Class (1A)";
            case "2A" -> "AC 2 Tier (2A)";
            case "3A" -> "AC 3 Tier (3A)";
            case "SL" -> "Sleeper (SL)";
            case "CC" -> "AC Chair Car (CC)";
            case "EC" -> "Exec. Chair Car (EC)";
            default -> classCode;
        };
    }

    public enum AvailabilityStatus {
        AVAILABLE,
        RAC,
        WAITLIST
    }
}
