package com.trainticket.model;

/**
 * Categorization of passenger trains in the Indian Railways network.
 */
public enum TrainType {
    VANDE_BHARAT("Vande Bharat Express", "VB"),
    RAJDHANI("Tejas Rajdhani Express", "RAJ"),
    SHATABDI("Shatabdi Express", "SHT"),
    SUPERFAST("Superfast Express", "SF"),
    EXPRESS("Mail / Express", "EXP");

    private final String displayName;
    private final String shortBadge;

    TrainType(String displayName, String shortBadge) {
        this.displayName = displayName;
        this.shortBadge = shortBadge;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortBadge() {
        return shortBadge;
    }
}
