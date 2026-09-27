package com.trainticket.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.trainticket.util.AssetManager;
import com.trainticket.util.AudioManager;
import com.trainticket.view.component.VideoBackgroundPanel;
import com.trainticket.view.pages.HomeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Main application window for RailFlow.
 * Handles the top minimalist navigation bar, OS taskbar/dock icon integration,
 * background video rendering, and page view switching.
 */
public class MainFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static final String APP_ICON_PATH = "/assets/icons/icon.png";
    private static final String APP_WIDE_ICON_PATH = "/assets/icons/icon-wide.png";

    private JPanel contentContainer;
    private HomeView homeView;
    private VideoBackgroundPanel videoBackgroundPanel;

    public MainFrame() {
        super("RailFlow — Train Ticket Management");
        initWindow();
        initAppIcon();
        initComponents();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null); // Center on screen

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (videoBackgroundPanel != null) {
                    videoBackgroundPanel.dispose();
                }
                AudioManager.dispose();
            }

            @Override
            public void windowIconified(WindowEvent e) {
                if (videoBackgroundPanel != null) {
                    videoBackgroundPanel.pauseVideo();
                }
                AudioManager.pause();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                if (videoBackgroundPanel != null) {
                    videoBackgroundPanel.resumeVideo();
                }
                AudioManager.resume();
            }
        });
    }

    /**
     * Binds the application icon to both the window title bar
     * and the OS Taskbar / macOS Dock.
     */
    private void initAppIcon() {
        Image appIcon = AssetManager.getImage(APP_ICON_PATH);
        if (appIcon != null) {
            // Set window title bar icon
            setIconImage(appIcon);

            // Set macOS Dock / OS Taskbar icon
            try {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.setIconImage(appIcon);
                    }
                }
            } catch (Exception ex) {
                logger.warn("Could not bind icon to OS taskbar: {}", ex.getMessage());
            }
        } else {
            logger.warn("App icon not found at classpath path: {}", APP_ICON_PATH);
        }
    }

    private void initComponents() {
        // Layered pane hosting the video background at Layer 0 and UI components at Layer 1
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false; // Enable proper overlapping repainting
            }

            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                for (Component c : getComponents()) {
                    c.setBounds(0, 0, w, h);
                }
            }
        };
        layeredPane.setBackground(new Color(15, 18, 26));
        layeredPane.setOpaque(true);

        // 1. Bottom Layer (0): High-performance looping video background
        videoBackgroundPanel = new VideoBackgroundPanel();
        layeredPane.add(videoBackgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // 2. Top Layer (1): Passive UI Views & Navigation Header with extended ambient scrim
        JPanel uiOverlayPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int gradientHeight = 240; // Extended smooth falloff height

                // Ultra-smooth subtle dark ambient scrim fading gently into the video
                float[] fractions = {0.0f, 0.35f, 0.70f, 1.0f};
                Color[] colors = {
                    new Color(0, 0, 0, 95),  // Soft, subtle starting black (~37% opacity)
                    new Color(0, 0, 0, 50),  // Smooth feathering
                    new Color(0, 0, 0, 15),  // Delicate ambient falloff
                    new Color(0, 0, 0, 0)    // Fully dissipated at 240px
                };
                LinearGradientPaint gradient = new LinearGradientPaint(0, 0, 0, gradientHeight, fractions, colors);
                g2.setPaint(gradient);
                g2.fillRect(0, 0, w, gradientHeight);
                g2.dispose();
            }
        };
        uiOverlayPanel.setOpaque(false);

        // Top Minimalist Header
        uiOverlayPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // Main Content View Container
        contentContainer = new JPanel(new CardLayout());
        contentContainer.setOpaque(false);

        homeView = new HomeView();
        contentContainer.add(homeView, "HOME");

        uiOverlayPanel.add(contentContainer, BorderLayout.CENTER);

        layeredPane.add(uiOverlayPanel, JLayeredPane.PALETTE_LAYER);

        setContentPane(layeredPane);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 32, 28, 32));

        // Brand & Logo Left - Uses icon-wide.png for heading
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);

        Image wideIcon = AssetManager.getImage(APP_WIDE_ICON_PATH);
        if (wideIcon != null) {
            // Aspect ratio of icon-wide.png is 2000x400 (5:1) -> 175x35
            int logoHeight = 35;
            int logoWidth = logoHeight * 5;
            Image scaledLogo = wideIcon.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            brandPanel.add(logoLabel);
        } else {
            JLabel titleLabel = new JLabel("RailFlow");
            titleLabel.setFont(new Font("Inter", Font.BOLD, 22));
            titleLabel.setForeground(new Color(248, 250, 252));
            brandPanel.add(titleLabel);
        }

        header.add(brandPanel, BorderLayout.WEST);

        // Navigation Navigation Pills (Center)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        navPanel.setOpaque(false);

        JButton bookBtn = createNavPill("Book Journey", true);
        JButton pnrBtn = createNavPill("PNR Status", false);
        JButton scheduleBtn = createNavPill("Train Schedule", false);
        JButton adminBtn = createNavPill("Admin", false);

        navPanel.add(bookBtn);
        navPanel.add(pnrBtn);
        navPanel.add(scheduleBtn);
        navPanel.add(adminBtn);

        header.add(navPanel, BorderLayout.CENTER);

        // Right Action Profile Pill & Ambient Audio Toggle
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        // Ambient Audio Toggle Button
        FlatSVGIcon speakerOffIcon = new FlatSVGIcon("assets/icons/speaker-off.svg", 18, 18);
        FlatSVGIcon speakerOnIcon = new FlatSVGIcon("assets/icons/speaker-on.svg", 18, 18);

        JButton speakerBtn = new JButton(speakerOffIcon);
        speakerBtn.setToolTipText("Play ambient journey sound");
        speakerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        speakerBtn.setContentAreaFilled(false);
        speakerBtn.setBorderPainted(false);
        speakerBtn.setFocusPainted(false);
        speakerBtn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #F1F5F9; hoverForeground: #FA5909; borderWidth: 0;");

        speakerBtn.addActionListener(e -> {
            AudioManager.toggleAmbientSound(isPlaying -> {
                String dev = AudioManager.getActiveDeviceName();
                if (isPlaying) {
                    speakerBtn.setIcon(speakerOnIcon);
                    speakerBtn.setToolTipText("Mute ambient sound (" + dev + ") • Right-click to switch device");
                    speakerBtn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #FA5909; hoverForeground: #E04D05; borderWidth: 0;");
                } else {
                    speakerBtn.setIcon(speakerOffIcon);
                    speakerBtn.setToolTipText("Play ambient journey sound • Right-click to switch device");
                    speakerBtn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #F1F5F9; hoverForeground: #FA5909; borderWidth: 0;");
                }
            });
        });

        // Right-click context menu to manually select audio output hardware if desired
        speakerBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    showAudioDevicePopup(speakerBtn, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAudioDevicePopup(speakerBtn, e.getX(), e.getY());
                }
            }
        });

        rightPanel.add(speakerBtn);

        JButton accountBtn = new JButton("My Account");
        accountBtn.setFont(new Font("Inter", Font.PLAIN, 13));
        accountBtn.setContentAreaFilled(false);
        accountBtn.setBorderPainted(false);
        accountBtn.setFocusPainted(false);
        accountBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        accountBtn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #F1F5F9; hoverForeground: #FFFFFF; borderWidth: 0;");
        rightPanel.add(accountBtn);

        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JButton createNavPill(String title, boolean active) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Inter", active ? Font.BOLD : Font.PLAIN, 14));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (active) {
            // Primary brand orange text, no background, no border
            btn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #FA5909; borderWidth: 0;");
        } else {
            // Clean high-contrast off-white text, no background, no border
            btn.putClientProperty(FlatClientProperties.STYLE, "background: #00000000; foreground: #F1F5F9; hoverForeground: #FA5909; borderWidth: 0;");
        }
        return btn;
    }

    private void showAudioDevicePopup(Component invoker, int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        popup.putClientProperty(FlatClientProperties.STYLE, "arc: 16;");

        JLabel title = new JLabel("Audio Output Source");
        title.setFont(new Font("Inter", Font.BOLD, 12));
        title.setBorder(new EmptyBorder(6, 12, 4, 12));
        popup.add(title);
        popup.addSeparator();

        String currentSelected = AudioManager.getSelectedOutputDevice();
        List<String> devices = AudioManager.getAvailableOutputDevices();

        ButtonGroup group = new ButtonGroup();
        for (String device : devices) {
            boolean isSelected = device.equalsIgnoreCase(currentSelected) ||
                    ("Auto".equalsIgnoreCase(currentSelected) && device.startsWith("Auto"));
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(device, isSelected);
            item.setFont(new Font("Inter", Font.PLAIN, 12));
            item.addActionListener(ev -> {
                String chosen = device.startsWith("Auto") ? "Auto" : device;
                AudioManager.setSelectedOutputDevice(chosen);
            });
            group.add(item);
            popup.add(item);
        }

        popup.show(invoker, x, y);
    }
}
