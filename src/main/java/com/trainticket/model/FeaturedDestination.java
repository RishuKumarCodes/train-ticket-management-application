package com.trainticket.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain entity representing an editorial featured travel destination.
 * Maps scenic photography and monument metadata to an associated IRCTC railway station.
 */
public class FeaturedDestination implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String monumentName;
    private final String locationText;
    private final String stationCode;
    private final String imagePath;
    private final String layoutStyle;

    public FeaturedDestination(Long id, String monumentName, String locationText,
                               String stationCode, String imagePath, String layoutStyle) {
        this.id = id;
        this.monumentName = Objects.requireNonNull(monumentName, "Monument name cannot be null").trim();
        this.locationText = Objects.requireNonNull(locationText, "Location text cannot be null").trim();
        this.stationCode = Objects.requireNonNull(stationCode, "Station code cannot be null").toUpperCase().trim();
        this.imagePath = Objects.requireNonNull(imagePath, "Image path cannot be null").trim();
        this.layoutStyle = layoutStyle != null ? layoutStyle.trim() : "";
    }

    public Long getId() {
        return id;
    }

    public String getMonumentName() {
        return monumentName;
    }

    public String getLocationText() {
        return locationText;
    }

    public String getStationCode() {
        return stationCode;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getLayoutStyle() {
        return layoutStyle;
    }

    /**
     * Returns the monument name formatted on a single line (replacing internal line breaks).
     */
    public String getSingleLineName() {
        return monumentName.replace("\n", " ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeaturedDestination that = (FeaturedDestination) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(monumentName, that.monumentName) && Objects.equals(stationCode, that.stationCode));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, monumentName, stationCode);
    }

    @Override
    public String toString() {
        return "FeaturedDestination{" +
                "id=" + id +
                ", monumentName='" + getSingleLineName() + '\'' +
                ", locationText='" + locationText + '\'' +
                ", stationCode='" + stationCode + '\'' +
                '}';
    }
}
