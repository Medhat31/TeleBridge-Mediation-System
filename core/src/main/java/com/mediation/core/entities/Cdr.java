package com.mediation.core.entities;

/**
 * Represents a parsed Call Detail Record (CDR).
 * Holds the extracted data fields required for mediation, filtering, and billing.
 */
public class Cdr {
    
    private int recordId;
    private int recordType;       // 0 = Voice, 1 = SMS
    private String dialA;
    private String dialB;
    private String answerTime;    // Format: YYYYMMDDHHMMSS
    private int quantity;         // Duration in seconds OR number of SMS
    private int causeForTerm;     // e.g., 16 = Normal Release
    private int callDirection;    // 0 = MO (Originated), 1 = MT (Terminated)

    public Cdr() {
    }

    // --- Getters and Setters ---

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public int getRecordType() { return recordType; }
    public void setRecordType(int recordType) { this.recordType = recordType; }

    public String getDialA() { return dialA; }
    public void setDialA(String dialA) { this.dialA = dialA; }

    public String getDialB() { return dialB; }
    public void setDialB(String dialB) { this.dialB = dialB; }

    public String getAnswerTime() { return answerTime; }
    public void setAnswerTime(String answerTime) { this.answerTime = answerTime; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getCauseForTerm() { return causeForTerm; }
    public void setCauseForTerm(int causeForTerm) { this.causeForTerm = causeForTerm; }

    public int getCallDirection() { return callDirection; }
    public void setCallDirection(int callDirection) { this.callDirection = callDirection; }

    // --- Helper Method for Debugging ---
    @Override
    public String toString() {
        String typeStr = (recordType == 0) ? "VOICE" : "SMS";
        String dirStr = (callDirection == 0) ? "MO" : "MT";
        
        return String.format(
            "CDR [#%04d] | Type: %-5s | Dir: %-2s | A: %-12s | B: %-12s | Time: %s | Qty: %-4d | Cause: %d",
            recordId, typeStr, dirStr, dialA, dialB, answerTime, quantity, causeForTerm
        );
    }
}