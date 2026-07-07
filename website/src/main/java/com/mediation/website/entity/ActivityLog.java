package com.mediation.website.entity;

import java.sql.Timestamp;

/**
 * Represents a single system event or activity log.
 * Used for displaying recent system events on the administrative dashboard.
 */
public class ActivityLog {

    private int id;
    private Timestamp logTime;
    private String message;
    private String type;

    public ActivityLog() {}

    public ActivityLog(int id, Timestamp logTime, String message, String type) {
        this.id = id;
        this.logTime = logTime;
        this.message = message;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getLogTime() {
        return logTime;
    }

    public void setLogTime(Timestamp logTime) {
        this.logTime = logTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
