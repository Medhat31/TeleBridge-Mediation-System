package com.mediation.website.resources;

import com.mediation.website.service.MetricsService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API endpoint for retrieving system performance metrics.
 * Used by the website frontend to display statistics.
 */
@Path("/metrics")
public class MetricsResource {

    private final MetricsService metricsService;

    public MetricsResource() {
        this.metricsService = new MetricsService();
    }

    /**
     * Retrieves the total number of CDRs successfully processed by the system today.
     *
     * @return A JSON response containing the total CDR count.
     */
    @GET
    @Path("/cdrs-today")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCdrsProcessedToday() {
        int count = metricsService.getCdrsProcessedToday();
        Map<String, Integer> response = new HashMap<>();
        response.put("totalCdrs", count);
        return Response.ok(response).build();
    }
}
