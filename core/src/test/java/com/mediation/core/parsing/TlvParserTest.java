package com.mediation.core.parsing;

import com.mediation.core.entities.Cdr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TlvParserTest {

    private TlvParser parser;

    @BeforeEach
    void setUp() {
        parser = new TlvParser();
    }

    // ==========================================
    // Hex Stream Decoding Tests
    // ==========================================

    @Test
    void testDecodeValidVoiceCdr() {
        // Build a known TLV: recordId=1, type=0(VOICE), dialA="20101234567", dialB="20119876543",
        // answerTime="20260706120000", quantity=300, cause=16, direction=0(MO)
        String hex = buildTestTlvHex(1, 0, "20101234567", "20119876543", "20260706120000", 300, 16, 0);

        Cdr cdr = parser.decodeHexStream(hex);

        assertNotNull(cdr);
        assertEquals(1, cdr.getRecordId());
        assertEquals(0, cdr.getRecordType()); // VOICE
        assertEquals("20101234567", cdr.getDialA());
        assertEquals("20119876543", cdr.getDialB());
        assertEquals("20260706120000", cdr.getAnswerTime());
        assertEquals(300, cdr.getQuantity());
        assertEquals(16, cdr.getCauseForTerm());
        assertEquals(0, cdr.getCallDirection()); // MO
    }

    @Test
    void testDecodeSmsCdr() {
        String hex = buildTestTlvHex(42, 1, "20105551234", "20118887654", "20260706130000", 1, 16, 1);

        Cdr cdr = parser.decodeHexStream(hex);

        assertNotNull(cdr);
        assertEquals(42, cdr.getRecordId());
        assertEquals(1, cdr.getRecordType()); // SMS
        assertEquals(1, cdr.getQuantity()); // 1 SMS
        assertEquals(1, cdr.getCallDirection()); // MT
    }

    @Test
    void testDecodeZeroDurationCdr() {
        String hex = buildTestTlvHex(5, 0, "20101111111", "20112222222", "20260706140000", 0, 16, 0);

        Cdr cdr = parser.decodeHexStream(hex);

        assertNotNull(cdr);
        assertEquals(0, cdr.getQuantity());
    }

    @Test
    void testDecodeEmergencyCallCdr() {
        String hex = buildTestTlvHex(10, 0, "20103333333", "112", "20260706150000", 60, 16, 0);

        Cdr cdr = parser.decodeHexStream(hex);

        assertNotNull(cdr);
        assertEquals("112", cdr.getDialB());
    }

    @Test
    void testDecodeInvalidMasterTagReturnsNull() {
        // Replace A0 with B0 (invalid master tag)
        String hex = "B0" + "10" + "0104" + "00000001";

        Cdr cdr = parser.decodeHexStream(hex);

        assertNull(cdr);
    }

    @Test
    void testDecodeTruncatedStreamReturnsNull() {
        // Stream too short to contain valid data
        String hex = "A004010400";

        Cdr cdr = parser.decodeHexStream(hex);

        assertNull(cdr);
    }

    @Test
    void testDecodeEmptyStringReturnsNull() {
        Cdr cdr = parser.decodeHexStream("");
        assertNull(cdr);
    }

    // ==========================================
    // File Parsing Tests
    // ==========================================

    @Test
    void testParseFileWithMultipleRecords(@TempDir Path tempDir) throws Exception {
        File testFile = tempDir.resolve("test_cdr.dat").toFile();

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(buildTestTlvHex(1, 0, "20101111111", "20112222222", "20260706120000", 100, 16, 0) + "\n");
            writer.write(buildTestTlvHex(2, 1, "20103333333", "20114444444", "20260706120001", 1, 16, 1) + "\n");
            writer.write(buildTestTlvHex(3, 0, "20105555555", "20116666666", "20260706120002", 500, 16, 0) + "\n");
        }

        List<Cdr> records = parser.parseFile(testFile);

        assertEquals(3, records.size());
        assertEquals(1, records.get(0).getRecordId());
        assertEquals(2, records.get(1).getRecordId());
        assertEquals(3, records.get(2).getRecordId());
    }

    @Test
    void testParseEmptyFile(@TempDir Path tempDir) throws Exception {
        File testFile = tempDir.resolve("empty.dat").toFile();
        testFile.createNewFile();

        List<Cdr> records = parser.parseFile(testFile);

        assertTrue(records.isEmpty());
    }

    @Test
    void testParseFileSkipsBlankLines(@TempDir Path tempDir) throws Exception {
        File testFile = tempDir.resolve("blanks.dat").toFile();

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(buildTestTlvHex(1, 0, "20101111111", "20112222222", "20260706120000", 100, 16, 0) + "\n");
            writer.write("\n"); // blank line
            writer.write("   \n"); // whitespace-only line
            writer.write(buildTestTlvHex(2, 0, "20103333333", "20114444444", "20260706120001", 200, 16, 1) + "\n");
        }

        List<Cdr> records = parser.parseFile(testFile);

        assertEquals(2, records.size());
    }

    // ==========================================
    // Helper: Build a TLV hex string
    // ==========================================
    private String buildTestTlvHex(int recordId, int recordType, String dialA, String dialB,
                                    String answerTime, int quantity, int cause, int direction) {
        StringBuilder payload = new StringBuilder();

        // Tag 01: recordId (4 bytes)
        payload.append(intToTlv("01", recordId, 4));
        // Tag 02: recordType (1 byte)
        payload.append(intToTlv("02", recordType, 1));
        // Tag 03: dialA (string)
        payload.append(strToTlv("03", dialA));
        // Tag 04: dialB (string)
        payload.append(strToTlv("04", dialB));
        // Tag 05: answerTime (string)
        payload.append(strToTlv("05", answerTime));
        // Tag 06: quantity (4 bytes)
        payload.append(intToTlv("06", quantity, 4));
        // Tag 07: causeForTerm (1 byte)
        payload.append(intToTlv("07", cause, 1));
        // Tag 08: callDirection (1 byte)
        payload.append(intToTlv("08", direction, 1));

        int payloadBytes = payload.length() / 2;
        String masterLen = String.format("%02X", payloadBytes);

        return "A0" + masterLen + payload;
    }

    private String intToTlv(String tag, int value, int numBytes) {
        String valHex = String.format("%0" + (numBytes * 2) + "X", value);
        String lenHex = String.format("%02X", numBytes);
        return tag + lenHex + valHex;
    }

    private String strToTlv(String tag, String text) {
        StringBuilder hexBuilder = new StringBuilder();
        for (char c : text.toCharArray()) {
            hexBuilder.append(String.format("%02X", (int) c));
        }
        String valHex = hexBuilder.toString();
        int numBytes = valHex.length() / 2;
        String lenHex = String.format("%02X", numBytes);
        return tag + lenHex + valHex;
    }
}
