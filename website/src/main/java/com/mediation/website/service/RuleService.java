package com.mediation.website.service;

import com.mediation.website.entity.MediationRule;
import com.mediation.website.entity.Node;
import com.mediation.website.repository.NodeRepository;
import com.mediation.website.repository.RuleRepository;

import java.util.List;

/**
 * Service layer for managing Mediation Rules.
 * Contains the business logic for validating rule configurations and ensuring rules do not conflict.
 */
public class RuleService {

    private final RuleRepository ruleRepository;
    private final NodeRepository nodeRepository;

    public RuleService() {
        this.ruleRepository = new RuleRepository();
        this.nodeRepository = new NodeRepository();
    }

    /**
     * Retrieves all mediation rules from the repository.
     *
     * @return A list of all MediationRule objects.
     */
    public List<MediationRule> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * Retrieves a specific mediation rule by its ID.
     *
     * @param id The ID of the rule.
     * @return The MediationRule object if found, or null.
     */
    public MediationRule getRuleById(int id) {
        return ruleRepository.findById(id);
    }

    /**
     * Creates a new mediation rule.
     * Validates that both source and destination nodes exist, and prevents duplicate routing paths.
     *
     * @param rule The rule configuration to create.
     * @return The fully formed MediationRule with its generated ID.
     * @throws IllegalArgumentException If validation fails.
     */
    public MediationRule createRule(MediationRule rule) {
        if (!validateNodesExist(rule.getSourceNodeId(), rule.getDestinationNodeId())) {
            throw new IllegalArgumentException("Source or Destination Node does not exist.");
        }
        validateRuleNotDuplicate(rule.getSourceNodeId(), rule.getDestinationNodeId(), null);
        return ruleRepository.insert(rule);
    }

    /**
     * Updates an existing mediation rule.
     * Validates that both source and destination nodes still exist, and prevents duplicate routing paths.
     *
     * @param id   The ID of the rule to update.
     * @param rule The updated rule configuration.
     * @return True if successful.
     * @throws IllegalArgumentException If validation fails.
     */
    public boolean updateRule(int id, MediationRule rule) {
        if (!validateNodesExist(rule.getSourceNodeId(), rule.getDestinationNodeId())) {
            throw new IllegalArgumentException("Source or Destination Node does not exist.");
        }
        validateRuleNotDuplicate(rule.getSourceNodeId(), rule.getDestinationNodeId(), id);
        return ruleRepository.update(id, rule);
    }

    /**
     * Toggles a rule's active status.
     *
     * @param id       The ID of the rule.
     * @param isActive The new active status.
     * @return True if successful.
     */
    public boolean toggleRuleStatus(int id, boolean isActive) {
        return ruleRepository.updateStatus(id, isActive);
    }

    /**
     * Deletes a mediation rule from the system.
     *
     * @param id The ID of the rule to delete.
     * @return True if successful.
     */
    public boolean deleteRule(int id) {
        return ruleRepository.delete(id);
    }

    /**
     * Validates that both the source and destination nodes exist in the database.
     *
     * @param sourceNodeId The ID of the source node.
     * @param destNodeId   The ID of the destination node.
     * @return True if both nodes exist in the database.
     */
    private boolean validateNodesExist(int sourceNodeId, int destNodeId) {
        Node source = nodeRepository.findById(sourceNodeId);
        Node dest = nodeRepository.findById(destNodeId);
        
        if (source == null || dest == null) {
            return false;
        }
        
        return true;
    }

    /**
     * Validates that a rule with the same source and destination doesn't already exist.
     * 
     * @param sourceNodeId  The ID of the source node.
     * @param destNodeId    The ID of the destination node.
     * @param currentRuleId The ID of the rule being updated, or null if creating a new rule.
     * @throws IllegalArgumentException If a duplicate rule is found.
     */
    private void validateRuleNotDuplicate(int sourceNodeId, int destNodeId, Integer currentRuleId) {
        List<MediationRule> existingRules = ruleRepository.findAll();
        for (MediationRule existing : existingRules) {
            if (existing.getSourceNodeId() == sourceNodeId && existing.getDestinationNodeId() == destNodeId) {
                if (currentRuleId == null || existing.getRuleId() != currentRuleId) {
                    throw new IllegalArgumentException("A rule with this exact Source and Destination already exists.");
                }
            }
        }
    }
}
