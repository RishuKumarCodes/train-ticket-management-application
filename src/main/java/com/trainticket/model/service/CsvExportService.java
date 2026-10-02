package com.trainticket.model.service;

import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.model.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing RFC 4180-compliant CSV export for booking manifests and train fleet rosters.
 */
public class CsvExportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvExportService.class);
    private static final CsvExportService INSTANCE = new CsvExportService();

    public static CsvExportService getInstance() {
        return INSTANCE;
    }

    public boolean exportBookingsToCsv(File targetFile, List<Booking> bookings) {
        if (targetFile == null || bookings == null) return false;

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {

            // Header
            writer.write("PNR,Train Number,Train Name,Journey Date,From Station,To Station,Class,Quota,Total Fare,Status,Created At,Passengers\n");

            for (Booking b : bookings) {
                String passengersSummary = b.getPassengers().stream()
                        .map(p -> p.getPassengerName() + " (" + p.getAge() + p.getGender() + ", " + p.getCoachNumber() + "-" + p.getSeatNumber() + ")")
                        .collect(Collectors.joining("; "));

                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s\n",
                        escapeCsv(b.getPnr()),
                        escapeCsv(b.getTrainNumber()),
                        escapeCsv(b.getTrainName()),
                        b.getJourneyDate(),
                        escapeCsv(b.getFromStationCode()),
                        escapeCsv(b.getToStationCode()),
                        escapeCsv(b.getClassCode()),
                        escapeCsv(b.getQuotaCode()),
                        b.getTotalFare(),
                        escapeCsv(b.getStatus()),
                        b.getCreatedAt() != null ? b.getCreatedAt().toString() : "",
                        escapeCsv(passengersSummary)
                ));
            }
            logger.info("Successfully exported {} bookings to CSV: {}", bookings.size(), targetFile.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            logger.error("Failed to export bookings to CSV: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public boolean exportFleetToCsv(File targetFile, List<Train> trains) {
        if (targetFile == null || trains == null) return false;

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {

            writer.write("Train Number,Train Name,Type,Source,Destination,Running Days,Status,Delay Minutes,Total Halts,Classes\n");

            for (Train t : trains) {
                String classes = t.getCoachClasses().stream()
                        .map(c -> c.getClassCode() + "(Avail:" + c.getAvailableSeats() + ")")
                        .collect(Collectors.joining("; "));

                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%d,%d,%s\n",
                        escapeCsv(t.getTrainNumber()),
                        escapeCsv(t.getName()),
                        t.getType() != null ? escapeCsv(t.getType().name()) : "EXPRESS",
                        t.getSourceStation() != null ? escapeCsv(t.getSourceStation().getCode()) : "",
                        t.getDestStation() != null ? escapeCsv(t.getDestStation().getCode()) : "",
                        escapeCsv(t.getFormattedRunningDays()),
                        t.getStatus() != null ? escapeCsv(t.getStatus().name()) : "ON_TIME",
                        t.getDelayMinutes(),
                        t.getRouteHalts().size(),
                        escapeCsv(classes)
                ));
            }
            logger.info("Successfully exported {} trains to CSV: {}", trains.size(), targetFile.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            logger.error("Failed to export fleet to CSV: {}", ex.getMessage(), ex);
            return false;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
