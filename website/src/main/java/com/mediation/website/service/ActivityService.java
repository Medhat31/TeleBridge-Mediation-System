package com.mediation.website.service;

import com.mediation.website.entity.ActivityLog;
import com.mediation.website.repository.ActivityRepository;

import java.util.List;

/**
 * Service layer for handling business logic related to system activity logs.
 * Acts as an intermediary between the REST resources and the database repository.
 */
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService() {
        this.activityRepository = new ActivityRepository();
    }

    /**
     * Retrieves the most recent activity logs from the system.
     *
     * @param limit The maximum number of logs to fetch.
     * @return A list of ActivityLog objects representing recent events.
     */
    public List<ActivityLog> getRecentActivities(int limit) {
        return activityRepository.getRecentActivities(limit);
    }
}
