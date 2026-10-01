package com.trainticket.model;

import java.util.Objects;

/**
 * Immutable domain entity representing a railway station node.
 */
public final class Station {

    private final long id;
    private final String code;
    private final String name;
    private final String city;
    private final String state;

    public Station(long id, String code, String name, String city, String state) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "Station code cannot be null").toUpperCase().trim();
        this.name = Objects.requireNonNull(name, "Station name cannot be null").trim();
        this.city = Objects.requireNonNull(city, "Station city cannot be null").trim();
        this.state = Objects.requireNonNull(state, "Station state cannot be null").trim();
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getDisplayName() {
        return name + " (" + code + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station other)) return false;
        return code.equalsIgnoreCase(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code.toUpperCase());
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
