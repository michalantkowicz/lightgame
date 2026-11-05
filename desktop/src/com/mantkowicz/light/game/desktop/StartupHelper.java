package com.mantkowicz.light.game.desktop;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * On macOS, GLFW must run on the JVM's first thread ({@code -XstartOnFirstThread}).
 * When the game is started without that flag (e.g. {@code java -jar}), this relaunches
 * the JVM with it.
 */
final class StartupHelper {
    private static final String RESTARTED_FLAG = "lightgame.restarted";

    private StartupHelper() {
    }

    /** @return true if a child JVM was started and the current one should exit. */
    static boolean startNewJvmIfRequired() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return false;
        }
        // Already on the first thread, or already relaunched once.
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + ProcessHandle.current().pid()))
                || System.getProperty(RESTARTED_FLAG) != null) {
            return false;
        }

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-XstartOnFirstThread");
        command.add("-D" + RESTARTED_FLAG + "=true");
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(DesktopLauncher.class.getName());

        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Could not restart the JVM with -XstartOnFirstThread: " + e);
            return false;
        }
        return true;
    }
}
