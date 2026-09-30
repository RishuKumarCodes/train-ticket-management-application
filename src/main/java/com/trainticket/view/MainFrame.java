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
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.view.dialog.AuthDialog;
import com.trainticket.view.admin.AdminDashboardFrame;

/**
 * Main application window for RailFlow.
 * Handles the top minimalist navigation bar, OS taskbar/dock icon integration,
 * background video rendering, and page view switching.
 */
public class MainFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static final String APP_ICON_PATH = "/assets/icons/icon.png";
    private static final String APP_WIDE_ICON_PATH = "/assets/icons/icon-wide-white.png";

    private JPanel contentContainer;
    private HomeView homeView;
    private VideoBackgroundPanel videoBackgroundPanel;
    private JButton authBtn;

    public MainFrame() {
        super("");
        initWindow();
        initAppIcon();
        initComponents();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null); // Center on screen

        // Enable cross-platform full-window content so video background extends to the
        // very top edge
        JRootPane root = getRootPane();
        root.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        root.putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
        root.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false);
        root.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false);
        root.putClientProperty("apple.awt.fullWindowContent", true);
        root.putClientProperty("apple.awt.transparentTitleBar", true);
        root.putClientProperty("apple.awt.windowTitleVisible", false);

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

        // In-app live reload shortcut (Cmd+R on Mac, Ctrl+R on Windows/Linux, or F5)
        KeyStroke cmdR = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke f5 = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0);
        getRootPane().registerKeyboardAction(e -> reloadCurrentView(), cmdR, JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> reloadCurrentView(), f5, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Hot-reloads the active page view without restarting the application.
     */
    public void reloadCurrentView() {
        SwingUtilities.invokeLater(() -> {
            logger.info("Hot-reloading UI view on user shortcut (Cmd/Ctrl + R)...");
            contentContainer.removeAll();
            homeView = new HomeView();
            contentContainer.add(homeView, "HOME");
            contentContainer.revalidate();
            contentContainer.repaint();
            logger.info("UI view hot-reloaded successfully.");
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
        // Layered pane hosting the video background at Layer 0 and UI components at
        // Layer 1
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

        // 2. Top Layer (1): Passive UI Views & Navigation Header with extended ambient
        // scrim
        JPanel uiOverlayPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int gradientHeight = 240; // Extended smooth falloff height

                // Ultra-smooth subtle dark ambient scrim fading gently into the video
                float[] fractions = { 0.0f, 0.35f, 0.70f, 1.0f };
                Color[] colors = {
                        new Color(0, 0, 0, 95), // Soft, subtle starting black (~37% opacity)
                        new Color(0, 0, 0, 50), // Smooth feathering
                        new Color(0, 0, 0, 15), // Delicate ambient falloff
                        new Color(0, 0, 0, 0) // Fully dissipated at 240px
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
        header.setBorder(new EmptyBorder(36, 48, 16, 48));
        header.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true);

        // 1. Brand Logo Left (icon-wide-white.png) + Circular White Music Toggle Button
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        brandPanel.setOpaque(false);

        Image wideIcon = AssetManager.getImage(APP_WIDE_ICON_PATH);
        if (wideIcon != null) {
            // Aspect ratio of icon-wide-white.png is 2000x400 (5:1) -> 180x36
            int logoHeight = 36;
            int logoWidth = logoHeight * 5;
            JComponent logoComp = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.drawImage(wideIcon, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            };
            logoComp.setPreferredSize(new Dimension(logoWidth, logoHeight));
            logoComp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            brandPanel.add(logoComp);
        } else {
            JLabel brandTitle = new JLabel("RAILFLOW");
            brandTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 20f));
            brandTitle.setForeground(Color.WHITE);
            brandTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            brandPanel.add(brandTitle);
        }

        // Ambient Audio Toggle Button with White Circular Background & Music Note Icons
        FlatSVGIcon musicOnIcon = new FlatSVGIcon("assets/icons/music.svg", 20, 20);
        FlatSVGIcon musicCutIcon = new FlatSVGIcon("assets/icons/music-cut.svg", 20, 20);

        JButton musicBtn = new JButton(musicCutIcon) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                boolean playing = AudioManager.isPlaying();

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isPressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                if (!playing) {
                    // MUSIC OFF: Solid white circular pill + subtle drop shadow
                    g2.setColor(new Color(0, 0, 0, 22));
                    g2.fillOval(1, 2, w - 2, h - 2);

                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillOval(0, 0, w, h);
                } else {
                    // MUSIC ON: Dark black smoked glass with live backdrop blur (matching
                    // navCapsule)
                    g2.setColor(new Color(0, 0, 0, 45));
                    g2.fillOval(0, 2, w, h);

                    // Translucent dark glass fill matching navCapsule (rgba(15, 23, 42, 60))
                    g2.setColor(isHovered ? new Color(15, 23, 42, 100) : new Color(15, 23, 42, 60));
                    g2.fillOval(0, 0, w, h);

                    if (isHovered) {
                        g2.setColor(new Color(255, 255, 255, 25));
                        g2.fillOval(1, 1, w - 2, h - 2);
                    }
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        musicBtn.setPreferredSize(new Dimension(42, 42));
        musicBtn.setContentAreaFilled(false);
        musicBtn.setBorderPainted(false);
        musicBtn.setFocusPainted(false);
        musicBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean isMusicPlaying = AudioManager.isPlaying();
        if (isMusicPlaying) {
            musicBtn.setIcon(musicOnIcon);
            String dev = AudioManager.getActiveDeviceName();
            musicBtn.setToolTipText("Mute ambient sound (" + dev + ") • Right-click to switch device");
        } else {
            musicBtn.setIcon(musicCutIcon);
            musicBtn.setToolTipText("Play ambient journey sound • Right-click to switch device");
        }

        musicBtn.addActionListener(e -> {
            AudioManager.toggleAmbientSound(isPlaying -> {
                String dev = AudioManager.getActiveDeviceName();
                if (isPlaying) {
                    musicBtn.setIcon(musicOnIcon);
                    musicBtn.setToolTipText("Mute ambient sound (" + dev + ") • Right-click to switch device");
                } else {
                    musicBtn.setIcon(musicCutIcon);
                    musicBtn.setToolTipText("Play ambient journey sound • Right-click to switch device");
                }
                musicBtn.repaint();
            });
        });

        // Right-click context menu for audio output hardware
        musicBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    showAudioDevicePopup(musicBtn, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAudioDevicePopup(musicBtn, e.getX(), e.getY());
                }
            }
        });
        brandPanel.add(musicBtn);

        header.add(brandPanel, BorderLayout.WEST);

        // 2. Navigation Capsule (Center) - Floating Frosted Glass Pill with Real
        // Backdrop Blur
        JPanel navCenterWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        navCenterWrapper.setOpaque(false);

        JPanel navCapsule = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, 42);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth();
                int h = getHeight();

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Soft ambient drop shadow
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 2, w, h, h, h);

                // Decreased opacity translucent black glass fill (no border)
                g2.setColor(new Color(15, 23, 42, 60));
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        navCapsule.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        navCapsule.setOpaque(false);
        navCapsule.setBorder(new EmptyBorder(0, 0, 0, 8));

        // Active "Book Journey" Pill: Solid White Button with Dark Typography (42px
        // height, flush fit)
        JButton bookBtn = new JButton("Book Journey") {
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                // Solid White Pill Body
                g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Dark Typography
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                g2.setColor(new Color(15, 23, 42));
                FontMetrics fm = g2.getFontMetrics();
                String text = "Book Journey";
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        bookBtn.setPreferredSize(new Dimension(132, 42));
        bookBtn.setContentAreaFilled(false);
        bookBtn.setBorderPainted(false);
        bookBtn.setFocusPainted(false);
        bookBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        navCapsule.add(bookBtn);

        // "PNR Status" pill
        JButton pnrBtn = createTranslucentNavPill("PNR Status");
        pnrBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "PNR Status & Live Tracking\nEnter your 10-digit PNR to retrieve booking status.",
                "PNR Status", JOptionPane.INFORMATION_MESSAGE));
        navCapsule.add(pnrBtn);

        // "Train Schedule" pill
        JButton scheduleBtn = createTranslucentNavPill("Train Schedule");
        scheduleBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Train Rosters & Timetable\nBrowse route schedules and platform halts.",
                "Train Schedule", JOptionPane.INFORMATION_MESSAGE));
        navCapsule.add(scheduleBtn);

        navCenterWrapper.add(navCapsule);
        header.add(navCenterWrapper, BorderLayout.CENTER);

        // 3. Right Action Panel ("Plan My Trip" + "Login" Pills)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        // "Plan My Trip ↗" Pill Button (to the left of Login button)
        JButton planTripBtn = new JButton() {
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                // Solid White Pill Body
                g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Button Text: "Plan My Trip"
                String text = "Plan My Trip";
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                g2.setColor(new Color(15, 23, 42));
                FontMetrics fm = g2.getFontMetrics();
                int tx = 18;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                // Circular Brand-Orange Arrow Badge (#FA5909)
                int badgeSize = 30;
                int badgeX = w - badgeSize - 6;
                int badgeY = (h - badgeSize) / 2;
                g2.setColor(new Color(250, 89, 9));
                g2.fillOval(badgeX, badgeY, badgeSize, badgeSize);

                // White Diagonal Arrow ↗
                int cx = badgeX + badgeSize / 2;
                int cy = badgeY + badgeSize / 2;
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 4, cy + 4, cx + 4, cy - 4);
                g2.drawLine(cx, cy - 4, cx + 4, cy - 4);
                g2.drawLine(cx + 4, cy, cx + 4, cy - 4);

                g2.dispose();
            }
        };
        planTripBtn.setPreferredSize(new Dimension(160, 42));
        planTripBtn.setContentAreaFilled(false);
        planTripBtn.setBorderPainted(false);
        planTripBtn.setFocusPainted(false);
        planTripBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        planTripBtn.addActionListener(e -> {
            if (homeView != null && homeView.getSearchButton() != null) {
                homeView.getSearchButton().doClick();
            }
        });
        rightPanel.add(planTripBtn);

        // Dynamic Auth Action Pill Button (Guest: Login, Passenger: Profile, Admin:
        // Console)
        authBtn = new JButton("Login");
        authBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        authBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateAuthButton();

        authBtn.addActionListener(e -> handleAuthButtonClick(authBtn));
        AuthSession.getInstance()
                .addAuthStateListener((user, role) -> SwingUtilities.invokeLater(this::updateAuthButton));
        rightPanel.add(authBtn);

        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private void handleAuthButtonClick(Component invoker) {
        AuthSession session = AuthSession.getInstance();
        if (session.isGuest()) {
            AuthDialog dialog = new AuthDialog(this, user -> updateAuthButton());
            dialog.setVisible(true);
        } else if (session.isAdmin()) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem launchCmd = new JMenuItem("⚡ Station Master Console");
            launchCmd.addActionListener(ev -> {
                AdminDashboardFrame admin = new AdminDashboardFrame();
                admin.setVisible(true);
            });
            menu.add(launchCmd);
            menu.addSeparator();
            JMenuItem signOut = new JMenuItem("Sign Out");
            signOut.addActionListener(ev -> session.logout());
            menu.add(signOut);
            menu.show(invoker, 0, invoker.getHeight() + 4);
        } else {
            User user = session.getCurrentUser();
            JPopupMenu menu = new JPopupMenu();
            JLabel header = new JLabel("Signed in as @" + (user != null ? user.getUsername() : ""));
            header.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
            header.setBorder(new EmptyBorder(4, 12, 4, 12));
            menu.add(header);
            menu.addSeparator();
            JMenuItem bookingsItem = new JMenuItem("My Bookings");
            bookingsItem.addActionListener(ev -> JOptionPane.showMessageDialog(this,
                    "No bookings recorded yet.", "My Bookings", JOptionPane.INFORMATION_MESSAGE));
            menu.add(bookingsItem);
            JMenuItem travelersItem = new JMenuItem("Saved Passengers");
            travelersItem.addActionListener(ev -> JOptionPane.showMessageDialog(this,
                    "0 saved passengers.", "Saved Passengers", JOptionPane.INFORMATION_MESSAGE));
            menu.add(travelersItem);
            menu.addSeparator();
            JMenuItem signOut = new JMenuItem("Sign Out");
            signOut.addActionListener(ev -> session.logout());
            menu.add(signOut);
            menu.show(invoker, 0, invoker.getHeight() + 4);
        }
    }

    private void updateAuthButton() {
        if (authBtn == null)
            return;
        AuthSession session = AuthSession.getInstance();
        User user = session.getCurrentUser();

        if (user == null || session.isGuest()) {
            authBtn.setText("Login");
            authBtn.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #FFFFFF; foreground: #0F172A; hoverBackground: #F1F5F9; borderWidth: 0; margin: 6,14,6,14;");
            authBtn.setPreferredSize(new Dimension(72, 42));
        } else if (session.isAdmin()) {
            authBtn.setText("⚡ Admin Portal ▾");
            authBtn.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #0284C7; foreground: #FFFFFF; hoverBackground: #0369A1; borderWidth: 0; margin: 6,14,6,14;");
            Dimension pref = authBtn.getPreferredSize();
            authBtn.setPreferredSize(new Dimension(Math.max(pref.width + 16, 140), 42));
        } else {
            String name = user.getFullName();
            if (name.length() > 14) {
                name = name.substring(0, 12) + "..";
            }
            authBtn.setText("👤 " + name + " ▾");
            authBtn.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #FFFFFF; foreground: #0F172A; hoverBackground: #F1F5F9; borderWidth: 0; margin: 6,14,6,14;");
            Dimension pref = authBtn.getPreferredSize();
            authBtn.setPreferredSize(new Dimension(Math.max(pref.width + 16, 120), 42));
        }
        if (authBtn.getParent() != null) {
            authBtn.getParent().revalidate();
            authBtn.getParent().repaint();
        }
    }

    private JButton createTranslucentNavPill(String title) {
        JButton btn = new JButton(title);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;" +
                        "background: #00000000;" +
                        "foreground: #FFFFFF;" +
                        "hoverBackground: #FFFFFF24;" +
                        "borderWidth: 0;" +
                        "margin: 0,16,0,16;");
        Dimension pref = btn.getPreferredSize();
        btn.setPreferredSize(new Dimension(pref.width, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showDestinationsMenu(Component invoker) {
        JPopupMenu popup = new JPopupMenu();

        JLabel header = new JLabel("Popular Rail Expeditions");
        header.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        header.setBorder(new EmptyBorder(6, 12, 4, 12));
        popup.add(header);
        popup.addSeparator();

        String[] destinations = {
                "Swiss Alps Panoramic Express",
                "Himalayan Toy Train (Kalka - Shimla)",
                "Vande Bharat Express (Delhi - Varanasi)",
                "Kashmir Valley Snow Rail (Banihal - Baramulla)",
                "Palace on Wheels Heritage Tour"
        };

        for (String dest : destinations) {
            JMenuItem item = new JMenuItem(dest);
            item.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
            item.addActionListener(e -> {
                JOptionPane.showMessageDialog(this,
                        "Destination Selected: " + dest + "\nFind available departures below.",
                        "Rail Destination", JOptionPane.INFORMATION_MESSAGE);
            });
            popup.add(item);
        }

        popup.show(invoker, 0, invoker.getHeight() + 6);
    }

    private void showPackagesMenu(Component invoker) {
        JPopupMenu popup = new JPopupMenu();

        JLabel header = new JLabel("Curated Travel Packages");
        header.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        header.setBorder(new EmptyBorder(6, 12, 4, 12));
        popup.add(header);
        popup.addSeparator();

        String[] packages = {
                "Weekend Alpine Explorer (3 Days / 2 Nights)",
                "Golden Triangle Heritage Circuit (5 Days / 4 Nights)",
                "Kashmir Valley Snow Safari (4 Days / 3 Nights)",
                "Coastal Konkan Rail Journey (3 Days / 2 Nights)"
        };

        for (String pkg : packages) {
            JMenuItem item = new JMenuItem(pkg);
            item.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
            item.addActionListener(e -> {
                JOptionPane.showMessageDialog(this,
                        "Selected Package: " + pkg + "\nViewing itinerary & reservation details.",
                        "Travel Packages", JOptionPane.INFORMATION_MESSAGE);
            });
            popup.add(item);
        }

        popup.show(invoker, 0, invoker.getHeight() + 6);
    }

    private void showAudioDevicePopup(Component invoker, int x, int y) {
        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel("Audio Output Source");
        title.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
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
            item.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
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
