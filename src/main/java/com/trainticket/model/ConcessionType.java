package com.trainticket.model;

/**
 * Passenger concession categories for fare adjustments.
 */
public enum ConcessionType {
    NONE("None / Standard Fare", 0.00),
    PERSON_WITH_DISABILITY("Person with Disability (Divyangjan)", 0.50), // 50% discount on base fare
    RAILWAY_PASS("Railway Pass Concession", 1.00); // 100% concession on base fare (statutory fee ₹40)

    private final String displayName;
    private final double discountRatio;

    ConcessionType(String displayName, double discountRatio) {
        this.displayName = displayName;
        this.discountRatio = discountRatio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDiscountRatio() {
        return discountRatio;
    }

    /**
     * Calculates the adjusted fare after applying concession rules.
     *
     * @param baseFare Original base fare in INR
     * @return Discounted fare, maintaining minimum statutory fee where applicable
     */
    public double applyConcession(double baseFare) {
        if (this == NONE) {
            return baseFare;
        }
        if (this == RAILWAY_PASS) {
            return 40.0; // Flat nominal statutory reservation surcharge
        }
        if (this == PERSON_WITH_DISABILITY) {
            return Math.max(50.0, Math.round(baseFare * (1.0 - discountRatio)));
        }
        return baseFare;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
