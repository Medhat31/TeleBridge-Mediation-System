package com.mediation.website.resources;

import com.mediation.website.entity.MediationRule;
import com.mediation.website.service.RuleService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST API endpoints for managing Mediation Rules.
 * Allows the frontend to perform CRUD operations on rules that dictate CDR routing.
 */
@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RuleResource {

    private final RuleService ruleService;

    public RuleResource() {
        this.ruleService = new RuleService();
    }

    /**
     * Retrieves a list of all configured mediation rules.
     *
     * @return A JSON array of MediationRule objects.
     */
    @GET
    public Response getAllRules() {
        List<MediationRule> rules = ruleService.getAllRules();
        return Response.ok(rules).build();
    }

    /**
     * Retrieves a specific mediation rule by its ID.
     *
     * @param id The ID of the rule to retrieve.
     * @return A JSON representation of the MediationRule, or 404 Not Found.
     */
    @GET
    @Path("/{id}")
    public Response getRuleById(@PathParam("id") int id) {
        MediationRule rule = ruleService.getRuleById(id);
        if (rule != null) {
            return Response.ok(rule).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Creates a new mediation rule. Validates that the referenced nodes exist and are of the correct type.
     *
     * @param rule The MediationRule configuration to create.
     * @return The created MediationRule with its assigned ID, or a 400 Bad Request on validation failure.
     */
    @POST
    public Response createRule(MediationRule rule) {
        try {
            MediationRule created = ruleService.createRule(rule);
            if (created != null) {
                return Response.status(Response.Status.CREATED).entity(created).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Updates an existing mediation rule's configuration.
     *
     * @param id   The ID of the rule to update.
     * @param rule The updated MediationRule configuration.
     * @return A 200 OK response on success, or 400 Bad Request on validation failure.
     */
    @PUT
    @Path("/{id}")
    public Response updateRule(@PathParam("id") int id, MediationRule rule) {
        try {
            boolean updated = ruleService.updateRule(id, rule);
            if (updated) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Toggles the active status of a mediation rule. The core engine will hot-reload this.
     *
     * @param id     The ID of the rule.
     * @param active True to activate, false to deactivate.
     * @return A 200 OK response on success, or 404 Not Found.
     */
    @PUT
    @Path("/{id}/status")
    public Response toggleRuleStatus(@PathParam("id") int id, @QueryParam("active") boolean active) {
        boolean updated = ruleService.toggleRuleStatus(id, active);
        if (updated) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Deletes a mediation rule from the database.
     *
     * @param id The ID of the rule to delete.
     * @return A 200 OK response on success, or 404 Not Found.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteRule(@PathParam("id") int id) {
        boolean deleted = ruleService.deleteRule(id);
        if (deleted) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
