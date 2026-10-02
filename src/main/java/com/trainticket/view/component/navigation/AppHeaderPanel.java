package com.trainticket.view.component.navigation;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;
import com.trainticket.util.AudioManager;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Modern floating frosted glass top navigation header for RailFlow.
 * Encapsulates:
 * <ul>
 *   <li>Brand Logo (Left) with home view trigger</li>
 *   <li>Ambient Music Toggle Pill (Left) with macOS CoreAudio output source picker</li>
 *   <li>3-Tab Passenger Navigation Capsule (Center: Book Journey, Trains, My Bookings)</li>
 *   <li>"Plan My Trip ↗" Action Button (Right)</li>
 *   <li>Dynamic User Authentication / Profile Pill Button with popup menu (Right)</li>
 * </ul>
 */
public class AppHeaderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String APP_WIDE_ICON_PATH = "assets/icons/icon-wide-white.png";

    private final Runnable onHomeAction;
    private final Runnable onTrainsAction;
    private final Runnable onMyBookingsAction;
    private final Runnable onPlanTripAction;
    private final Runnable onOpenAuthAction;
    private final Runnable onSwitchToAdminAction;
    private final Runnable onSignOutAction;

    private int activeTabIndex = 0; // 0 = Book Journey, 1 = Trains, 2 = My Bookings
    private JButton bookJourneyBtn;
    private JButton trainsBtn;
    private JButton myBookingsBtn;
    private JButton authBtn;
    private JButton musicBtn; // kept as field so we can show/hide per tab
    private JPanel navCapsule;  // kept as field so we can repaint on tab switch
    private Image whiteLogo;
    private Image darkLogo;
    private JComponent logoComp;

    public AppHeaderPanel(Runnable onHomeAction,
                          Runnable onTrainsAction,
                          Runnable onMyBookingsAction,
                          Runnable onPlanTripAction,
                          Runnable onOpenAuthAction,
                          Runnable onSwitchToAdminAction,
                          Runnable onSignOutAction) {
        this.onHomeAction = onHomeAction;
        this.onTrainsAction = onTrainsAction;
        this.onMyBookingsAction = onMyBookingsAction;
        this.onPlanTripAction = onPlanTripAction;
        this.onOpenAuthAction = onOpenAuthAction;
        this.onSwitchToAdminAction = onSwitchToAdminAction;
        this.onSignOutAction = onSignOutAction;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(36, 48, 16, 48));
        putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true);

        initComponents();

        AuthSession.getInstance().addAuthStateListener((user, role) -> 
                SwingUtilities.invokeLater(this::updateAuthButton));
    }

    /** Compatibility constructor for callers passing legacy PNR status callback. */
    public AppHeaderPanel(Runnable onHomeAction,
                          Runnable onTrainsAction,
                          Runnable onPnrStatusAction,
                          Runnable onMyBookingsAction,
                          Runnable onPlanTripAction,
                          Runnable onOpenAuthAction,
                          Runnable onSwitchToAdminAction,
                          Runnable onSignOutAction) {
        this(onHomeAction, onTrainsAction, onMyBookingsAction, onPlanTripAction,
                onOpenAuthAction, onSwitchToAdminAction, onSignOutAction);
    }

    private void initComponents() {
        // 1. Brand Logo Left (icon-wide-white.png / icon-wide.png) + Circular White Music Toggle Button
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        brandPanel.setOpaque(false);

        whiteLogo = AssetManager.getImage("/assets/icons/icon-wide-white.png");
        darkLogo = AssetManager.getImage("/assets/icons/icon-wide.png");
        if (darkLogo == null) {
            darkLogo = AssetManager.getImage("/assets/icons/icon-wide-black.png");
        }

        if (whiteLogo != null || darkLogo != null) {
            int logoHeight = 36;
            int logoWidth = logoHeight * 5;
            logoComp = new JComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    Image imgToDraw = (activeTabIndex == 0)
                            ? (whiteLogo != null ? whiteLogo : darkLogo)
                            : (darkLogo != null ? darkLogo : whiteLogo);

                    if (imgToDraw != null) {
                        g2.drawImage(imgToDraw, 0, 0, getWidth(), getHeight(), null);
                    }
                    g2.dispose();
                }
            };
            logoComp.setPreferredSize(new Dimension(logoWidth, logoHeight));
            logoComp.setMinimumSize(new Dimension(logoWidth, logoHeight));
            logoComp.setMaximumSize(new Dimension(logoWidth, logoHeight));
            logoComp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            logoComp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onHomeAction != null) onHomeAction.run();
                }
            });
            brandPanel.add(logoComp);
        } else {
            JLabel brandTitle = new JLabel("RAILFLOW");
            brandTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 20f));
            brandTitle.setForeground(Color.WHITE);
            brandTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            brandTitle.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onHomeAction != null) onHomeAction.run();
                }
            });
            brandPanel.add(brandTitle);
        }

        // Ambient Audio Toggle Button with White Circular Background & Music Note Icons
        FlatSVGIcon musicOnIcon = new FlatSVGIcon("assets/icons/music.svg", 20, 20);
        FlatSVGIcon musicCutIcon = new FlatSVGIcon("assets/icons/music-cut.svg", 20, 20);

        musicBtn = new JButton(musicCutIcon) {
            private static final long serialVersionUID = 1L;

            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                    @Override
                    public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                    @Override
                    public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
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
                    g2.setColor(new Color(0, 0, 0, 22));
                    g2.fillOval(1, 2, w - 2, h - 2);

                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillOval(0, 0, w, h);
                } else {
                    g2.setColor(new Color(0, 0, 0, 45));
                    g2.fillOval(0, 2, w, h);

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
        add(brandPanel, BorderLayout.WEST);

        // 2. Navigation Capsule (Center) - 3 Dedicated Tabs
        JPanel navCenterWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        navCenterWrapper.setOpaque(false);

        navCapsule = new JPanel() {
            private static final long serialVersionUID = 1L;

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

                boolean onHome = (activeTabIndex == 0);
                if (onHome) {
                    // Dark smoked glass capsule on video background
                    g2.setColor(new Color(0, 0, 0, 45));
                    g2.fillRoundRect(0, 2, w, h, h, h);
                    g2.setColor(new Color(15, 23, 42, 60));
                    g2.fillRoundRect(0, 0, w, h, h, h);
                } else {
                    // Frosted white capsule on light #F8FAFC canvas
                    g2.setColor(new Color(0, 0, 0, 14));
                    g2.fillRoundRect(0, 2, w, h, h, h);
                    g2.setColor(new Color(255, 255, 255, 220));
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    // Hairline border
                    g2.setColor(new Color(226, 232, 240, 200));
                    g2.setStroke(new java.awt.BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        navCapsule.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        navCapsule.setOpaque(false);
        navCapsule.setBorder(new EmptyBorder(0, 0, 0, 8));

        bookJourneyBtn = createNavTabButton("Book Journey", 0, onHomeAction);
        trainsBtn = createNavTabButton("Trains", 1, onTrainsAction);
        myBookingsBtn = createNavTabButton("My Bookings", 2, onMyBookingsAction);

        navCapsule.add(bookJourneyBtn);
        navCapsule.add(trainsBtn);
        navCapsule.add(myBookingsBtn);

        navCenterWrapper.add(navCapsule);
        add(navCenterWrapper, BorderLayout.CENTER);

        // 3. Right Action Panel ("Plan My Trip" + "Login" Pills)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        // "Plan My Trip ↗" Pill Button
        JButton planTripBtn = new JButton() {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                String text = "Plan My Trip";
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                g2.setColor(new Color(15, 23, 42));
                FontMetrics fm = g2.getFontMetrics();
                int tx = 18;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                int badgeSize = 30;
                int badgeX = w - badgeSize - 6;
                int badgeY = (h - badgeSize) / 2;
                g2.setColor(new Color(250, 89, 9));
                g2.fillOval(badgeX, badgeY, badgeSize, badgeSize);

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
            if (onPlanTripAction != null) onPlanTripAction.run();
        });
        rightPanel.add(planTripBtn);

        // Dynamic Auth Action Pill Button
        authBtn = new JButton("Login");
        authBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        authBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateAuthButton();

        authBtn.addActionListener(e -> handleAuthButtonClick(authBtn));
        rightPanel.add(authBtn);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setActiveTab(int tabIndex) {
        boolean wasHome = (this.activeTabIndex == 0);
        this.activeTabIndex = tabIndex;
        boolean isHome = (tabIndex == 0);

        // Show / hide music button based on whether home is active
        if (musicBtn != null) {
            musicBtn.setVisible(isHome);
            // Stop ambient audio when navigating away from home
            if (wasHome && !isHome && AudioManager.isPlaying()) {
                AudioManager.pause();
            }
        }

        if (bookJourneyBtn != null) bookJourneyBtn.repaint();
        if (trainsBtn != null) trainsBtn.repaint();
        if (myBookingsBtn != null) myBookingsBtn.repaint();
        if (navCapsule != null) navCapsule.repaint();
        if (logoComp != null) logoComp.repaint();
    }

    /** Stops ambient audio programmatically (called by MainFrame on view switch). */
    public void stopMusicIfPlaying() {
        if (AudioManager.isPlaying()) {
            AudioManager.pause();
        }
        if (musicBtn != null) {
            musicBtn.setVisible(false);
        }
    }

    public void updateAuthButton() {
        if (authBtn == null) return;
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

    private void handleAuthButtonClick(Component invoker) {
        AuthSession session = AuthSession.getInstance();
        if (session.isGuest()) {
            if (onOpenAuthAction != null) onOpenAuthAction.run();
        } else if (session.isAdmin()) {
            if (onSwitchToAdminAction != null) onSwitchToAdminAction.run();
        } else {
            User user = session.getCurrentUser();
            JPopupMenu menu = new JPopupMenu();
            JLabel header = new JLabel("Signed in as @" + (user != null ? user.getUsername() : ""));
            header.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
            header.setBorder(new EmptyBorder(4, 12, 4, 12));
            menu.add(header);
            menu.addSeparator();

            JMenuItem bookingsItem = new JMenuItem("My Bookings");
            bookingsItem.addActionListener(ev -> {
                if (onMyBookingsAction != null) onMyBookingsAction.run();
            });
            menu.add(bookingsItem);
            menu.addSeparator();

            JMenuItem signOut = new JMenuItem("Sign Out");
            signOut.addActionListener(ev -> {
                session.logout();
                if (onSignOutAction != null) onSignOutAction.run();
                updateAuthButton();
            });
            menu.add(signOut);
            menu.show(invoker, 0, invoker.getHeight() + 4);
        }
    }

    private JButton createNavTabButton(String title, int tabIndex, Runnable onClick) {
        JButton btn = new JButton(title) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                boolean isActive = (activeTabIndex == tabIndex);
                // On home (video bg) active pill is white; on light pages active pill is black
                boolean onHomePage = (activeTabIndex == 0);

                if (isActive) {
                    if (onHomePage) {
                        // White pill on dark video background
                        g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                        g2.fillRoundRect(0, 0, w, h, h, h);
                        g2.setColor(new Color(15, 23, 42)); // text: Deep Slate
                    } else {
                        // Black pill on light #F8FAFC canvas
                        g2.setColor(isHovered ? new Color(30, 41, 59) : new Color(15, 23, 42));
                        g2.fillRoundRect(0, 0, w, h, h, h);
                        g2.setColor(Color.WHITE); // text: white on black pill
                    }
                } else {
                    // Inactive tab: text color adapts to background
                    if (isHovered) {
                        Color hoverFill = onHomePage
                                ? new Color(255, 255, 255, 35)
                                : new Color(15, 23, 42, 18);
                        g2.setColor(hoverFill);
                        g2.fillRoundRect(0, 0, w, h, h, h);
                    }
                    g2.setColor(onHomePage ? Color.WHITE : new Color(100, 116, 139));
                }

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(115, 42));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (tabIndex >= 0) {
                setActiveTab(tabIndex);
            }
            if (onClick != null) onClick.run();
        });
        return btn;
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
