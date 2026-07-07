package com.mediation.website.resources;

import com.mediation.website.entity.ActivityLog;
import com.mediation.website.service.ActivityService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST API endpoint for retrieving system activity logs.
 * Used by the website frontend to populate the recent activity dashboard.
 */
@Path("/activities")
public class ActivityResource {

    private final ActivityService activityService;

    public ActivityResource() {
        this.activityService = new ActivityService();
    }

    /**
     * Retrieves the most recent activity logs.
     *
     * @param limit Optional parameter defining the maximum number of logs to return. Defaults to 10.
     * @return A JSON response containing a list of ActivityLog objects.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecentActivities(@QueryParam("limit") Integer limit) {
        int queryLimit = (limit != null && limit > 0) ? limit : 10;
        
        List<ActivityLog> activities = activityService.getRecentActivities(queryLimit);
        return Response.ok(activities).build();
    }
}
