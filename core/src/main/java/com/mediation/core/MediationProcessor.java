package com.mediation.core;

import com.mediation.core.config.AppConfig;
import com.mediation.core.entities.Cdr;
import com.mediation.core.entities.MediationRule;
import com.mediation.core.entities.Node;
import com.mediation.core.repository.ActivityLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the business logic for filtering, cleaning, and routing telecom records.
 * This class applies mediation rules to raw data, dropping duplicates and invalid entries.
 */
public class MediationProcessor {

    private static final Logger log = LoggerFactory.getLogger(MediationProcessor.class);

    /**
     * Processes a raw list of CDRs based on the active rules configured for the source node.
     * Applies deduplication, zero-duration filtering, and emergency number filtering.
     *
     * @param rawCdrs     The list of parsed, unfiltered CDR objects.
     * @param sourceNode  The node that these CDRs originated from.
     * @param activeRules The list of currently active mediation rules.
     * @return A map mapping the Destination Node ID to the final cleaned list of CDRs.
     */
    public Map<Integer, List<Cdr>> process(List<Cdr> rawCdrs, Node sourceNode, List<MediationRule> activeRules) {
        
        Map<Integer, List<Cdr>> routedCdrs = new HashMap<>();
        Set<String> uniqueSignatures = new HashSet<>();
        
        MediationRule appliedRule = null;
        for (MediationRule rule : activeRules) {
            if (rule.getSourceNodeId() == sourceNode.getNodeId()) {
                appliedRule = rule;
                break;
            }
        }

        if (appliedRule == null) {
            log.warn("  -> No active mediation rule found for Node ID: {}. Dropping CDRs.", sourceNode.getNodeId());
            return routedCdrs; 
        }

        int destinationId = appliedRule.getDestinationNodeId();
        routedCdrs.put(destinationId, new ArrayList<>());

        int duplicateCount = 0;
        int zeroDurationCount = 0;
        int emergencyCount = 0;

        Set<String> emergencyNumbers = AppConfig.getEmergencyNumbers();

        for (Cdr cdr : rawCdrs) {
            
            String signature = cdr.getDialA().trim() + "_" + cdr.getDialB().trim() + "_" + cdr.getAnswerTime().trim();
            
            if (uniqueSignatures.contains(signature)) {
                duplicateCount++;
                continue; 
            }
            uniqueSignatures.add(signature);

            if (appliedRule.isFilterZeroDuration() && cdr.getQuantity() == 0) {
                zeroDurationCount++;
                continue;
            }

            if (appliedRule.isFilterEmergency()) {
                String bParty = cdr.getDialB().trim();
                if (emergencyNumbers.contains(bParty)) {
                    emergencyCount++;
                    continue;
                }
            }

            routedCdrs.get(destinationId).add(cdr);
        }

        log.info("     [MEDIATION SUMMARY for {}]", sourceNode.getNodeName());
        log.info("      - Raw Records: {}", rawCdrs.size());
        log.info("      - Dropped (Duplicates): {}", duplicateCount);
        log.info("      - Dropped (Zero Duration): {}", zeroDurationCount);
        log.info("      - Dropped (Emergency): {}", emergencyCount);
        log.info("      - Clean Records Routed to Node {}: {}", destinationId, routedCdrs.get(destinationId).size());

        ActivityLogger.logInfo(String.format("Engine processed %d records from %s", rawCdrs.size(), sourceNode.getNodeName()));
        if (duplicateCount > 0) ActivityLogger.logWarning(String.format("Filtered %d duplicate records from %s", duplicateCount, sourceNode.getNodeName()));
        if (zeroDurationCount > 0) ActivityLogger.logWarning(String.format("Filtered %d zero-duration calls from %s", zeroDurationCount, sourceNode.getNodeName()));
        if (emergencyCount > 0) ActivityLogger.logWarning(String.format("Filtered %d emergency calls from %s", emergencyCount, sourceNode.getNodeName()));

        return routedCdrs;
    }
}