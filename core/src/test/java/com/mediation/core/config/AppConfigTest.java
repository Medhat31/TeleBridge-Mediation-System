package com.mediation.core.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void testEmergencyNumbersLoadCorrectly() {
        Set<String> numbers = AppConfig.getEmergencyNumbers();

        assertNotNull(numbers);
        assertFalse(numbers.isEmpty());
        assertTrue(numbers.contains("112"));
        assertTrue(numbers.contains("122"));
        assertTrue(numbers.contains("123"));
        assertTrue(numbers.contains("180"));
    }

    @Test
    void testPollIntervalHasValidDefault() {
        int interval = AppConfig.getPollIntervalSeconds();
        assertTrue(interval > 0);
        assertTrue(interval <= 300); // Reasonable upper bound
    }

    @Test
    void testDockerNetworkHasDefault() {
        String network = AppConfig.getDockerNetwork();
        assertNotNull(network);
        assertFalse(network.isEmpty());
    }

    @Test
    void testSshStrictHostKeyCheckingHasDefault() {
        String value = AppConfig.getSshStrictHostKeyChecking();
        assertNotNull(value);
        assertTrue(value.equals("no") || value.equals("yes"));
    }

    @Test
    void testProjectRootIsNotNull() {
        String root = AppConfig.getProjectRoot();
        assertNotNull(root);
        assertFalse(root.isEmpty());
    }

    @Test
    void testWorkspaceDirectoriesAreNotNull() {
        assertNotNull(AppConfig.getInputDir());
        assertNotNull(AppConfig.getOutputDir());
        assertNotNull(AppConfig.getArchiveDir());
        assertNotNull(AppConfig.getNodeVolumesDir());
    }
}
