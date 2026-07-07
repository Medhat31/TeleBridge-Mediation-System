package com.mediation.core;

import com.mediation.core.entities.Cdr;
import com.mediation.core.entities.MediationRule;
import com.mediation.core.entities.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MediationProcessorTest {

    private MediationProcessor processor;
    private Node sourceNode;
    private List<MediationRule> rules;

    @BeforeEach
    void setUp() {
        processor = new MediationProcessor();

        sourceNode = new Node(1, "Cairo_MSC_01", "UPSTREAM", "FTP",
                "localhost", 21, "user", "pass", true);

        MediationRule rule = new MediationRule(1, 1, 2, true, true, true);
        rules = new ArrayList<>();
        rules.add(rule);
    }

    // ==========================================
    // Basic Routing Tests
    // ==========================================

    @Test
    void testCleanRecordsAreRoutedToDestination() {
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 300),
                createCdr(2, "20103333333", "20114444444", "20260706120001", 600)
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertTrue(result.containsKey(2)); // destination node ID
        assertEquals(2, result.get(2).size());
    }

    @Test
    void testNoRuleFoundReturnsEmptyMap() {
        Node unknownNode = new Node(99, "Unknown_Node", "UPSTREAM", "FTP",
                "localhost", 21, "user", "pass", true);

        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 300)
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, unknownNode, rules);

        assertTrue(result.isEmpty());
    }

    // ==========================================
    // Deduplication Tests
    // ==========================================

    @Test
    void testDuplicateCdrsAreRemoved() {
        // Same A, B, and time = duplicate
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 300),
                createCdr(2, "20101111111", "20112222222", "20260706120000", 300), // duplicate
                createCdr(3, "20103333333", "20114444444", "20260706120001", 600)  // unique
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertEquals(2, result.get(2).size()); // 1 duplicate removed
    }

    @Test
    void testDifferentTimesAreNotDuplicates() {
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 300),
                createCdr(2, "20101111111", "20112222222", "20260706120001", 300) // same parties, different time
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertEquals(2, result.get(2).size()); // Both kept
    }

    // ==========================================
    // Zero Duration Filter Tests
    // ==========================================

    @Test
    void testZeroDurationFilteredWhenEnabled() {
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 0),   // zero duration
                createCdr(2, "20103333333", "20114444444", "20260706120001", 300)  // normal
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertEquals(1, result.get(2).size());
        assertEquals(300, result.get(2).get(0).getQuantity());
    }

    @Test
    void testZeroDurationKeptWhenFilterDisabled() {
        MediationRule noZeroFilter = new MediationRule(1, 1, 2, false, true, true);
        List<MediationRule> customRules = List.of(noZeroFilter);

        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 0)
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, customRules);

        assertEquals(1, result.get(2).size()); // Zero duration kept
    }

    // ==========================================
    // Emergency Filter Tests
    // ==========================================

    @Test
    void testEmergencyNumbersFilteredWhenEnabled() {
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "112", "20260706120000", 60),   // emergency 112
                createCdr(2, "20101111111", "122", "20260706120001", 45),   // emergency 122
                createCdr(3, "20101111111", "123", "20260706120002", 30),   // emergency 123
                createCdr(4, "20101111111", "180", "20260706120003", 90),   // emergency 180
                createCdr(5, "20101111111", "20115555555", "20260706120004", 300) // normal
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertEquals(1, result.get(2).size());
        assertEquals("20115555555", result.get(2).get(0).getDialB());
    }

    @Test
    void testEmergencyNumbersKeptWhenFilterDisabled() {
        MediationRule noEmergencyFilter = new MediationRule(1, 1, 2, true, false, true);
        List<MediationRule> customRules = List.of(noEmergencyFilter);

        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "112", "20260706120000", 60)
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, customRules);

        assertEquals(1, result.get(2).size()); // Emergency kept
    }

    // ==========================================
    // Combined Filter Tests
    // ==========================================

    @Test
    void testAllFiltersAppliedTogether() {
        List<Cdr> rawCdrs = List.of(
                createCdr(1, "20101111111", "20112222222", "20260706120000", 300),  // KEEP
                createCdr(2, "20101111111", "20112222222", "20260706120000", 300),  // DUPLICATE
                createCdr(3, "20103333333", "20114444444", "20260706120001", 0),    // ZERO DURATION
                createCdr(4, "20105555555", "112", "20260706120002", 60),           // EMERGENCY
                createCdr(5, "20107777777", "20118888888", "20260706120003", 500)   // KEEP
        );

        Map<Integer, List<Cdr>> result = processor.process(rawCdrs, sourceNode, rules);

        assertEquals(2, result.get(2).size()); // Only records 1 and 5
    }

    // ==========================================
    // Helper
    // ==========================================

    private Cdr createCdr(int id, String dialA, String dialB, String time, int quantity) {
        Cdr cdr = new Cdr();
        cdr.setRecordId(id);
        cdr.setRecordType(0);
        cdr.setDialA(dialA);
        cdr.setDialB(dialB);
        cdr.setAnswerTime(time);
        cdr.setQuantity(quantity);
        cdr.setCauseForTerm(16);
        cdr.setCallDirection(0);
        return cdr;
    }
}
