package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.Station;
import com.trainticket.model.dao.FeaturedDestinationDAO;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Modal dialog for Station Masters / Administrators to add or edit a Featured Destination.
 * Features seamless PC photo file upload with real-time thumbnail preview,
 * preset photography selectors, and pure universal light styling.
 */
public class AddDestinationDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] PRESET_IMAGES = {
            "/assets/images/taj-mahal.png",
            "/assets/images/dal-lake.png",
            "/assets/images/Munnar, Kerala.png",
            "/assets/images/lake-in-ladakh.png",
            "/assets/images/hawa-mahal.png",
            "/assets/images/Varanasi ghat.png",
            "/assets/images/Lake Palace (Jag Niwas).png",
            "/assets/images/Alleppey,kerela.png",
            "/assets/images/statue-of-unity.png"
    };

    private static final String[] LAYOUT_STYLES = {
            "center",
            "staircase",
            "left",
            "right",
            "staircase:0.55"
    };

    private final StationDAO stationDAO = new StationDAO();
    private final FeaturedDestinationDAO destinationDAO = FeaturedDestinationDAO.getInstance();
    private final FeaturedDestination destinationToEdit;
    private final Runnable onDestinationSaved;

    private JTextField nameField;
    private JTextField locationField;
    private JComboBox<Station> stationCombo;
    private JComboBox<String> layoutStyleCombo;
    private JComboBox<String> presetCombo;

    private String currentImagePath = "/assets/images/taj-mahal.png";
    private JPanel imagePreviewBox;
    private JLabel imageFileNameLabel;

    public AddDestinationDialog(Window owner, Runnable onDestinationSaved) {
        this(owner, null, onDestinationSaved);
    }

    public AddDestinationDialog(Window owner, FeaturedDestination destinationToEdit, Runnable onDestinationSaved) {
        super(owner, destinationToEdit == null ? "Admin • Add Featured Destination" : "Admin • Edit Featured Destination", 530, 600);
        this.destinationToEdit = destinationToEdit;
        this.onDestinationSaved = onDestinationSaved;

        if (destinationToEdit != null) {
            this.currentImagePath = destinationToEdit.getImagePath();
        }

        setHeaderTitle(destinationToEdit == null ? "ADD FEATURED DESTINATION" : "EDIT FEATURED DESTINATION");
        buildContent();
    }

    private void buildContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(4, 12, 8, 12));

        // 1. Destination / Monument Name
        container.add(createFieldLabel("DESTINATION / MONUMENT NAME"));
        container.add(Box.createVerticalStrut(6));
        nameField = createPillTextField("e.g. GOLDEN TEMPLE or CHARMINAR");
        if (destinationToEdit != null) {
            nameField.setText(destinationToEdit.getSingleLineName());
        }
        container.add(nameField);
        container.add(Box.createVerticalStrut(12));

        // 2. Location
        container.add(createFieldLabel("LOCATION (CITY, STATE)"));
        container.add(Box.createVerticalStrut(6));
        locationField = createPillTextField("e.g. Amritsar, Punjab");
        if (destinationToEdit != null) {
            locationField.setText(destinationToEdit.getLocationText());
        }
        container.add(locationField);
        container.add(Box.createVerticalStrut(12));

        // 3. Mapped Railway Station
        container.add(createFieldLabel("MAPPED RAILWAY STATION"));
        container.add(Box.createVerticalStrut(6));

        stationCombo = new JComboBox<>();
        stationCombo.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        stationCombo.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #0284C7; focusedBackground: #FFFFFF;");
        stationCombo.setPreferredSize(new Dimension(480, 42));
        stationCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        stationCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(6, 14, 6, 14));
                if (value instanceof Station s) {
                    setText(s.getDisplayName() + " — " + s.getCity() + ", " + s.getState());
                }
                return this;
            }
        });

        populateStations();
        if (destinationToEdit != null) {
            for (int i = 0; i < stationCombo.getItemCount(); i++) {
                Station s = stationCombo.getItemAt(i);
                if (s.getCode().equalsIgnoreCase(destinationToEdit.getStationCode())) {
                    stationCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        container.add(stationCombo);
        container.add(Box.createVerticalStrut(12));

        // 4. Destination Image Upload & Selection (Minimal and clean, no heavy nested cards)
        container.add(createFieldLabel("DESTINATION PHOTO"));
        container.add(Box.createVerticalStrut(6));

        JPanel imagePickerRow = new JPanel(new BorderLayout(14, 0));
        imagePickerRow.setOpaque(false);
        imagePickerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        // Thumbnail Preview in 4:3 portrait orientation
        imagePreviewBox = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();
                int arc = 20;

                RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
                g2.setClip(clip);

                Image img = AssetManager.getImage(currentImagePath);
                if (img != null) {
                    int imgW = img.getWidth(null);
                    int imgH = img.getHeight(null);
                    if (imgW > 0 && imgH > 0) {
                        double scale = Math.max((double) w / imgW, (double) h / imgH);
                        int drawW = (int) Math.round(imgW * scale);
                        int drawH = (int) Math.round(imgH * scale);
                        int drawX = (w - drawW) / 2;
                        int drawY = (h - drawH) / 2;
                        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
                    }
                } else {
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(new Color(148, 163, 184));
                    g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
                    FontMetrics fm = g2.getFontMetrics();
                    String t = "PREVIEW";
                    g2.drawString(t, (w - fm.stringWidth(t)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                }
                g2.dispose();
            }
        };
        imagePreviewBox.setOpaque(false);
        imagePreviewBox.setPreferredSize(new Dimension(60, 80));
        imagePickerRow.add(imagePreviewBox, BorderLayout.WEST);

        // Upload Button + Presets combo
        JPanel imageControls = new JPanel();
        imageControls.setOpaque(false);
        imageControls.setLayout(new BoxLayout(imageControls, BoxLayout.Y_AXIS));

        JPanel buttonsLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonsLine.setOpaque(false);

        JButton uploadBtn = createPillButton("UPLOAD FROM PC", new Color(2, 132, 199), Color.WHITE);
        uploadBtn.setPreferredSize(new Dimension(145, 34));
        uploadBtn.addActionListener(e -> handleUploadImageFromPC());
        buttonsLine.add(uploadBtn);

        presetCombo = new JComboBox<>(PRESET_IMAGES);
        presetCombo.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        presetCombo.setPreferredSize(new Dimension(200, 34));
        presetCombo.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0;");
        presetCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(4, 10, 4, 10));
                if (value != null) {
                    String str = value.toString();
                    int lastSlash = str.lastIndexOf('/');
                    setText(lastSlash >= 0 ? str.substring(lastSlash + 1) : str);
                }
                return this;
            }
        });
        presetCombo.addActionListener(e -> {
            String selected = (String) presetCombo.getSelectedItem();
            if (selected != null && !selected.isBlank()) {
                currentImagePath = selected;
                updateImageDisplay();
            }
        });
        buttonsLine.add(presetCombo);
        imageControls.add(buttonsLine);
        imageControls.add(Box.createVerticalStrut(4));

        imageFileNameLabel = new JLabel(formatImageName(currentImagePath));
        imageFileNameLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        imageFileNameLabel.setForeground(new Color(100, 116, 139));
        imageFileNameLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        imageControls.add(imageFileNameLabel);

        imagePickerRow.add(imageControls, BorderLayout.CENTER);
        container.add(imagePickerRow);
        container.add(Box.createVerticalStrut(12));

        // 5. Typography Layout Style
        container.add(createFieldLabel("TYPOGRAPHY LAYOUT STYLE"));
        container.add(Box.createVerticalStrut(6));

        layoutStyleCombo = new JComboBox<>(LAYOUT_STYLES);
        layoutStyleCombo.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        layoutStyleCombo.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #0284C7; focusedBackground: #FFFFFF;");
        layoutStyleCombo.setPreferredSize(new Dimension(480, 42));
        layoutStyleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        if (destinationToEdit != null) {
            String currentStyle = destinationToEdit.getLayoutStyle();
            if (currentStyle == null || currentStyle.isBlank()) {
                layoutStyleCombo.setSelectedItem("center");
            } else {
                layoutStyleCombo.setSelectedItem(currentStyle);
            }
        }
        container.add(layoutStyleCombo);
        container.add(Box.createVerticalStrut(20));

        // 6. Action Buttons Row: Cancel + Save Button
        JPanel buttonsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonsRow.setOpaque(false);
        buttonsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton cancelBtn = createPillButton("CANCEL", new Color(241, 245, 249), new Color(71, 85, 105));
        cancelBtn.setPreferredSize(new Dimension(150, 44));
        cancelBtn.addActionListener(e -> dispose());
        buttonsRow.add(cancelBtn);

        String saveTitle = destinationToEdit == null ? "+ ADD DESTINATION" : "SAVE CHANGES";
        JButton saveBtn = createPillButton(saveTitle, new Color(2, 132, 199), Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(200, 44));
        saveBtn.addActionListener(e -> handleSaveDestination());
        buttonsRow.add(saveBtn);

        container.add(buttonsRow);
        getContentCard().add(container, BorderLayout.CENTER);
    }

    private void handleUploadImageFromPC() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Destination Image from PC");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.jpeg, *.webp)", "png", "jpg", "jpeg", "webp");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists()) {
                try {
                    String cleanName = selectedFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");

                    // Try copying into project assets directory
                    File resDir = new File("src/main/resources/assets/images");
                    if (resDir.exists() && resDir.isDirectory()) {
                        File destRes = new File(resDir, cleanName);
                        Files.copy(selectedFile.toPath(), destRes.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        // Also copy to compiled target directory for immediate hot availability
                        File targetClasses = new File("target/classes/assets/images");
                        if (targetClasses.exists() && targetClasses.isDirectory()) {
                            Files.copy(selectedFile.toPath(), new File(targetClasses, cleanName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        currentImagePath = "/assets/images/" + cleanName;
                    } else {
                        // Direct file system path fallback
                        currentImagePath = selectedFile.getAbsolutePath();
                    }

                    AssetManager.evictImage(currentImagePath);
                    updateImageDisplay();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Could not load image: " + ex.getMessage(),
                            "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updateImageDisplay() {
        if (imageFileNameLabel != null) {
            imageFileNameLabel.setText(formatImageName(currentImagePath));
        }
        if (imagePreviewBox != null) {
            imagePreviewBox.repaint();
        }
    }

    private String formatImageName(String path) {
        if (path == null || path.isBlank()) return "No image selected";
        int lastSlash = path.replace('\\', '/').lastIndexOf('/');
        String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return "Image: " + filename;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(new Color(100, 116, 139)); // #64748B
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void populateStations() {
        List<Station> stations = stationDAO.getAllStations();
        for (Station s : stations) {
            stationCombo.addItem(s);
        }
    }

    private void handleSaveDestination() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        Station station = (Station) stationCombo.getSelectedItem();
        String style = (String) layoutStyleCombo.getSelectedItem();
        if ("center".equalsIgnoreCase(style)) {
            style = "";
        }

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the destination monument name.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }

        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the location (city, state).",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            locationField.requestFocusInWindow();
            return;
        }

        if (station == null) {
            JOptionPane.showMessageDialog(this, "Please select the nearest railway station.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Format name with newline if two words (for monumental Bebas Neue card styling on passenger screens)
        String formattedName = name;
        if (!name.contains("\n") && name.contains(" ")) {
            int spaceIdx = name.indexOf(' ');
            formattedName = name.substring(0, spaceIdx) + "\n" + name.substring(spaceIdx + 1);
        }

        if (destinationToEdit == null) {
            FeaturedDestination newDest = new FeaturedDestination(
                    0L,
                    formattedName.toUpperCase(),
                    location,
                    station.getCode(),
                    currentImagePath,
                    style != null ? style : ""
            );
            destinationDAO.addDestination(newDest);
        } else {
            FeaturedDestination updatedDest = new FeaturedDestination(
                    destinationToEdit.getId(),
                    formattedName.toUpperCase(),
                    location,
                    station.getCode(),
                    currentImagePath,
                    style != null ? style : ""
            );
            destinationDAO.updateDestination(updatedDest);
        }

        dispose();

        if (onDestinationSaved != null) {
            onDestinationSaved.run();
        }
    }
}
