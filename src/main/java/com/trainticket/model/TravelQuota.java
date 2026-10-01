package com.trainticket.model;

/**
 * Reservation Quota types supported in RailFlow search and booking.
 */
public enum TravelQuota {
    GENERAL("General (GN)", "GN", 1.00),
    ALL_AC("All AC Classes", "AC", 1.00),
    TATKAL("Tatkal Quota (TQ)", "TQ", 1.30), // 30% premium surge on base fare
    PREMIUM_TATKAL("Premium Tatkal (PT)", "PT", 1.50); // Dynamic premium pool

    private final String displayName;
    private final String code;
    private final double fareMultiplier;

    TravelQuota(String displayName, String code, double fareMultiplier) {
        this.displayName = displayName;
        this.code = code;
        this.fareMultiplier = fareMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public double getFareMultiplier() {
        return fareMultiplier;
    }

    public static TravelQuota fromCode(String code) {
        if (code == null) return GENERAL;
        for (TravelQuota q : values()) {
            if (q.code.equalsIgnoreCase(code) || q.name().equalsIgnoreCase(code)) {
                return q;
            }
        }
        return GENERAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
