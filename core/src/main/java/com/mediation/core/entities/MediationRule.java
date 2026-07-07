package com.mediation.core.entities;

/**
 * Represents a mediation routing rule used by the core engine.
 * Defines how CDRs flow from a source node to a destination node and specifies applicable filters.
 */
public class MediationRule {

    private int ruleId;
    private int sourceNodeId;
    private int destinationNodeId;
    private boolean filterZeroDuration;
    private boolean filterEmergency;
    private boolean isActive;

    public MediationRule() {
    }

    public MediationRule(int ruleId, int sourceNodeId, int destinationNodeId, boolean filterZeroDuration, boolean filterEmergency, boolean isActive) {
        this.ruleId = ruleId;
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
        this.filterZeroDuration = filterZeroDuration;
        this.filterEmergency = filterEmergency;
        this.isActive = isActive;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(int sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public int getDestinationNodeId() {
        return destinationNodeId;
    }

    public void setDestinationNodeId(int destinationNodeId) {
        this.destinationNodeId = destinationNodeId;
    }

    public boolean isFilterZeroDuration() {
        return filterZeroDuration;
    }

    public void setFilterZeroDuration(boolean filterZeroDuration) {
        this.filterZeroDuration = filterZeroDuration;
    }

    public boolean isFilterEmergency() {
        return filterEmergency;
    }

    public void setFilterEmergency(boolean filterEmergency) {
        this.filterEmergency = filterEmergency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    @Override
    public String toString() {
        return "MediationRule{"
                + "ruleId=" + ruleId
                + ", source=" + sourceNodeId
                + ", destination=" + destinationNodeId
                + ", active=" + isActive
                + '}';
    }
}
