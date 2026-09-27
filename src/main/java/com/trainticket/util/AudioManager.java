package com.trainticket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * High-fidelity ambient audio manager utilizing Java Sound (javax.sound.sampled)
 * with native macOS CoreAudio output device tracking.
 *
 * Automatically detects and binds to the active OS default audio output device
 * (e.g., External Headphones, MacBook Air Speakers, External Displays) and
 * dynamically migrates playback when the user changes output settings in macOS.
 */
public final class AudioManager {

    private static final Logger logger = LoggerFactory.getLogger(AudioManager.class);
    private static final String AUDIO_RESOURCE_PATH = "/assets/audio/hero.wav";

    private static final Object LOCK = new Object();
    private static Clip activeClip;
    private static volatile boolean playing = false;
    private static volatile long playbackPositionUs = 0;
    private static volatile String activeDeviceName = "Default Audio Device";
    private static volatile String manualDeviceOverride = null; // null means Auto (follow OS)

    private static byte[] audioDataCache = null;
    private static ScheduledExecutorService deviceWatcherExecutor;
    private static volatile String cachedOSDefaultDevice = null;
    private static volatile long lastOSDeviceQueryTime = 0;

    // Python CoreAudio ctypes script for querying macOS kAudioHardwarePropertyDefaultOutputDevice
    private static final String MACOS_COREAUDIO_SCRIPT =
            "import ctypes, ctypes.util\n" +
            "try:\n" +
            "    ca = ctypes.cdll.LoadLibrary(ctypes.util.find_library('CoreAudio'))\n" +
            "    class Addr(ctypes.Structure):\n" +
            "        _fields_ = [('s', ctypes.c_uint32), ('c', ctypes.c_uint32), ('e', ctypes.c_uint32)]\n" +
            "    p = Addr(int.from_bytes(b'dOut', 'big'), int.from_bytes(b'glob', 'big'), 0)\n" +
            "    d = ctypes.c_uint32(); s = ctypes.c_uint32(4)\n" +
            "    ca.AudioObjectGetPropertyData(1, ctypes.byref(p), 0, None, ctypes.byref(s), ctypes.byref(d))\n" +
            "    p.s = int.from_bytes(b'lnam', 'big')\n" +
            "    ref = ctypes.c_void_p(); s.value = ctypes.sizeof(ref)\n" +
            "    ca.AudioObjectGetPropertyData(d, ctypes.byref(p), 0, None, ctypes.byref(s), ctypes.byref(ref))\n" +
            "    cf = ctypes.cdll.LoadLibrary(ctypes.util.find_library('CoreFoundation'))\n" +
            "    buf = ctypes.create_string_buffer(256)\n" +
            "    cf.CFStringGetCString(ref, buf, 256, 0x08000100)\n" +
            "    print(buf.value.decode('utf-8'))\n" +
            "except Exception:\n" +
            "    pass\n";

    private AudioManager() {}

    /**
     * Toggles ambient sound playback.
     *
     * @param onStateChanged callback executed on the Swing EDT with the new boolean playing state
     */
    public static void toggleAmbientSound(Consumer<Boolean> onStateChanged) {
        CompletableFuture.runAsync(() -> {
            boolean newState;
            synchronized (LOCK) {
                if (playing) {
                    stopInternal();
                    newState = false;
                } else {
                    startInternal();
                    newState = true;
                }
            }
            if (onStateChanged != null) {
                SwingUtilities.invokeLater(() -> onStateChanged.accept(newState));
            }
        });
    }

    /**
     * Starts or resumes playback on the OS-selected output device.
     */
    public static void play() {
        CompletableFuture.runAsync(() -> {
            synchronized (LOCK) {
                if (!playing) {
                    startInternal();
                }
            }
        });
    }

    /**
     * Pauses audio playback without resetting position (e.g., when window is minimized).
     */
    public static void pause() {
        synchronized (LOCK) {
            if (activeClip != null && playing) {
                playbackPositionUs = activeClip.getMicrosecondPosition();
                activeClip.stop();
                playing = false;
                logger.info("Ambient audio paused at {}ms", playbackPositionUs / 1000);
            }
        }
    }

    /**
     * Resumes audio playback when window is restored, maintaining position.
     */
    public static void resume() {
        synchronized (LOCK) {
            if (!playing && playbackPositionUs > 0) {
                startInternal();
            }
        }
    }

    /**
     * Shuts down the audio player and releases all system mixers.
     */
    public static void dispose() {
        synchronized (LOCK) {
            stopDeviceWatcher();
            if (activeClip != null) {
                try {
                    activeClip.stop();
                    activeClip.close();
                } catch (Exception ignored) {}
                activeClip = null;
            }
            playing = false;
            playbackPositionUs = 0;
        }
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static String getActiveDeviceName() {
        return activeDeviceName;
    }

    /**
     * Sets a manual output device override, or passes null to auto-follow macOS default.
     */
    public static void setSelectedOutputDevice(String deviceName) {
        CompletableFuture.runAsync(() -> {
            synchronized (LOCK) {
                manualDeviceOverride = "Auto".equalsIgnoreCase(deviceName) ? null : deviceName;
                logger.info("Audio output device set to: {}", manualDeviceOverride != null ? manualDeviceOverride : "Auto (System Default)");
                if (playing) {
                    // Re-open on the newly selected device
                    migrateToDevice(resolveTargetDeviceName());
                }
            }
        });
    }

    public static String getSelectedOutputDevice() {
        return manualDeviceOverride != null ? manualDeviceOverride : "Auto";
    }

    /**
     * Returns the list of active direct audio output devices discovered on the system.
     */
    public static List<String> getAvailableOutputDevices() {
        List<String> devices = new ArrayList<>();
        devices.add("Auto (System Default)");

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().startsWith("Port ")) continue;
            if ("Default Audio Device".equalsIgnoreCase(info.getName())) continue;

            try {
                Mixer mixer = AudioSystem.getMixer(info);
                for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                    if (SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass()) ||
                        Clip.class.isAssignableFrom(lineInfo.getLineClass())) {
                        if (!devices.contains(info.getName())) {
                            devices.add(info.getName());
                        }
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        return Collections.unmodifiableList(devices);
    }

    // --- Internal Implementation ---

    private static void startInternal() {
        try {
            ensureAudioDataLoaded();
            if (audioDataCache == null) {
                logger.warn("Could not load ambient audio asset: {}", AUDIO_RESOURCE_PATH);
                return;
            }

            String targetDevice = resolveTargetDeviceName();
            openAndPlayClip(targetDevice);
            startDeviceWatcher();
        } catch (Exception ex) {
            logger.error("Failed to start ambient audio: {}", ex.getMessage(), ex);
        }
    }

    private static void stopInternal() {
        stopDeviceWatcher();
        if (activeClip != null) {
            playbackPositionUs = activeClip.getMicrosecondPosition();
            try {
                activeClip.stop();
                activeClip.close();
            } catch (Exception ignored) {}
            activeClip = null;
        }
        playing = false;
        logger.info("Ambient audio stopped");
    }

    private static void openAndPlayClip(String targetDeviceName) {
        try {
            Mixer.Info mixerInfo = findMixerForDevice(targetDeviceName);
            Mixer mixer = (mixerInfo != null) ? AudioSystem.getMixer(mixerInfo) : AudioSystem.getMixer(null);
            String actualName = (mixerInfo != null) ? mixerInfo.getName() : "Default Audio Device";

            ByteArrayInputStream bais = new ByteArrayInputStream(audioDataCache);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bais);

            DataLine.Info clipInfo = new DataLine.Info(Clip.class, ais.getFormat());
            Clip clip = (Clip) mixer.getLine(clipInfo);
            clip.open(ais);

            // Set comfortable ambient background volume (-6.0 dB)
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-6.0f);
            }

            // Restore position if resuming or migrating
            if (playbackPositionUs > 0 && playbackPositionUs < clip.getMicrosecondLength()) {
                clip.setMicrosecondPosition(playbackPositionUs);
            }

            // Loop infinitely
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

            activeClip = clip;
            activeDeviceName = actualName;
            playing = true;
            logger.info("Ambient audio playing on [{}] (position: {}ms)", actualName, playbackPositionUs / 1000);
        } catch (Exception ex) {
            logger.error("Error opening audio clip on [{}]: {}", targetDeviceName, ex.getMessage(), ex);
        }
    }

    private static void migrateToDevice(String newDeviceName) {
        if (activeClip != null) {
            playbackPositionUs = activeClip.getMicrosecondPosition();
            try {
                activeClip.stop();
                activeClip.close();
            } catch (Exception ignored) {}
            activeClip = null;
        }
        openAndPlayClip(newDeviceName);
    }

    private static String resolveTargetDeviceName() {
        if (manualDeviceOverride != null && !manualDeviceOverride.isBlank()) {
            return manualDeviceOverride;
        }
        String osDefault = queryOSDefaultDevice();
        return (osDefault != null && !osDefault.isBlank()) ? osDefault : "Default Audio Device";
    }

    /**
     * Finds the corresponding direct hardware mixer for a friendly device name.
     */
    private static Mixer.Info findMixerForDevice(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) return null;

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().startsWith("Port ")) continue;
            if (info.getName().equalsIgnoreCase(deviceName) ||
                info.getName().toLowerCase().contains(deviceName.toLowerCase()) ||
                deviceName.toLowerCase().contains(info.getName().toLowerCase())) {
                try {
                    Mixer mixer = AudioSystem.getMixer(info);
                    for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                        if (SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass()) ||
                            Clip.class.isAssignableFrom(lineInfo.getLineClass())) {
                            return info;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Queries macOS CoreAudio for the current system default audio output device.
     */
    private static String queryOSDefaultDevice() {
        long now = System.currentTimeMillis();
        if (cachedOSDefaultDevice != null && (now - lastOSDeviceQueryTime) < 1500) {
            return cachedOSDefaultDevice;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            try {
                Process process = new ProcessBuilder("/usr/bin/python3", "-c", MACOS_COREAUDIO_SCRIPT)
                        .redirectErrorStream(true)
                        .start();
                boolean finished = process.waitFor(400, TimeUnit.MILLISECONDS);
                if (finished) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null && !line.isBlank()) {
                            cachedOSDefaultDevice = line.trim();
                            lastOSDeviceQueryTime = now;
                            return cachedOSDefaultDevice;
                        }
                    }
                } else {
                    process.destroyForcibly();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Periodically monitors macOS default audio device changes while playing,
     * seamlessly migrating the active stream if the user switches outputs in OS.
     */
    private static void startDeviceWatcher() {
        if (deviceWatcherExecutor == null || deviceWatcherExecutor.isShutdown()) {
            deviceWatcherExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RailFlow-AudioDeviceWatcher");
                t.setDaemon(true);
                return t;
            });
            deviceWatcherExecutor.scheduleWithFixedDelay(() -> {
                try {
                    if (!playing || manualDeviceOverride != null) return;

                    String currentOSDevice = queryOSDefaultDevice();
                    if (currentOSDevice != null && !currentOSDevice.equalsIgnoreCase(activeDeviceName)) {
                        logger.info("OS output device changed to [{}]; migrating ambient audio stream...", currentOSDevice);
                        synchronized (LOCK) {
                            if (playing && manualDeviceOverride == null) {
                                migrateToDevice(currentOSDevice);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.debug("Error during audio device watch: {}", ex.getMessage());
                }
            }, 2, 2, TimeUnit.SECONDS);
        }
    }

    private static void stopDeviceWatcher() {
        if (deviceWatcherExecutor != null) {
            deviceWatcherExecutor.shutdownNow();
            deviceWatcherExecutor = null;
        }
    }

    private static synchronized void ensureAudioDataLoaded() {
        if (audioDataCache != null) return;

        try (InputStream in = AudioManager.class.getResourceAsStream(AUDIO_RESOURCE_PATH)) {
            if (in == null) {
                logger.error("Audio asset not found on classpath: {}", AUDIO_RESOURCE_PATH);
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            audioDataCache = baos.toByteArray();
            logger.info("Loaded ambient audio into memory ({} KB)", audioDataCache.length / 1024);
        } catch (Exception ex) {
            logger.error("Failed to load ambient audio resource: {}", ex.getMessage(), ex);
        }
    }
}
