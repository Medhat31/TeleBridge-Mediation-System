package com.mediation.core.config;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Centralized application configuration.
 * Loads defaults from app.properties on the classpath, with environment variable overrides.
 * Environment variable names are derived from property keys: dots become underscores, uppercased.
 * Example: "engine.poll.interval.seconds" -> ENV "ENGINE_POLL_INTERVAL_SECONDS"
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("[AppConfig] Warning: Could not load app.properties, using defaults.");
        }
    }

    /**
     * Retrieves a config value. Priority: Environment Variable > app.properties > defaultValue.
     */
    public static String get(String key, String defaultValue) {
        // Convert "workspace.root" -> "WORKSPACE_ROOT"
        String envKey = key.replace(".", "_").toUpperCase();
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) {
            return envVal;
        }
        return props.getProperty(key, defaultValue);
    }

    // --- Workspace Paths ---

    /**
     * The root directory of the entire project.
     * In Docker: set via WORKSPACE_ROOT=/app
     * Locally: defaults to parent of user.dir (assumes running from core/)
     */
    public static String getProjectRoot() {
        return get("workspace.root", new File(System.getProperty("user.dir")).getParent());
    }

    /**
     * The host machine's project root, used for Docker volume mounts.
     * Only set when running inside a Docker container.
     */
    public static String getHostProjectRoot() {
        String hostRoot = System.getenv("HOST_PROJECT_ROOT");
        if (hostRoot != null && !hostRoot.isEmpty()) {
            return hostRoot;
        }
        // Fallback to project root (when running locally, they're the same)
        return getProjectRoot();
    }

    public static String getInputDir() {
        return getProjectRoot() + File.separator + "engine_workspace" + File.separator + "input";
    }

    public static String getOutputDir() {
        return getProjectRoot() + File.separator + "engine_workspace" + File.separator + "output";
    }

    public static String getArchiveDir() {
        return getProjectRoot() + File.separator + "engine_workspace" + File.separator + "archive";
    }

    public static String getNodeVolumesDir() {
        return getProjectRoot() + File.separator + "node_volumes";
    }

    // --- Engine Settings ---

    public static int getPollIntervalSeconds() {
        return Integer.parseInt(get("engine.poll.interval.seconds", "15"));
    }

    // --- Mediation Rules ---

    public static Set<String> getEmergencyNumbers() {
        String numbers = get("mediation.emergency.numbers", "112,122,123,180");
        return new HashSet<>(Arrays.asList(numbers.split(",")));
    }

    // --- Docker ---

    public static String getDockerNetwork() {
        return get("docker.network", "telebridge-mediation-system_telecom-net");
    }

    // --- Security ---

    public static String getSshStrictHostKeyChecking() {
        return get("security.ssh.strict.host.checking", "no");
    }
}
