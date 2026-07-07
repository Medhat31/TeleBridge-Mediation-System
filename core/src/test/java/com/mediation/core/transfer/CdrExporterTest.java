package com.mediation.core.transfer;

import com.mediation.core.entities.Cdr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CdrExporterTest {

    private CdrExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CdrExporter();
    }

    @Test
    void testExportCreatesValidCsvWithHeaders(@TempDir Path tempDir) throws Exception {
        List<Cdr> cdrs = List.of(
                createCdr(0, 0, "20101111111", "20112222222", "20260706120000", 300, 16),
                createCdr(1, 1, "20103333333", "20114444444", "20260706120001", 1, 16)
        );

        File result = exporter.exportToCsv(cdrs, tempDir.toString(), "test_file", "Billing_Sys_01");

        assertTrue(result.exists());
        assertTrue(result.getName().startsWith("FORWARDED_Billing_Sys_01_test_file"));
        assertTrue(result.getName().endsWith(".csv"));

        List<String> lines = Files.readAllLines(result.toPath());
        assertEquals(3, lines.size()); // header + 2 data rows

        // Verify header
        assertEquals("Type,Direction,A_Party,B_Party,Time,Duration,Cause", lines.get(0));
    }

    @Test
    void testExportMapsVoiceTypeCorrectly(@TempDir Path tempDir) throws Exception {
        List<Cdr> cdrs = List.of(
                createCdr(0, 0, "20101111111", "20112222222", "20260706120000", 300, 16) // VOICE, MO
        );

        File result = exporter.exportToCsv(cdrs, tempDir.toString(), "voice_test", "Dest");
        List<String> lines = Files.readAllLines(result.toPath());

        assertTrue(lines.get(1).startsWith("VOICE,MO,"));
    }

    @Test
    void testExportMapsSmsTypeCorrectly(@TempDir Path tempDir) throws Exception {
        List<Cdr> cdrs = List.of(
                createCdr(1, 1, "20101111111", "20112222222", "20260706120000", 1, 16) // SMS, MT
        );

        File result = exporter.exportToCsv(cdrs, tempDir.toString(), "sms_test", "Dest");
        List<String> lines = Files.readAllLines(result.toPath());

        assertTrue(lines.get(1).startsWith("SMS,MT,"));
    }

    @Test
    void testExportEmptyListProducesHeaderOnly(@TempDir Path tempDir) throws Exception {
        List<Cdr> cdrs = new ArrayList<>();

        File result = exporter.exportToCsv(cdrs, tempDir.toString(), "empty_test", "Dest");
        List<String> lines = Files.readAllLines(result.toPath());

        assertEquals(1, lines.size()); // Header only
        assertEquals("Type,Direction,A_Party,B_Party,Time,Duration,Cause", lines.get(0));
    }

    @Test
    void testExportCreatesOutputDirectory(@TempDir Path tempDir) throws Exception {
        String nestedDir = tempDir.resolve("nested").resolve("output").toString();

        List<Cdr> cdrs = List.of(
                createCdr(0, 0, "20101111111", "20112222222", "20260706120000", 100, 16)
        );

        File result = exporter.exportToCsv(cdrs, nestedDir, "dir_test", "Dest");

        assertTrue(result.exists());
        assertTrue(new File(nestedDir).isDirectory());
    }

    @Test
    void testExportedCsvDataIntegrity(@TempDir Path tempDir) throws Exception {
        List<Cdr> cdrs = List.of(
                createCdr(0, 0, "20101234567", "20119876543", "20260706150030", 1800, 16)
        );

        File result = exporter.exportToCsv(cdrs, tempDir.toString(), "integrity_test", "Billing");
        List<String> lines = Files.readAllLines(result.toPath());

        String[] fields = lines.get(1).split(",");
        assertEquals("VOICE", fields[0]);
        assertEquals("MO", fields[1]);
        assertEquals("20101234567", fields[2]);
        assertEquals("20119876543", fields[3]);
        assertEquals("20260706150030", fields[4]);
        assertEquals("1800", fields[5]);
        assertEquals("16", fields[6]);
    }

    // ==========================================
    // Helper
    // ==========================================

    private Cdr createCdr(int type, int direction, String dialA, String dialB,
                          String time, int quantity, int cause) {
        Cdr cdr = new Cdr();
        cdr.setRecordType(type);
        cdr.setCallDirection(direction);
        cdr.setDialA(dialA);
        cdr.setDialB(dialB);
        cdr.setAnswerTime(time);
        cdr.setQuantity(quantity);
        cdr.setCauseForTerm(cause);
        return cdr;
    }
}
