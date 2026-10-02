package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.util.AssetManager;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Official High-Fidelity Electronic Reservation Slip (ERS) Digital Pass Dialog.
 * Extends {@link ModernModalDialog} for 50px card geometry, Bebas Neue headers, and pill buttons.
 * Renders official Indian Railways / RailFlow pass with QR matrix graphic, barcode,
 * passenger berth allocations, fare receipt breakdown, and native Print / Image export.
 */
public class ETicketPassDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final Booking booking;
    private final Runnable onStatusChangedCallback;
    private JPanel printablePassCard;

    public ETicketPassDialog(Window owner, Booking booking) {
        this(owner, booking, null);
    }

    public ETicketPassDialog(Window owner, Booking booking, Runnable onStatusChangedCallback) {
        super(owner, "RailFlow • Electronic Reservation Slip (ERS) • " + (booking != null ? booking.getPnr() : ""), 720, 760);
        this.booking = booking;
        this.onStatusChangedCallback = onStatusChangedCallback;

        setHeaderTitle("ELECTRONIC RESERVATION SLIP");
        initContent();
    }

    private void initContent() {
        JPanel mainContainer = new JPanel(new BorderLayout(0, 16));
        mainContainer.setOpaque(false);
        mainContainer.setBorder(new EmptyBorder(4, 12, 10, 12));

        // 1. Scrollable Printable Pass Container
        printablePassCard = buildPrintablePass();
        JScrollPane sp = new JScrollPane(printablePassCard);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(sp, BorderLayout.CENTER);

        // 2. Action Toolbar Footer (Print, Save PNG, Copy PNR, Cancel, Close)
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JButton copyPnrBtn = createPillButton("COPY PNR 📋", new Color(241, 245, 249), new Color(15, 23, 42));
        copyPnrBtn.setPreferredSize(new Dimension(130, 40));
        copyPnrBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(booking.getPnr()), null);
            JOptionPane.showMessageDialog(this, "PNR " + booking.getPnr() + " copied to clipboard!",
                    "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        footer.add(copyPnrBtn, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        if (booking != null && !booking.isCancelled()) {
            JButton cancelBtn = createPillButton("CANCEL RESERVATION ✕", new Color(254, 242, 242), new Color(239, 68, 68));
            cancelBtn.setPreferredSize(new Dimension(180, 40));
            cancelBtn.addActionListener(e -> {
                CancelTicketModalDialog cancelDialog = new CancelTicketModalDialog(
                        SwingUtilities.getWindowAncestor(this),
                        booking,
                        () -> {
                            dispose();
                            if (onStatusChangedCallback != null) {
                                onStatusChangedCallback.run();
                            }
                        }
                );
                cancelDialog.setVisible(true);
            });
            rightActions.add(cancelBtn);
        }

        JButton saveImgBtn = createPillButton("SAVE AS PNG 📥", new Color(241, 245, 249), new Color(2, 132, 199));
        saveImgBtn.setPreferredSize(new Dimension(150, 40));
        saveImgBtn.addActionListener(e -> handleSavePng());
        rightActions.add(saveImgBtn);

        JButton printBtn = createPillButton("PRINT TICKET 🖨️", new Color(250, 89, 9), Color.WHITE);
        printBtn.setPreferredSize(new Dimension(150, 40));
        printBtn.addActionListener(e -> handlePrint());
        rightActions.add(printBtn);

        footer.add(rightActions, BorderLayout.EAST);
        mainContainer.add(footer, BorderLayout.SOUTH);

        getContentCard().add(mainContainer, BorderLayout.CENTER);
    }

    private JPanel buildPrintablePass() {
        JPanel pass = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 4, w - 4, h - 4, 32, 32);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 32, 32);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pass.setOpaque(false);
        pass.setLayout(new BoxLayout(pass, BoxLayout.Y_AXIS));
        pass.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header: RailFlow E-Ticket Title & PNR Badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel brandCol = new JPanel();
        brandCol.setLayout(new BoxLayout(brandCol, BoxLayout.Y_AXIS));
        brandCol.setOpaque(false);

        JLabel brandTitle = new JLabel("INDIAN RAILWAYS • RAILFLOW E-TICKET");
        brandTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        brandTitle.setForeground(new Color(250, 89, 9)); // Brand Orange

        JLabel subTitle = new JLabel("ELECTRONIC RESERVATION SLIP (IRCTC COMPLIANT)");
        subTitle.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        subTitle.setForeground(new Color(100, 116, 139));

        brandCol.add(brandTitle);
        brandCol.add(Box.createVerticalStrut(2));
        brandCol.add(subTitle);
        topRow.add(brandCol, BorderLayout.WEST);

        // Status Badge & PNR
        JPanel pnrCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnrCol.setOpaque(false);

        JLabel pnrPill = new JLabel("PNR: " + booking.getPnr());
        pnrPill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        pnrPill.setForeground(new Color(15, 23, 42));
        pnrPill.setBackground(new Color(241, 245, 249));
        pnrPill.setOpaque(true);
        pnrPill.setBorder(new EmptyBorder(6, 14, 6, 14));
        pnrPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        pnrCol.add(pnrPill);

        boolean isConfirmed = "CONFIRMED".equalsIgnoreCase(booking.getStatus());
        JLabel statusPill = new JLabel(booking.getStatus().toUpperCase());
        statusPill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        statusPill.setForeground(isConfirmed ? new Color(16, 185, 129) : new Color(239, 68, 68));
        statusPill.setBackground(isConfirmed ? new Color(236, 253, 245) : new Color(254, 242, 242));
        statusPill.setOpaque(true);
        statusPill.setBorder(new EmptyBorder(6, 14, 6, 14));
        statusPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        pnrCol.add(statusPill);

        topRow.add(pnrCol, BorderLayout.EAST);
        pass.add(topRow);
        pass.add(Box.createVerticalStrut(14));

        if (!isConfirmed) {
            JPanel cancelBanner = new JPanel(new BorderLayout(8, 0));
            cancelBanner.setBackground(new Color(254, 242, 242));
            cancelBanner.setBorder(new EmptyBorder(10, 16, 10, 16));
            cancelBanner.putClientProperty(FlatClientProperties.STYLE, "arc: 16;");

            JLabel cancelMsg = new JLabel("THIS TICKET HAS BEEN CANCELLED • SEAT RESERVATION RELEASED");
            cancelMsg.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            cancelMsg.setForeground(new Color(220, 38, 38));
            cancelBanner.add(cancelMsg, BorderLayout.CENTER);

            pass.add(cancelBanner);
            pass.add(Box.createVerticalStrut(14));
        } else {
            pass.add(Box.createVerticalStrut(4));
        }

        // Train Details Box
        JPanel trainBox = new JPanel(new BorderLayout(14, 0));
        trainBox.putClientProperty(FlatClientProperties.STYLE, "arc: 20; background: #F8FAFC;");
        trainBox.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel tLeft = new JPanel();
        tLeft.setLayout(new BoxLayout(tLeft, BoxLayout.Y_AXIS));
        tLeft.setOpaque(false);

        JLabel tName = new JLabel(booking.getTrainNumber() + "  " + booking.getTrainName().toUpperCase());
        tName.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        tName.setForeground(new Color(15, 23, 42));

        String dateStr = booking.getJourneyDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        String userInfo = (booking.getUserIdentifier() != null && !booking.getUserIdentifier().isBlank())
                ? "  •  Booked By: " + booking.getUserIdentifier() : "";
        JLabel tDate = new JLabel("Journey Date: " + dateStr + "  •  Class: " + booking.getClassCode() + "  •  Quota: " + booking.getQuotaCode() + userInfo);
        tDate.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        tDate.setForeground(new Color(100, 116, 139));

        tLeft.add(tName);
        tLeft.add(Box.createVerticalStrut(4));
        tLeft.add(tDate);
        trainBox.add(tLeft, BorderLayout.CENTER);

        pass.add(trainBox);
        pass.add(Box.createVerticalStrut(18));

        // Corridor Schedule Stepper Bar
        JPanel routePanel = new JPanel(new GridLayout(1, 3, 16, 0));
        routePanel.setOpaque(false);

        routePanel.add(createStationTimeCard("DEPARTURE", booking.getFromStationName() + " (" + booking.getFromStationCode() + ")", booking.getDepartureTime()));
        routePanel.add(createMidwayArrowCard());
        routePanel.add(createStationTimeCard("ARRIVAL", booking.getToStationName() + " (" + booking.getToStationCode() + ")", booking.getArrivalTime()));

        pass.add(routePanel);
        pass.add(Box.createVerticalStrut(18));

        // Perforated Divider Line
        pass.add(new PerforatedDividerPanel());
        pass.add(Box.createVerticalStrut(18));

        // Passenger Berth Table
        JLabel passTitle = new JLabel("PASSENGER DETAILS & BERTH ALLOCATIONS");
        passTitle.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 18f));
        passTitle.setForeground(new Color(15, 23, 42));
        pass.add(passTitle);
        pass.add(Box.createVerticalStrut(8));

        String[] cols = {"#", "Passenger Name", "Age", "Gender", "Coach", "Berth No.", "Berth Type", "Status"};
        List<BookingPassenger> passengers = booking.getPassengers();
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (passengers != null && !passengers.isEmpty()) {
            for (int i = 0; i < passengers.size(); i++) {
                BookingPassenger p = passengers.get(i);
                model.addRow(new Object[]{
                        String.valueOf(i + 1),
                        p.getPassengerName(),
                        p.getAge() + " yrs",
                        p.getGender(),
                        p.getCoachNumber() != null ? p.getCoachNumber() : "B2",
                        p.getSeatNumber() > 0 ? String.valueOf(p.getSeatNumber()) : "34",
                        p.getBerthPreference() != null ? p.getBerthPreference() : "LOWER",
                        p.getStatus() != null ? p.getStatus() : "CNF"
                });
            }
        } else {
            model.addRow(new Object[]{"1", "Passenger 1", "26 yrs", "M", "B2", "34", "LOWER", "CNF"});
        }

        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        table.setForeground(new Color(15, 23, 42));
        table.setBackground(Color.WHITE);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));

        table.getTableHeader().setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(100, 116, 139));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane tableSp = new JScrollPane(table);
        tableSp.setBorder(BorderFactory.createEmptyBorder());
        tableSp.putClientProperty(FlatClientProperties.STYLE, "arc: 16;");
        tableSp.setPreferredSize(new Dimension(600, Math.min(160, 36 + model.getRowCount() * 34)));
        tableSp.setMaximumSize(new Dimension(800, 160));
        pass.add(tableSp);
        pass.add(Box.createVerticalStrut(18));

        // Fare Breakdown & QR Matrix Section
        JPanel bottomSection = new JPanel(new BorderLayout(24, 0));
        bottomSection.setOpaque(false);

        // QR Matrix Graphic
        JPanel qrPanel = createQrMatrixGraphic();
        bottomSection.add(qrPanel, BorderLayout.WEST);

        // Fare Breakdown Box
        JPanel fareBox = new JPanel(new GridLayout(4, 2, 10, 6));
        fareBox.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #F8FAFC;");
        fareBox.setBorder(new EmptyBorder(12, 16, 12, 16));

        addFareRow(fareBox, "Ticket Base Fare", "₹" + String.format("%,.0f", booking.getTotalFare() * 0.92));
        addFareRow(fareBox, "Reservation & Superfast Surcharge", "₹60.00");
        addFareRow(fareBox, "IRCTC Service GST (5%)", "₹" + String.format("%,.0f", booking.getTotalFare() * 0.05));
        addFareRow(fareBox, "TOTAL AMOUNT PAID", "₹" + String.format("%,.0f", booking.getTotalFare()));

        bottomSection.add(fareBox, BorderLayout.CENTER);
        pass.add(bottomSection);
        pass.add(Box.createVerticalStrut(14));

        // Advisory Note
        JLabel advisory = new JLabel("<html><font color='#64748B'><b>IMPORTANT ADVISORY:</b> Original government photo identity card is mandatory during the journey. This electronic reservation slip is legally valid under Indian Railways passenger rules.</font></html>");
        advisory.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        pass.add(advisory);

        return pass;
    }

    private JPanel createStationTimeCard(String label, String station, String time) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #F8FAFC;");
        p.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        l.setForeground(new Color(100, 116, 139));

        JLabel s = new JLabel(station);
        s.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        s.setForeground(new Color(15, 23, 42));

        JLabel t = new JLabel(time != null ? time : "--:--");
        t.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 20f));
        t.setForeground(new Color(2, 132, 199));

        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(s);
        p.add(Box.createVerticalStrut(4));
        p.add(t);
        return p;
    }

    private JPanel createMidwayArrowCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel arrow = new JLabel("➔  DIRECT ROUTE  ➔", SwingConstants.CENTER);
        arrow.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        arrow.setForeground(new Color(250, 89, 9));
        p.add(arrow, BorderLayout.CENTER);
        return p;
    }

    private void addFareRow(JPanel parent, String label, String value) {
        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", label.startsWith("TOTAL") ? Font.BOLD : Font.PLAIN, 11f));
        l.setForeground(label.startsWith("TOTAL") ? new Color(15, 23, 42) : new Color(100, 116, 139));

        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, label.startsWith("TOTAL") ? 13f : 11f));
        v.setForeground(label.startsWith("TOTAL") ? new Color(250, 89, 9) : new Color(15, 23, 42));

        parent.add(l);
        parent.add(v);
    }

    private JPanel createQrMatrixGraphic() {
        return new JPanel() {
            {
                setPreferredSize(new Dimension(96, 96));
                setMaximumSize(new Dimension(96, 96));
                putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFFFFF;");
                setBorder(new EmptyBorder(4, 4, 4, 4));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                int size = 96;
                int modules = 16;
                int blockSize = size / modules;
                int hash = (booking.getPnr() + booking.getTrainNumber()).hashCode();

                g2.setColor(new Color(15, 23, 42));
                for (int r = 0; r < modules; r++) {
                    for (int c = 0; c < modules; c++) {
                        // Corner finder patterns
                        if ((r < 4 && c < 4) || (r < 4 && c >= modules - 4) || (r >= modules - 4 && c < 4)) {
                            if (r == 0 || r == 3 || c == 0 || c == 3 || (r >= modules - 4 && (r == modules - 4 || r == modules - 1))
                                    || (c >= modules - 4 && (c == modules - 4 || c == modules - 1))) {
                                g2.fillRect(c * blockSize + 2, r * blockSize + 2, blockSize, blockSize);
                            }
                        } else if (((hash >> ((r * c) % 31)) & 1) == 1) {
                            g2.fillRect(c * blockSize + 2, r * blockSize + 2, blockSize, blockSize);
                        }
                    }
                }
                g2.dispose();
            }
        };
    }

    private void handleSavePng() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save E-Ticket Pass Image");
        chooser.setSelectedFile(new File("railflow_eticket_" + booking.getPnr() + ".png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".png")) {
                target = new File(target.getAbsolutePath() + ".png");
            }

            try {
                int width = printablePassCard.getWidth() > 0 ? printablePassCard.getWidth() : 680;
                int height = printablePassCard.getHeight() > 0 ? printablePassCard.getHeight() : 540;
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, width, height);
                printablePassCard.setSize(width, height);
                printablePassCard.doLayout();
                printablePassCard.paint(g2);
                g2.dispose();

                ImageIO.write(image, "png", target);
                JOptionPane.showMessageDialog(this, "E-Ticket saved successfully: " + target.getName(),
                        "Ticket Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save image: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handlePrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("RailFlow E-Ticket - PNR " + booking.getPnr());

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            double scaleX = pageFormat.getImageableWidth() / printablePassCard.getWidth();
            double scaleY = pageFormat.getImageableHeight() / printablePassCard.getHeight();
            double scale = Math.min(scaleX, scaleY);
            g2.scale(scale, scale);

            printablePassCard.paint(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "E-Ticket sent to printer successfully!",
                        "Printing", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(),
                        "Printer Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class PerforatedDividerPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f));
            int y = getHeight() / 2;
            g2.drawLine(0, y, getWidth(), y);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 10);
        }
    }
}
