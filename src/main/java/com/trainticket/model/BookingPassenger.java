package com.trainticket.model;

import java.io.Serializable;

/**
 * Domain entity representing an individual passenger on a booking.
 * Pure model with zero UI dependencies.
 */
public class BookingPassenger implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String passengerName;
    private final int age;
    private final String gender;
    private final String berthPreference;
    private final String coachNumber;
    private final int seatNumber;
    private final String status; // CONFIRMED, CANCELLED

    public BookingPassenger(Long id, String passengerName, int age, String gender,
                            String berthPreference, String coachNumber, int seatNumber, String status) {
        this.id = id;
        this.passengerName = passengerName != null ? passengerName.trim() : "Passenger";
        this.age = age;
        this.gender = gender != null ? gender.trim() : "M";
        this.berthPreference = berthPreference != null ? berthPreference.trim() : "LOWER";
        this.coachNumber = coachNumber != null ? coachNumber.trim() : "B1";
        this.seatNumber = seatNumber;
        this.status = status != null ? status.trim().toUpperCase() : "CONFIRMED";
    }

    public static BookingPassenger create(String passengerName, int age, String gender,
                                         String berthPreference, String coachNumber, int seatNumber) {
        return new BookingPassenger(null, passengerName, age, gender, berthPreference, coachNumber, seatNumber, "CONFIRMED");
    }

    public Long getId() {
        return id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getBerthPreference() {
        return berthPreference;
    }

    public String getCoachNumber() {
        return coachNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return passengerName + " (" + age + ", " + gender + ") • Coach " + coachNumber + ", Seat " + seatNumber;
    }
}
