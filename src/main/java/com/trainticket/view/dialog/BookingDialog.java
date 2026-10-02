package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.User;
import com.trainticket.model.service.BookingService;
import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.trainticket.view.component.selector.ModernSmoothDropdown;
import java.util.function.Consumer;

/**
 * IRCTC-style booking dialog: up to 6 passengers, each with Name/Age/Gender/Berth
 * Preference. Includes contact section and itemised fare summary.
 */
public class BookingDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;
    private static final int MAX_PASSENGERS = 6;

    private static final Color BRAND   = new Color(250, 89, 9);
    private static final Color SLATE   = new Color(15, 23, 42);
    private static final Color MUTED   = new Color(100, 116, 139);
    private static final Color SURFACE = new Color(238, 242, 246);
    private static final Color BORDER_C = new Color(226, 232, 240);

    private final TrainSearchResult  searchResult;
    private final CoachAvailability  coachAvailability;
    private final TrainSearchQuery   searchQuery;

    private final List<PassengerRow> passengerRows = new ArrayList<>();
    private JPanel passengerListPanel;
    private JTextField mobileField;
    private JTextField emailField;
    private JLabel feedbackLabel;
    private JLabel totalFareLabel;
    private JPanel cardContainer;
    private CardLayout cardLayout;

    public BookingDialog(Window owner, TrainSearchResult searchResult,
                         CoachAvailability coachAvailability, TrainSearchQuery searchQuery) {
        super(owner, "RailFlow \u2022 Book Ticket", 740, 680);
        this.searchResult     = searchResult;
        this.coachAvailability = coachAvailability;
        this.searchQuery      = searchQuery;
        setHeaderTitle("BOOK TICKET");
        buildContent();
    }

    private void buildContent() {
        cardLayout    = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);
        cardContainer.add(buildFormCard(), "FORM");
        getContentCard().add(cardContainer, BorderLayout.CENTER);
    }

    // ── Form card ──────────────────────────────────────────────────────────────

    private JPanel buildFormCard() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);
        root.add(buildJourneySummary(), BorderLayout.NORTH);
        root.add(buildScrollableBody(), BorderLayout.CENTER);
        root.add(buildSouthStrip(),     BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildJourneySummary() {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel trainLbl = new JLabel(searchResult.getTrain().getTrainNumber()
                + "  " + searchResult.getTrain().getName());
        trainLbl.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        trainLbl.setForeground(SLATE);

        String date = (searchQuery != null && searchQuery.getJourneyDate() != null)
                ? searchQuery.getJourneyDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
                : "Scheduled";

        JLabel routeLbl = new JLabel(
                searchResult.getOriginHalt().getStation().getCode() + " \u2192 "
                + searchResult.getDestinationHalt().getStation().getCode()
                + "   \u2022   " + searchResult.getDepartureTime()
                + " \u2192 " + searchResult.getArrivalTime()
                + " " + searchResult.getDayOffsetLabel()
                + "   \u2022   " + date);
        routeLbl.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        routeLbl.setForeground(MUTED);

        left.add(trainLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(routeLbl);
        card.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JLabel classLbl = new JLabel(coachAvailability.getClassCode()
                + " \u2022 " + coachAvailability.getQuotaCode(), SwingConstants.RIGHT);
        classLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        classLbl.setForeground(BRAND);

        JLabel statusLbl = new JLabel(coachAvailability.getStatusBadgeText(), SwingConstants.RIGHT);
        statusLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        statusLbl.setForeground(new Color(16, 185, 129));

        right.add(classLbl);
        right.add(Box.createVerticalStrut(2));
        right.add(statusLbl);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JScrollPane buildScrollableBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // ── Passengers ────────────────────────────────────────────────────────
        body.add(sectionHeader("PASSENGER(S)", "Up to 6 per booking"));
        body.add(Box.createVerticalStrut(6));
        body.add(buildColumnHeaders());
        body.add(Box.createVerticalStrut(4));

        passengerListPanel = new JPanel();
        passengerListPanel.setLayout(new BoxLayout(passengerListPanel, BoxLayout.Y_AXIS));
        passengerListPanel.setOpaque(false);
        body.add(passengerListPanel);

        User u = AuthSession.getInstance().getCurrentUser();
        addPassengerRow(u != null ? u.getFullName() : "");

        body.add(Box.createVerticalStrut(8));

        JButton addBtn = buildAddPassengerBtn();
        JButton savedBtn = buildSavedTravelersBtn();
        JButton seatMapBtn = buildSeatMapBtn();

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(addBtn);
        leftBtns.add(savedBtn);

        JPanel addRow = new JPanel(new BorderLayout());
        addRow.setOpaque(false);
        addRow.add(leftBtns, BorderLayout.WEST);
        addRow.add(seatMapBtn, BorderLayout.EAST);
        body.add(addRow);
        body.add(Box.createVerticalStrut(16));

        // ── Contact ───────────────────────────────────────────────────────────
        body.add(sectionHeader("CONTACT DETAILS", "PNR sent via SMS / WhatsApp / Email"));
        body.add(Box.createVerticalStrut(8));

        JPanel contactRow = new JPanel(new GridLayout(1, 2, 12, 0));
        contactRow.setOpaque(false);
        mobileField = styledField("10-digit mobile number");
        emailField  = styledField("Optional — for email copy");
        contactRow.add(labeledField("MOBILE NUMBER *", mobileField));
        contactRow.add(labeledField("EMAIL ADDRESS", emailField));
        body.add(contactRow);
        body.add(Box.createVerticalStrut(12));

        // ── Fare summary ──────────────────────────────────────────────────────
        body.add(buildFareSummary());
        body.add(Box.createVerticalStrut(4));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel sectionHeader(String title, String sub) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel t = new JLabel(title);
        t.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        t.setForeground(SLATE);

        JLabel s = new JLabel(sub);
        s.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        s.setForeground(MUTED);

        p.add(t, BorderLayout.WEST);
        p.add(s, BorderLayout.EAST);
        return p;
    }

    private JPanel buildColumnHeaders() {
        JPanel row = new JPanel(new GridLayout(1, 6, 6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        for (String h : new String[]{"#", "FULL NAME", "AGE", "GENDER", "BERTH PREF.", ""}) {
            JLabel l = new JLabel(h);
            l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
            l.setForeground(MUTED);
            row.add(l);
        }
        return row;
    }

    private void addPassengerRow(String name) {
        if (passengerRows.size() >= MAX_PASSENGERS) return;
        int idx = passengerRows.size() + 1;
        PassengerRow pr = new PassengerRow(idx, name, this::removePassengerRow);
        passengerRows.add(pr);
        passengerListPanel.add(pr);
        passengerListPanel.add(Box.createVerticalStrut(5));
        passengerListPanel.revalidate();
        passengerListPanel.repaint();
        updateFare();
    }

    private void removePassengerRow(PassengerRow pr) {
        if (passengerRows.size() <= 1) return;
        passengerRows.remove(pr);
        for (int i = 0; i < passengerRows.size(); i++) passengerRows.get(i).setIndex(i + 1);
        passengerListPanel.removeAll();
        for (PassengerRow r : passengerRows) {
            passengerListPanel.add(r);
            passengerListPanel.add(Box.createVerticalStrut(5));
        }
        passengerListPanel.revalidate();
        passengerListPanel.repaint();
        updateFare();
    }

    private JButton buildAddPassengerBtn() {
        JButton btn = new JButton("+ Add Passenger");
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setForeground(BRAND);
        btn.setBackground(new Color(255, 247, 237));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(250, 89, 9, 90), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> addPassengerRow(""));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        return btn;
    }

    private JButton buildSavedTravelersBtn() {
        JButton btn = new JButton("SAVED TRAVELERS \u25BE");
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        btn.setForeground(new Color(2, 132, 199));
        btn.setBackground(new Color(239, 246, 255));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(2, 132, 199, 120), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        btn.addActionListener(e -> {
            User u = AuthSession.getInstance().getCurrentUser();
            Long userId = (u != null) ? u.getId() : null;
            List<com.trainticket.model.PassengerMasterRecord> saved = new com.trainticket.model.dao.PassengerMasterDAO().getPassengersForUser(userId);

            JPopupMenu menu = new JPopupMenu();
            menu.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
            for (com.trainticket.model.PassengerMasterRecord r : saved) {
                JMenuItem item = new JMenuItem(r.getFullName() + " (" + r.getAge() + " " + r.getGender() + ") \u2022 " + r.getBerthPreference());
                item.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
                item.addActionListener(ev -> populateSavedPassenger(r));
                menu.add(item);
            }
            menu.show(btn, 0, btn.getHeight() + 4);
        });
        return btn;
    }

    private void populateSavedPassenger(com.trainticket.model.PassengerMasterRecord r) {
        if (passengerRows.isEmpty()) {
            addPassengerRow(r.getFullName());
            passengerRows.get(0).setPassengerData(r.getFullName(), r.getAge(), r.getGender(), r.getBerthPreference());
        } else {
            PassengerRow first = passengerRows.get(0);
            if (first.isNameEmpty()) {
                first.setPassengerData(r.getFullName(), r.getAge(), r.getGender(), r.getBerthPreference());
            } else if (passengerRows.size() < MAX_PASSENGERS) {
                addPassengerRow(r.getFullName());
                passengerRows.get(passengerRows.size() - 1).setPassengerData(r.getFullName(), r.getAge(), r.getGender(), r.getBerthPreference());
            } else {
                first.setPassengerData(r.getFullName(), r.getAge(), r.getGender(), r.getBerthPreference());
            }
        }
        feedbackLabel.setText("Filled passenger: " + r.getFullName());
        feedbackLabel.setForeground(new Color(16, 185, 129));
    }

    private JButton buildSeatMapBtn() {
        JButton btn = new JButton("CHOOSE SEATS ON COACH MAP \u2197");
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        btn.setForeground(BRAND);
        btn.setBackground(new Color(255, 247, 237));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(250, 89, 9, 120), 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        btn.addActionListener(e -> openCoachSeatMap());
        return btn;
    }

    private void openCoachSeatMap() {
        List<CoachSeatSelectionDialog.SelectedSeat> initialSeats = new ArrayList<>();
        for (PassengerRow pr : passengerRows) {
            if (pr.hasAssignedSeat()) {
                initialSeats.add(new CoachSeatSelectionDialog.SelectedSeat(
                        pr.getAssignedCoach(), pr.getAssignedSeat(), pr.getAssignedBerthType()));
            }
        }
        CoachSeatSelectionDialog dialog = new CoachSeatSelectionDialog(
                this, coachAvailability.getClassCode(), passengerRows.size(), initialSeats,
                chosenSeats -> {
                    for (int i = 0; i < chosenSeats.size() && i < passengerRows.size(); i++) {
                        CoachSeatSelectionDialog.SelectedSeat cs = chosenSeats.get(i);
                        passengerRows.get(i).setAssignedSeat(cs.coachNumber(), cs.seatNumber(), cs.berthType());
                    }
                    feedbackLabel.setText("Seats selected on coach map: " + chosenSeats.size() + " assigned.");
                    feedbackLabel.setForeground(new Color(16, 185, 129));
                }
        );
        dialog.setVisible(true);
    }

    private JPanel buildFareSummary() {
        JPanel card = new JPanel(new GridLayout(0, 2, 0, 5)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        double base = coachAvailability.getFinalFare();
        fareRow(card, "Base Fare (per passenger)", String.format("\u20B9%,.0f", base));
        fareRow(card, "Reservation Charge",        "\u20B940");
        fareRow(card, "GST (5%)",                  String.format("\u20B9%,.0f", base * 0.05));

        // spacer
        card.add(new JPanel() {{ setOpaque(false); }});
        card.add(new JPanel() {{ setOpaque(false); }});

        JLabel totalKey = new JLabel("TOTAL PAYABLE (1 pax)");
        totalKey.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        totalKey.setForeground(SLATE);

        totalFareLabel = new JLabel(String.format("\u20B9%,.0f", base * 1.05 + 40), SwingConstants.RIGHT);
        totalFareLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        totalFareLabel.setForeground(BRAND);

        card.add(totalKey);
        card.add(totalFareLabel);
        return card;
    }

    private void fareRow(JPanel p, String label, String value) {
        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        l.setForeground(MUTED);

        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        v.setForeground(SLATE);

        p.add(l); p.add(v);
    }

    private void updateFare() {
        if (totalFareLabel == null) return;
        int n = passengerRows.size();
        double total = coachAvailability.getFinalFare() * 1.05 * n + 40;
        totalFareLabel.setText(String.format("\u20B9%,.0f", total));
        Container parent = totalFareLabel.getParent();
        if (parent != null) {
            for (Component c : parent.getComponents()) {
                if (c instanceof JLabel lbl && lbl.getText().startsWith("TOTAL PAYABLE")) {
                    lbl.setText("TOTAL PAYABLE (" + n + " pax)");
                }
            }
        }
    }

    private JPanel buildSouthStrip() {
        JPanel strip = new JPanel(new BorderLayout(12, 0));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_C),
                new EmptyBorder(10, 0, 0, 0)));

        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        feedbackLabel.setForeground(new Color(239, 68, 68));
        strip.add(feedbackLabel, BorderLayout.CENTER);

        JButton confirm = pillButton("PROCEED TO PAYMENT", BRAND, Color.WHITE);
        confirm.setPreferredSize(new Dimension(220, 46));
        confirm.addActionListener(e -> handleConfirm());
        strip.add(confirm, BorderLayout.EAST);
        return strip;
    }

    // ── Confirm & Submit via Payment Gateway ──────────────────────────────────

    private void handleConfirm() {
        feedbackLabel.setText(" ");
        List<BookingPassenger> passengers = new ArrayList<>();

        for (int i = 0; i < passengerRows.size(); i++) {
            PassengerRow pr = passengerRows.get(i);
            String name = pr.getPassengerName().trim();
            String ageStr = pr.getAge().trim();
            int paxNum = i + 1;

            if (name.isBlank()) { feedbackLabel.setText("Passenger " + paxNum + ": name required."); return; }
            if (ageStr.isBlank()) { feedbackLabel.setText("Passenger " + paxNum + ": age required."); return; }

            int age;
            try {
                age = Integer.parseInt(ageStr);
                if (age < 1 || age > 120) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                feedbackLabel.setText("Passenger " + paxNum + ": enter a valid age (1\u2013120).");
                return;
            }

            String coach = pr.hasAssignedSeat() ? pr.getAssignedCoach() : "B" + (1 + new SecureRandom().nextInt(4));
            int seat = pr.hasAssignedSeat() ? pr.getAssignedSeat() : (1 + new SecureRandom().nextInt(64));
            String berth = pr.hasAssignedSeat() ? pr.getAssignedBerthType() : pr.getBerth();

            passengers.add(BookingPassenger.create(name, age, pr.getGender(), berth, coach, seat));
        }

        if (mobileField.getText().trim().isBlank()) {
            feedbackLabel.setText("Mobile number is required for ticket delivery.");
            return;
        }

        double fare = coachAvailability.getFinalFare() * 1.05 * passengers.size() + 40;
        String trainSummary = searchResult.getTrain().getTrainNumber() + " " + searchResult.getTrain().getName()
                + " (" + searchResult.getOriginHalt().getStation().getCode() + " \u2192 "
                + searchResult.getDestinationHalt().getStation().getCode() + ")";

        // Launch Mock Payment Gateway Modal
        PaymentGatewayDialog paymentDialog = new PaymentGatewayDialog(
                this, fare, trainSummary, paymentResult -> {
            completeBooking(passengers, fare, paymentResult);
        });
        paymentDialog.setVisible(true);
    }

    private void completeBooking(List<BookingPassenger> passengers, double fare, PaymentGatewayDialog.PaymentResult paymentResult) {
        String pnr  = generatePnr();
        User user   = AuthSession.getInstance().getCurrentUser();
        Long userId = user != null ? user.getId() : null;
        String contact = mobileField.getText().trim();

        LocalDate journeyDate = (searchQuery != null && searchQuery.getJourneyDate() != null)
                ? searchQuery.getJourneyDate() : LocalDate.now().plusDays(1);

        Booking booking = new Booking(null, pnr, userId, contact,
                searchResult.getTrain().getId(),
                searchResult.getTrain().getTrainNumber(),
                searchResult.getTrain().getName(),
                journeyDate,
                searchResult.getOriginHalt().getStation().getCode(),
                searchResult.getOriginHalt().getStation().getName(),
                searchResult.getDestinationHalt().getStation().getCode(),
                searchResult.getDestinationHalt().getStation().getName(),
                searchResult.getDepartureTime().toString(),
                searchResult.getArrivalTime().toString(),
                coachAvailability.getClassCode(),
                coachAvailability.getQuotaCode(),
                fare, "CONFIRMED", LocalDateTime.now(), passengers);

        BookingService.getInstance().createBooking(booking);

        // Auto-save new co-travelers to Master List if authenticated
        User currentUser = AuthSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            com.trainticket.model.dao.PassengerMasterDAO pmDao = new com.trainticket.model.dao.PassengerMasterDAO();
            List<com.trainticket.model.PassengerMasterRecord> existing = pmDao.getPassengersForUser(currentUser.getId());
            for (BookingPassenger bp : passengers) {
                boolean alreadySaved = existing.stream()
                        .anyMatch(e -> e.getFullName().equalsIgnoreCase(bp.getPassengerName().trim()));
                if (!alreadySaved && !bp.getPassengerName().trim().isBlank()) {
                    String gCode = "Female".equalsIgnoreCase(bp.getGender()) ? "F" : "M";
                    pmDao.savePassenger(currentUser.getId(), new com.trainticket.model.PassengerMasterRecord(
                            null, currentUser.getId(), bp.getPassengerName().trim(), bp.getAge(), gCode, bp.getBerthPreference()
                    ));
                }
            }
        }

        setHeaderTitle("TICKET CONFIRMED");
        JPanel successCard = buildSuccessCard(pnr, passengers, fare, paymentResult);
        cardContainer.add(successCard, "SUCCESS");
        cardLayout.show(cardContainer, "SUCCESS");
    }

    // ── Success card ───────────────────────────────────────────────────────────

    private JPanel buildSuccessCard(String pnr, List<BookingPassenger> passengers, double fare,
                                    PaymentGatewayDialog.PaymentResult paymentResult) {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setOpaque(false);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel ok = new JLabel("\u25CF  BOOKING CONFIRMED & TICKET ISSUED", SwingConstants.CENTER);
        ok.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        ok.setForeground(new Color(16, 185, 129));
        ok.setAlignmentX(0.5f);
        center.add(ok);
        center.add(Box.createVerticalStrut(6));

        if (paymentResult != null) {
            JLabel txnBadge = new JLabel("PAID VIA " + paymentResult.paymentMethod().toUpperCase()
                    + " \u2022 TXN: " + paymentResult.transactionId(), SwingConstants.CENTER);
            txnBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
            txnBadge.setForeground(MUTED);
            txnBadge.setAlignmentX(0.5f);
            center.add(txnBadge);
        }
        center.add(Box.createVerticalStrut(10));

        JLabel pnrTitle = new JLabel("PNR NUMBER", SwingConstants.CENTER);
        pnrTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        pnrTitle.setForeground(MUTED);
        pnrTitle.setAlignmentX(0.5f);
        center.add(pnrTitle);

        JLabel pnrVal = new JLabel(pnr, SwingConstants.CENTER);
        pnrVal.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 44f));
        pnrVal.setForeground(BRAND);
        pnrVal.setAlignmentX(0.5f);
        center.add(pnrVal);
        center.add(Box.createVerticalStrut(12));

        // Passenger table with 4 columns
        JPanel paxTable = new JPanel(new GridLayout(0, 4, 8, 5));
        paxTable.setOpaque(false);
        paxTable.setBorder(new EmptyBorder(10, 12, 10, 12));
        for (String h : new String[]{"PASSENGER", "AGE/GENDER", "COACH & SEAT", "BERTH"}) {
            JLabel l = new JLabel(h);
            l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
            l.setForeground(MUTED);
            paxTable.add(l);
        }
        for (BookingPassenger p : passengers) {
            detailLabel(paxTable, p.getPassengerName());
            detailLabel(paxTable, p.getAge() + " / " + p.getGender());
            detailLabel(paxTable, "Coach " + p.getCoachNumber() + ", Seat " + p.getSeatNumber());
            detailLabel(paxTable, p.getBerthPreference());
        }
        center.add(paxTable);
        center.add(Box.createVerticalStrut(10));

        // Journey summary
        JPanel journeyGrid = new JPanel(new GridLayout(2, 3, 8, 5));
        journeyGrid.setOpaque(false);
        detailPair(journeyGrid, "TRAIN",    searchResult.getTrain().getTrainNumber());
        detailPair(journeyGrid, "CLASS",    coachAvailability.getClassCode());
        detailPair(journeyGrid, "FARE PAID", String.format("\u20B9%,.0f", fare));
        detailPair(journeyGrid, "FROM",
                searchResult.getOriginHalt().getStation().getCode() + " " + searchResult.getDepartureTime());
        detailPair(journeyGrid, "TO",
                searchResult.getDestinationHalt().getStation().getCode() + " " + searchResult.getArrivalTime());
        detailPair(journeyGrid, "STATUS", "CONFIRMED");
        center.add(journeyGrid);

        card.add(center, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);

        JButton viewPassBtn = pillButton("VIEW & PRINT E-TICKET \u2197", new Color(2, 132, 199), Color.WHITE);
        viewPassBtn.setPreferredSize(new Dimension(200, 42));
        viewPassBtn.addActionListener(e -> {
            Booking b = BookingService.getInstance().getBookingByPnr(pnr);
            if (b != null) {
                new ETicketPassDialog(BookingDialog.this, b).setVisible(true);
            }
        });
        btnRow.add(viewPassBtn);

        JButton checkPnrBtn = pillButton("CHECK PNR STATUS \u2197", BRAND, Color.WHITE);
        checkPnrBtn.setPreferredSize(new Dimension(190, 42));
        checkPnrBtn.addActionListener(e -> {
            new PnrStatusDialog(BookingDialog.this, pnr).setVisible(true);
        });
        btnRow.add(checkPnrBtn);

        JButton done = pillButton("CLOSE", SLATE, Color.WHITE);
        done.setPreferredSize(new Dimension(140, 42));
        done.addActionListener(e -> dispose());
        btnRow.add(done);

        card.add(btnRow, BorderLayout.SOUTH);

        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void detailLabel(JPanel p, String value) {
        JLabel v = new JLabel(value);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        v.setForeground(SLATE);
        p.add(v);
    }

    private void detailPair(JPanel parent, String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
        l.setForeground(MUTED);

        JLabel v = new JLabel(value);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        v.setForeground(SLATE);

        p.add(l); p.add(v);
        parent.add(p);
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(100, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        f.setForeground(SLATE);
        f.setCaretColor(BRAND);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; " +
                "borderColor: #CBD5E1; focusedBorderColor: #FA5909; " +
                "focusedBackground: #FFFFFF; margin: 4,14,4,14;");
        return f;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
        l.setForeground(MUTED);

        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(field);
        return p;
    }

    private JButton pillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = bg;
                if (getModel().isPressed())   c = bg.darker().darker();
                else if (getModel().isRollover()) c = bg.darker();
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(fg);
                g2.setFont(getFont());
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String generatePnr() {
        SecureRandom r = new SecureRandom();
        return (100 + r.nextInt(900)) + "-" + (1000000 + r.nextInt(9000000));
    }

    // ── PassengerRow ───────────────────────────────────────────────────────────

    private class PassengerRow extends JPanel {

        private static final long serialVersionUID = 1L;

        private JLabel indexLbl;
        private final JTextField nameField;
        private final JTextField ageField;
        private final ModernSmoothDropdown<String> genderDropdown;
        private final ModernSmoothDropdown<String> berthDropdown;
        private String assignedCoach;
        private int assignedSeat = 0;
        private String assignedBerthType;

        PassengerRow(int index, String defaultName, Consumer<PassengerRow> onRemove) {
            setLayout(new GridLayout(1, 6, 6, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(600, 42));

            // # index
            indexLbl = new JLabel(String.valueOf(index), SwingConstants.CENTER);
            indexLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            indexLbl.setForeground(MUTED);
            add(indexLbl);

            // Name
            nameField = styledField("Full name");
            nameField.setText(defaultName);
            add(nameField);

            // Age
            ageField = styledField("Age");
            add(ageField);

            // Gender — custom animated dropdown
            java.util.List<String> genders = java.util.List.of("Male", "Female", "Transgender");
            genderDropdown = new ModernSmoothDropdown<>(genders, genders.get(0));
            add(genderDropdown);

            // Berth preference — custom animated dropdown
            java.util.List<String> berths = java.util.List.of(
                    "No Pref.", "Lower", "Middle", "Upper", "Side Lower", "Side Upper");
            berthDropdown = new ModernSmoothDropdown<>(berths, berths.get(0));
            add(berthDropdown);

            // Remove button
            JButton rm = new JButton("\u2715");
            rm.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            rm.setForeground(new Color(148, 163, 184));
            rm.setBorderPainted(false);
            rm.setContentAreaFilled(false);
            rm.setFocusPainted(false);
            rm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            rm.setToolTipText("Remove passenger");
            rm.setVisible(index > 1);
            rm.addActionListener(e -> onRemove.accept(this));
            add(rm);
        }

        void setIndex(int i) {
            if (hasAssignedSeat()) {
                indexLbl.setText(i + " (" + assignedCoach + "-" + assignedSeat + ")");
            } else {
                indexLbl.setText(String.valueOf(i));
            }
            getComponent(5).setVisible(i > 1);
        }

        void setAssignedSeat(String coach, int seat, String berthType) {
            this.assignedCoach = coach;
            this.assignedSeat = seat;
            this.assignedBerthType = berthType;
            indexLbl.setText(indexLbl.getText().split(" ")[0] + " (" + coach + "-" + seat + ")");
            indexLbl.setToolTipText("Seat: Coach " + coach + ", Seat " + seat + " (" + berthType + ")");
            indexLbl.setForeground(BRAND);
            if (berthType != null) {
                for (String b : java.util.List.of("Lower", "Middle", "Upper", "Side Lower", "Side Upper")) {
                    if (berthType.toUpperCase().contains(b.toUpperCase())) {
                        berthDropdown.setSelectedItem(b);
                        break;
                    }
                }
            }
            repaint();
        }

        boolean hasAssignedSeat() {
            return assignedSeat > 0;
        }

        String getAssignedCoach() { return assignedCoach; }
        int getAssignedSeat() { return assignedSeat; }
        String getAssignedBerthType() { return assignedBerthType; }

        public String getPassengerName() { return nameField.getText(); }
        String getAge()   { return ageField.getText(); }
        String getGender(){ return genderDropdown.getSelectedItem(); }
        String getBerth() { return berthDropdown.getSelectedItem(); }

        boolean isNameEmpty() {
            return nameField.getText().trim().isEmpty();
        }

        void setPassengerData(String name, int age, String gender, String berth) {
            if (name != null) nameField.setText(name);
            if (age > 0) ageField.setText(String.valueOf(age));
            if (gender != null) {
                if ("F".equalsIgnoreCase(gender) || "Female".equalsIgnoreCase(gender)) {
                    genderDropdown.setSelectedItem("Female");
                } else if ("T".equalsIgnoreCase(gender) || "Transgender".equalsIgnoreCase(gender)) {
                    genderDropdown.setSelectedItem("Transgender");
                } else {
                    genderDropdown.setSelectedItem("Male");
                }
            }
            if (berth != null) {
                for (String b : java.util.List.of("Lower", "Middle", "Upper", "Side Lower", "Side Upper")) {
                    if (berth.toUpperCase().contains(b.toUpperCase())) {
                        berthDropdown.setSelectedItem(b);
                        break;
                    }
                }
            }
            repaint();
        }
    }
}
