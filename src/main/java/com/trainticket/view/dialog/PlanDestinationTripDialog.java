package com.trainticket.view.dialog;

import com.trainticket.model.ConcessionType;
import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.Station;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TravelQuota;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.selector.ModernDatePicker;
import com.trainticket.view.component.selector.ModernSmoothDropdown;
import com.trainticket.view.component.selector.StationAutocompleteDropdown;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Minimal, polished modal dialog for planning a journey from a Featured Destination card.
 *
 * <p>Design principles:
 * <ul>
 *   <li>No borders — depth via shadow and clean white surfaces only.</li>
 *   <li>Custom dropdowns via {@link ModernSmoothDropdown} (no JComboBox).</li>
 *   <li>Station origin via {@link StationAutocompleteDropdown} with live filtering.</li>
 *   <li>Route-status alert shown <em>only</em> when there is a problem (hidden on happy path).</li>
 *   <li>Simplified header: "Trip to [Place]" with a light subtitle.</li>
 * </ul>
 */
public class PlanDestinationTripDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    // ── Colours ─────────────────────────────────────────────────────────────
    private static final Color CANVAS       = new Color(248, 250, 252); // #F8FAFC
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);    // #0F172A
    private static final Color TEXT_MUTED   = new Color(100, 116, 139); // #64748B
    private static final Color BRAND        = new Color(250, 89, 9);    // #FA5909
    private static final Color BRAND_HOVER  = new Color(224, 77, 5);    // #E04D05
    private static final Color DANGER_BG    = new Color(254, 242, 242); // #FEF2F2
    private static final Color DANGER_TXT   = new Color(220, 38, 38);   // #DC2626
    private static final Color WARN_BG      = new Color(254, 243, 199); // #FEF3C7
    private static final Color WARN_TXT     = new Color(180, 83, 9);    // #B45309
    private static final Color BTN_MUTED_BG  = new Color(241, 245, 249);
    private static final Color BTN_MUTED_TXT = new Color(71, 85, 105);

    // ── Data ─────────────────────────────────────────────────────────────────
    private final FeaturedDestination destination;
    private final Consumer<TrainSearchQuery> onSearchRequested;
    private final StationDAO stationDAO = new StationDAO();
    private final TrainDAO trainDAO = TrainDAO.getInstance();

    // ── Custom Selectors ─────────────────────────────────────────────────────
    private JTextField originField;          // Pill text field — autocomplete attached
    private StationAutocompleteDropdown stationDropdown;
    private Station selectedOrigin;

    private ModernDatePicker datePicker;

    private ModernSmoothDropdown<TravelQuota> quotaDropdown;
    private ModernSmoothDropdown<String>      classDropdown;

    // ── Status banner ─────────────────────────────────────────────────────────
    private JPanel statusBanner;
    private JLabel statusLabel;

    // ── CTA ──────────────────────────────────────────────────────────────────
    private JButton findTrainsBtn;
    private boolean routeOk = false;

    // ─────────────────────────────────────────────────────────────────────────

    public PlanDestinationTripDialog(Window owner, FeaturedDestination destination,
                                     Consumer<TrainSearchQuery> onSearchRequested) {
        super(owner,
              "Trip to " + destination.getSingleLineName(),
              560, 560);
        this.destination = destination;
        this.onSearchRequested = onSearchRequested;

        // Compact Bebas Neue header — "Trip to [Place]"
        setHeaderTitle("Trip to " + destination.getSingleLineName().toUpperCase());

        buildContent();
        setLocationRelativeTo(owner);

        // Initialise route state after UI is complete
        SwingUtilities.invokeLater(this::validateRoute);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Content builder
    // ─────────────────────────────────────────────────────────────────────────

    private void buildContent() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(0, 0, 4, 0));

        // ── Subtitle ─────────────────────────────────────────────────────────
        JLabel subtitle = new JLabel(destination.getLocationText());
        subtitle.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(0, 0, 14, 0));
        root.add(subtitle);

        // ── Station arrival pill ──────────────────────────────────────────────
        root.add(buildStationPill());
        root.add(Box.createVerticalStrut(18));

        // ── Route status banner (hidden by default) ───────────────────────────
        statusBanner = buildStatusBanner();
        statusBanner.setVisible(false);

        // ── Origin station ────────────────────────────────────────────────────
        root.add(fieldLabel("TRAVELING FROM"));
        root.add(Box.createVerticalStrut(6));
        root.add(buildOriginField());
        root.add(Box.createVerticalStrut(4));

        root.add(statusBanner);
        root.add(Box.createVerticalStrut(14));

        // ── Date + Quota row ──────────────────────────────────────────────────
        JPanel dateQuotaRow = new JPanel(new GridLayout(1, 2, 14, 0));
        dateQuotaRow.setOpaque(false);
        dateQuotaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateQuotaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        // Left: date
        JPanel dateCol = colPanel("JOURNEY DATE", buildDatePickerWidget());
        dateQuotaRow.add(dateCol);

        // Right: quota
        List<TravelQuota> quotas = Arrays.asList(TravelQuota.values());
        quotaDropdown = new ModernSmoothDropdown<>(quotas, TravelQuota.GENERAL);
        quotaDropdown.setTitleMapper(TravelQuota::getDisplayName);
        quotaDropdown.setPreferredSize(new Dimension(240, 42));
        quotaDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JPanel quotaCol = colPanel("TRAVEL QUOTA", quotaDropdown);
        dateQuotaRow.add(quotaCol);

        root.add(dateQuotaRow);
        root.add(Box.createVerticalStrut(14));

        // ── Class dropdown ────────────────────────────────────────────────────
        root.add(fieldLabel("SEATING CLASS"));
        root.add(Box.createVerticalStrut(6));

        List<String> classes = Arrays.asList(
                "All Classes",
                "1A \u2013 AC First Class",
                "2A \u2013 AC 2 Tier",
                "3A \u2013 AC 3 Tier",
                "SL \u2013 Sleeper",
                "CC \u2013 AC Chair Car",
                "EC \u2013 Exec. Chair Car"
        );
        classDropdown = new ModernSmoothDropdown<>(classes, "All Classes");
        classDropdown.setPreferredSize(new Dimension(500, 42));
        classDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        classDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(classDropdown);
        root.add(Box.createVerticalStrut(22));

        // ── Buttons row ───────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JButton cancelBtn = pillButton("Cancel", BTN_MUTED_BG, BTN_MUTED_TXT, false);
        cancelBtn.addActionListener(e -> dispose());

        // \u2192 is the clean right arrow →
        findTrainsBtn = pillButton("Find Trains  \u2192", BRAND, Color.WHITE, true);
        findTrainsBtn.addActionListener(e -> handleFindTrains());

        btnRow.add(cancelBtn);
        btnRow.add(findTrainsBtn);
        root.add(btnRow);

        getContentCard().add(root, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Widget builders
    // ─────────────────────────────────────────────────────────────────────────

    /** Arrival-station code pill. */
    private JPanel buildStationPill() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 247, 237)); // #FFF7ED
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
            }
        };
        pill.setOpaque(false);

        JLabel code = new JLabel(destination.getStationCode() + "  \u00B7  ARRIVAL STATION");
        code.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        code.setForeground(new Color(234, 88, 12)); // #EA580C
        pill.add(code);

        wrapper.add(pill);
        return wrapper;
    }

    /** Pill text field with StationAutocompleteDropdown wired up. */
    private JPanel buildOriginField() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        originField = ModernModalDialog.createPillTextField("Search stations \u2014 name, code, city\u2026");
        originField.setPreferredSize(new Dimension(500, 42));
        originField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Wire autocomplete — selection callback updates selectedOrigin + re-validates
        stationDropdown = new StationAutocompleteDropdown(
                originField, originField, stationDAO, null, station -> {
            selectedOrigin = station;
            validateRoute();
        });

        // Field starts empty with clean placeholder as requested
        selectedOrigin = null;

        wrap.add(originField, BorderLayout.CENTER);
        return wrap;
    }

    /** Date-picker wrapped in a borderless pill-shaped container. */
    private JPanel buildDatePickerWidget() {
        JPanel wrap = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANVAS);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 999, 999));
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 16, 0, 16));
        wrap.setPreferredSize(new Dimension(240, 42));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        datePicker = new ModernDatePicker(LocalDate.now().plusDays(1));
        datePicker.addDateChangeListener(d -> validateRoute());
        wrap.add(datePicker, BorderLayout.CENTER);
        return wrap;
    }

    /** Alert pill — only visible when route is unavailable. */
    private JPanel buildStatusBanner() {
        JPanel panel = new JPanel(new BorderLayout(8, 0)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(9, 14, 9, 14));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel();
        statusLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** Wraps a label + widget into a vertical column panel. */
    private JPanel colPanel(String label, JComponent widget) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.add(fieldLabel(label));
        col.add(Box.createVerticalStrut(6));
        widget.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(widget);
        return col;
    }

    /** Full pill button with correct arrow rendering via Unicode + bold font. */
    private JButton pillButton(String text, Color bg, Color fg, boolean isCta) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                Color paintBg = getBackground();
                if (!isEnabled()) {
                    paintBg = new Color(226, 232, 240);
                } else if (isCta && hovered) {
                    paintBg = BRAND_HOVER;
                }

                g2.setColor(paintBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));

                // Draw label text centred
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? getForeground() : TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };

        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        btn.setPreferredSize(new Dimension(160, 46));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Route validation — banner only shown on problems
    // ─────────────────────────────────────────────────────────────────────────

    private void validateRoute() {
        if (statusBanner == null || findTrainsBtn == null) {
            return;
        }

        if (selectedOrigin == null) {
            hideStatus();
            setFindEnabled(false);
            return;
        }

        String from = selectedOrigin.getCode().toUpperCase().trim();
        String to   = destination.getStationCode().toUpperCase().trim();

        if (from.equalsIgnoreCase(to)) {
            showStatus(WARN_BG, WARN_TXT, "\u26A0  Origin and destination cannot be the same station.");
            setFindEnabled(false);
            return;
        }

        if (!trainDAO.hasRoute(from, to)) {
            showStatus(DANGER_BG, DANGER_TXT,
                    "\u26A0  No direct route from " + selectedOrigin.getName()
                    + " (" + from + ") to " + destination.getSingleLineName()
                    + " (" + to + "). Please pick a different station.");
            setFindEnabled(false);
            return;
        }

        // Route is fine — hide banner entirely
        hideStatus();
        setFindEnabled(true);
    }

    private void showStatus(Color bg, Color fg, String message) {
        if (statusBanner == null || statusLabel == null) return;
        statusBanner.setBackground(bg);
        statusLabel.setForeground(fg);
        statusLabel.setText(message);
        if (!statusBanner.isVisible()) {
            statusBanner.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private void hideStatus() {
        if (statusBanner != null && statusBanner.isVisible()) {
            statusBanner.setVisible(false);
            revalidate();
            repaint();
        }
    }

    private void setFindEnabled(boolean enabled) {
        routeOk = enabled;
        if (findTrainsBtn != null) {
            findTrainsBtn.setEnabled(enabled);
            findTrainsBtn.setBackground(enabled ? BRAND : new Color(226, 232, 240));
            findTrainsBtn.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Find-trains action
    // ─────────────────────────────────────────────────────────────────────────

    private void handleFindTrains() {
        if (!routeOk || selectedOrigin == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        LocalDate date = datePicker != null
                ? datePicker.getSelectedDate()
                : LocalDate.now().plusDays(1);

        TravelQuota quota = quotaDropdown.getSelectedItem();
        if (quota == null) quota = TravelQuota.GENERAL;

        String rawClass = classDropdown.getSelectedItem();
        String preferredClass = "All Classes";
        // Parse "1A \u2013 AC First Class" -> "1A"
        if (rawClass != null && rawClass.contains(" \u2013 ")) {
            preferredClass = rawClass.substring(0, rawClass.indexOf(" \u2013 ")).trim();
        } else if (rawClass != null && rawClass.contains(" - ")) {
            preferredClass = rawClass.substring(0, rawClass.indexOf(" - ")).trim();
        }

        TrainSearchQuery query = new TrainSearchQuery(
                selectedOrigin.getCode(),
                destination.getStationCode(),
                date,
                quota,
                ConcessionType.NONE,
                preferredClass
        );

        dispose();

        if (onSearchRequested != null) {
            onSearchRequested.accept(query);
        }
    }
}
