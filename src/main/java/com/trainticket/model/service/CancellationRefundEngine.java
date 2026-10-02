package com.trainticket.model.service;

import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Random;

/**
 * Official Indian Railways / IRCTC Compliant Cancellation and Refund Engine.
 * <p>
 * Calculates tiered cancellation charges and net refund amount based on:
 * <ul>
 *   <li>Time delta between cancellation request and scheduled train departure.</li>
 *   <li>Travel class minimum charges (1A, 2A, 3A, SL, CC, etc.).</li>
 *   <li>Passenger count on the booking.</li>
 * </ul>
 * Zero UI dependencies — pure service model.
 */
public final class CancellationRefundEngine {

    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("H:m", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:m a", Locale.ENGLISH);

    private CancellationRefundEngine() {
        // Utility class
    }

    /**
     * Immutable value object holding the cancellation audit breakdown.
     */
    public static record CancellationBreakdown(
            double originalFare,
            double cancellationCharge,
            double refundAmount,
            long hoursToDeparture,
            String ruleDescription,
            boolean isEligibleForRefund,
            String refundReferenceId
    ) {}

    /**
     * Calculates the cancellation charges and refund amount for a confirmed booking.
     *
     * @param booking the booking to evaluate
     * @return structured cancellation breakdown
     */
    public static CancellationBreakdown calculateRefund(Booking booking) {
        if (booking == null) {
            return new CancellationBreakdown(0.0, 0.0, 0.0, 0, "Invalid booking", false, generateRefId());
        }

        double totalFare = booking.getTotalFare();
        int passengerCount = (booking.getPassengers() != null && !booking.getPassengers().isEmpty())
                ? booking.getPassengers().size()
                : 1;

        LocalDateTime departureDateTime = resolveDepartureDateTime(booking.getJourneyDate(), booking.getDepartureTime());
        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(now, departureDateTime);
        long hoursToDeparture = duration.toHours();

        double minClassFeePerPassenger = getMinimumCancellationFeePerPassenger(booking.getClassCode());
        double totalMinFee = minClassFeePerPassenger * passengerCount;

        double cancellationCharge;
        String ruleDesc;
        boolean eligible;

        if (hoursToDeparture < 0) {
            // Train has already departed
            cancellationCharge = totalFare;
            ruleDesc = "Train already departed (Non-refundable)";
            eligible = false;
        } else if (hoursToDeparture < 4) {
            // Less than 4 hours before departure (Chart prepared)
            cancellationCharge = totalFare;
            ruleDesc = "< 4 Hours to Departure: Chart prepared (Non-refundable)";
            eligible = false;
        } else if (hoursToDeparture < 12) {
            // Between 4 and 12 hours: 50% of fare subject to min per class
            double halfFare = totalFare * 0.50;
            cancellationCharge = Math.max(halfFare, totalMinFee);
            cancellationCharge = Math.min(cancellationCharge, totalFare);
            ruleDesc = "4 to 12 Hours to Departure: 50% cancellation fee";
            eligible = true;
        } else if (hoursToDeparture < 48) {
            // Between 12 and 48 hours: 25% of fare subject to min per class
            double quarterFare = totalFare * 0.25;
            cancellationCharge = Math.max(quarterFare, totalMinFee);
            cancellationCharge = Math.min(cancellationCharge, totalFare);
            ruleDesc = "12 to 48 Hours to Departure: 25% cancellation fee";
            eligible = true;
        } else {
            // More than 48 hours: Flat minimum per class
            cancellationCharge = Math.min(totalMinFee, totalFare);
            ruleDesc = "> 48 Hours to Departure: Flat standard fee (₹" + (int) minClassFeePerPassenger + "/passenger)";
            eligible = true;
        }

        double refundAmount = eligible ? Math.max(0.0, totalFare - cancellationCharge) : 0.0;

        return new CancellationBreakdown(
                totalFare,
                Math.round(cancellationCharge * 100.0) / 100.0,
                Math.round(refundAmount * 100.0) / 100.0,
                hoursToDeparture,
                ruleDesc,
                eligible,
                generateRefId()
        );
    }

    /**
     * Resolves local departure date & time safely handling multiple formats.
     */
    public static LocalDateTime resolveDepartureDateTime(LocalDate journeyDate, String departureTimeStr) {
        LocalDate date = journeyDate != null ? journeyDate : LocalDate.now();
        LocalTime time = LocalTime.of(8, 0); // fallback 08:00 AM

        if (departureTimeStr != null && !departureTimeStr.isBlank()) {
            String raw = departureTimeStr.trim().toUpperCase();
            try {
                if (raw.contains("AM") || raw.contains("PM")) {
                    time = LocalTime.parse(raw, TIME_12);
                } else {
                    time = LocalTime.parse(raw, TIME_24);
                }
            } catch (DateTimeParseException ignored) {
                // If it fails, attempt rudimentary extraction
                try {
                    String[] parts = raw.split("[: ]");
                    if (parts.length >= 2) {
                        int h = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        if (raw.contains("PM") && h < 12) h += 12;
                        if (raw.contains("AM") && h == 12) h = 0;
                        time = LocalTime.of(Math.min(Math.max(0, h), 23), Math.min(Math.max(0, m), 59));
                    }
                } catch (Exception ignored2) {
                    time = LocalTime.of(8, 0);
                }
            }
        }
        return LocalDateTime.of(date, time);
    }

    /**
     * Official Indian Railways Minimum Cancellation Charge per passenger.
     */
    public static double getMinimumCancellationFeePerPassenger(String classCode) {
        if (classCode == null) return 120.0;
        String code = classCode.toUpperCase().trim();
        return switch (code) {
            case "1A", "EC" -> 240.0;
            case "2A" -> 200.0;
            case "3A", "3E", "CC" -> 180.0;
            case "SL" -> 120.0;
            default -> 60.0; // 2S, General unreserved, etc.
        };
    }

    private static String generateRefId() {
        return "RF-REF-" + (100000 + new Random().nextInt(900000));
    }
}
