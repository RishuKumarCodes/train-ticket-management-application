package com.trainticket.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;
import com.trainticket.util.AudioManager;
import com.trainticket.view.component.VideoBackgroundPanel;
import com.trainticket.view.pages.HomeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import com.trainticket.controller.TrainSearchController;
import com.trainticket.model.AuthSession;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.FeaturedDestination;
import com.trainticket.view.dialog.AuthDialog;
import com.trainticket.view.dialog.PlanDestinationTripDialog;
import com.trainticket.view.dialog.PnrStatusDialog;
import com.trainticket.view.admin.AdminDashboardView;
import com.trainticket.view.pages.TrainsPageView;
import com.trainticket.view.pages.TrainSearchResultsView;
import com.trainticket.view.pages.MyBookingsPageView;
import com.trainticket.view.component.navigation.AppHeaderPanel;
import com.trainticket.view.pages.PlanMyTripView;

/**
 * Main application window for RailFlow.
 * Handles the top minimalist navigation bar, OS taskbar/dock icon integration,
 * background video rendering, and page view switching.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static final String APP_ICON_PATH = "/assets/icons/icon.png";

    private JPanel rootCardPanel;
    private JPanel contentContainer;
    private HomeView homeView;
    private TrainSearchResultsView routeSearchResultsView;
    private TrainsPageView trainsPageView;
    private MyBookingsPageView myBookingsPageView;
    private AdminDashboardView adminDashboardView;
    private PlanMyTripView planMyTripView;
    private VideoBackgroundPanel videoBackgroundPanel;
    private JPanel uiOverlayPanel;
    private AppHeaderPanel headerPanel;
    private boolean isHomeViewActive = true;
    private final TrainSearchController trainSearchController = new TrainSearchController();

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
                if (videoBackgroundPanel != null && isHomeViewActive) {
                    videoBackgroundPanel.pauseVideo();
                }
                AudioManager.pause();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                if (videoBackgroundPanel != null && isHomeViewActive) {
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
            wireSearchEvents(homeView);
            trainsPageView = new TrainsPageView(trainSearchController);
            myBookingsPageView = new MyBookingsPageView(this::openAuthDialog, this::showTrainsView);

            contentContainer.add(homeView, "HOME");
            contentContainer.add(trainsPageView, "TRAINS");
            contentContainer.add(myBookingsPageView, "MY_BOOKINGS");

            showHomeView();
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
        // Root card panel allows instant zero-lag switching between PASSENGER and ADMIN
        // full interfaces
        rootCardPanel = new JPanel(new CardLayout());
        rootCardPanel.setOpaque(true);

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
        uiOverlayPanel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            private static final int GRADIENT_HEIGHT = 240;
            private final float[] fractions = { 0.0f, 0.35f, 0.70f, 1.0f };
            private final Color[] colors = {
                    new Color(0, 0, 0, 95),
                    new Color(0, 0, 0, 50),
                    new Color(0, 0, 0, 15),
                    new Color(0, 0, 0, 0)
            };
            private final LinearGradientPaint scrimGradient = new LinearGradientPaint(0, 0, 0, GRADIENT_HEIGHT, fractions, colors);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isHomeViewActive) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    int w = getWidth();
                    g2.setPaint(scrimGradient);
                    g2.fillRect(0, 0, w, GRADIENT_HEIGHT);
                    g2.dispose();
                } else {
                    g.setColor(new Color(238, 242, 246));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        uiOverlayPanel.setOpaque(false);

        // Top Minimalist Header (Tabs: Book Journey, Trains, My Bookings)
        headerPanel = new AppHeaderPanel(
                this::showHomeView,
                this::showTrainsView,
                this::showMyBookingsView,
                this::showPlanMyTripView,
                this::openAuthDialog,
                this::switchToAdminDashboard,
                this::showHomeView);
        uiOverlayPanel.add(headerPanel, BorderLayout.NORTH);

        // Main Content View Container
        contentContainer = new JPanel(new CardLayout());
        contentContainer.setOpaque(false);

        homeView = new HomeView();
        wireSearchEvents(homeView);
        if (homeView.getDestinationsSection() != null) {
            homeView.getDestinationsSection().setDestinationSelectListener(this::openDestinationTripDialog);
        }

        routeSearchResultsView = new TrainSearchResultsView(null, List.of(), this::showHomeView, this::handleTrainSearch);
        trainsPageView = new TrainsPageView(trainSearchController);
        myBookingsPageView = new MyBookingsPageView(this::openAuthDialog, this::showTrainsView);

        contentContainer.add(homeView, "HOME");
        contentContainer.add(trainsPageView, "TRAINS");
        contentContainer.add(myBookingsPageView, "MY_BOOKINGS");

        uiOverlayPanel.add(contentContainer, BorderLayout.CENTER);
        layeredPane.add(uiOverlayPanel, JLayeredPane.PALETTE_LAYER);

        rootCardPanel.add(layeredPane, "PASSENGER");

        // Dedicated Full-Page Admin Dashboard (Station Master Console)
        adminDashboardView = new AdminDashboardView(this::switchToPassengerView);
        rootCardPanel.add(adminDashboardView, "ADMIN");

        // Dedicated Full-Page Plan My Trip view (own header, no video)
        planMyTripView = new PlanMyTripView(this::showHomeView, this::openDestinationTripDialog);
        rootCardPanel.add(planMyTripView, "PLAN_MY_TRIP");

        // Dedicated Full-Page Route Search Results view (own custom header, no video)
        rootCardPanel.add(routeSearchResultsView, "ROUTE_SEARCH");

        setContentPane(rootCardPanel);
    }

    public void openDestinationTripDialog(FeaturedDestination destination) {
        if (destination == null) return;
        PlanDestinationTripDialog dialog = new PlanDestinationTripDialog(this, destination, this::handleTrainSearch);
        dialog.setVisible(true);
    }

    public void openPnrStatusDialog() {
        openPnrStatusDialog(null);
    }

    public void openPnrStatusDialog(String initialPnr) {
        PnrStatusDialog dialog = new PnrStatusDialog(this, initialPnr);
        dialog.setVisible(true);
    }

    public void openLiveTrackerDialog() {
        openLiveTrackerDialog(null);
    }

    public void openLiveTrackerDialog(String initialTrainNumber) {
        com.trainticket.view.dialog.LiveTrainTrackerDialog dialog = 
                new com.trainticket.view.dialog.LiveTrainTrackerDialog(this, initialTrainNumber);
        dialog.setVisible(true);
    }

    public void openAuthDialog() {
        AuthDialog dialog = new AuthDialog(this, user -> {
            updateAuthButton();
            AuthSession session = AuthSession.getInstance();
            if (session.isAdmin()) {
                switchToAdminDashboard();
            } else {
                if (myBookingsPageView != null) {
                    myBookingsPageView.refreshView();
                }
            }
        });
        dialog.setVisible(true);
    }

    private void wireSearchEvents(HomeView view) {
        if (view != null && view.getHeroSection() != null && view.getHeroSection().getSearchCapsule() != null) {
            view.getHeroSection().getSearchCapsule().addSearchListener(this::handleTrainSearch);
        }
    }

    private void handleTrainSearch(TrainSearchQuery query) {
        logger.info("Executing train search: {} -> {} on {}",
                query.getFromStationCode(), query.getToStationCode(), query.getJourneyDate());

        trainSearchController.searchTrains(query, results -> {
            showSearchResults(query, results);
        }, errorMsg -> {
            logger.warn("Train search note/warning: {}", errorMsg);
            showSearchResults(query, List.of());
        });
    }

    private void showSearchResults(TrainSearchQuery query, List<TrainSearchResult> results) {
        SwingUtilities.invokeLater(() -> {
            if (routeSearchResultsView != null) {
                routeSearchResultsView.setSearchQueryAndResults(query, results);
            }
            showRouteSearchResults();
        });
    }

    public void showRouteSearchResults() {
        SwingUtilities.invokeLater(() -> {
            isHomeViewActive = false;
            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.pauseVideo();
                videoBackgroundPanel.setVisible(false);
            }
            AudioManager.pause();
            CardLayout rootCl = (CardLayout) rootCardPanel.getLayout();
            rootCl.show(rootCardPanel, "ROUTE_SEARCH");
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void showHomeView() {
        SwingUtilities.invokeLater(() -> {
            // Always ensure rootCardPanel is showing the PASSENGER layer first.
            // This is what makes "back" from PlanMyTripView (and any future top-level
            // overlay cards) work correctly.
            CardLayout rootCl = (CardLayout) rootCardPanel.getLayout();
            rootCl.show(rootCardPanel, "PASSENGER");

            setActiveTab(0);
            isHomeViewActive = true;
            CardLayout cl = (CardLayout) contentContainer.getLayout();
            cl.show(contentContainer, "HOME");

            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.setVisible(true);
                videoBackgroundPanel.resumeVideo();
            }
            if (uiOverlayPanel != null) {
                uiOverlayPanel.setOpaque(false);
                uiOverlayPanel.repaint();
            }
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void showTrainsView() {
        SwingUtilities.invokeLater(() -> {
            CardLayout rootCl = (CardLayout) rootCardPanel.getLayout();
            rootCl.show(rootCardPanel, "PASSENGER");

            setActiveTab(1);
            isHomeViewActive = false;
            CardLayout cl = (CardLayout) contentContainer.getLayout();
            cl.show(contentContainer, "TRAINS");

            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.pauseVideo();
                videoBackgroundPanel.setVisible(false);
            }
            if (uiOverlayPanel != null) {
                uiOverlayPanel.setOpaque(true);
                uiOverlayPanel.setBackground(new Color(238, 242, 246));
                uiOverlayPanel.repaint();
            }
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void showMyBookingsView() {
        SwingUtilities.invokeLater(() -> {
            setActiveTab(2);
            isHomeViewActive = false;
            if (myBookingsPageView != null) {
                myBookingsPageView.refreshView();
            }
            CardLayout cl = (CardLayout) contentContainer.getLayout();
            cl.show(contentContainer, "MY_BOOKINGS");

            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.pauseVideo();
                videoBackgroundPanel.setVisible(false);
            }
            if (uiOverlayPanel != null) {
                uiOverlayPanel.setOpaque(true);
                uiOverlayPanel.setBackground(new Color(238, 242, 246));
                uiOverlayPanel.repaint();
            }
        });
    }

    public void switchToAdminDashboard() {
        SwingUtilities.invokeLater(() -> {
            logger.info("Switching main application window to full-page Admin Command Center.");
            isHomeViewActive = false;
            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.pauseVideo();
                videoBackgroundPanel.setVisible(false);
            }
            AudioManager.pause();
            CardLayout cl = (CardLayout) rootCardPanel.getLayout();
            cl.show(rootCardPanel, "ADMIN");
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void showPlanMyTripView() {
        SwingUtilities.invokeLater(() -> {
            logger.info("Switching to Plan My Trip page.");
            isHomeViewActive = false;
            if (videoBackgroundPanel != null) {
                videoBackgroundPanel.pauseVideo();
                videoBackgroundPanel.setVisible(false);
            }
            // Stop music and hide music button
            if (headerPanel != null) {
                headerPanel.stopMusicIfPlaying();
            }
            CardLayout cl = (CardLayout) rootCardPanel.getLayout();
            cl.show(rootCardPanel, "PLAN_MY_TRIP");
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void switchToPassengerView() {
        SwingUtilities.invokeLater(() -> {
            logger.info("Returning main application window to Passenger view.");
            CardLayout cl = (CardLayout) rootCardPanel.getLayout();
            cl.show(rootCardPanel, "PASSENGER");
            showHomeView();
            updateAuthButton();
            rootCardPanel.revalidate();
            rootCardPanel.repaint();
        });
    }

    public void setActiveTab(int tabIndex) {
        if (headerPanel != null) {
            headerPanel.setActiveTab(tabIndex);
        }
    }

    public void updateAuthButton() {
        if (headerPanel != null) {
            headerPanel.updateAuthButton();
        }
    }
}
