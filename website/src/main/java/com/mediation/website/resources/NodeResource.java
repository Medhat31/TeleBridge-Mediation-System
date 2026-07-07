package com.mediation.website.resources;

import com.mediation.website.entity.Node;
import com.mediation.website.service.NodeService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST API endpoints for managing Node configurations.
 * Allows the website frontend to perform CRUD operations on network nodes.
 */
@Path("/nodes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NodeResource {

    private final NodeService nodeService;

    public NodeResource() {
        this.nodeService = new NodeService();
    }

    /**
     * Retrieves a list of all configured nodes.
     *
     * @return A JSON array of Node objects.
     */
    @GET
    public Response getAllNodes() {
        List<Node> nodes = nodeService.getAllNodes();
        return Response.ok(nodes).build();
    }

    /**
     * Retrieves a specific node by its ID.
     *
     * @param id The ID of the node to retrieve.
     * @return A JSON representation of the Node, or a 404 Not Found response.
     */
    @GET
    @Path("/{id}")
    public Response getNodeById(@PathParam("id") int id) {
        Node node = nodeService.getNodeById(id);
        if (node != null) {
            return Response.ok(node).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Creates a new node configuration and provisions its Docker container via the core engine.
     *
     * @param node The Node configuration to create.
     * @return The created Node with its assigned ID, or a 400 Bad Request if validation fails.
     */
    @POST
    public Response createNode(Node node) {
        try {
            Node created = nodeService.createNode(node);
            if (created != null) {
                return Response.status(Response.Status.CREATED).entity(created).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Updates an existing node's configuration.
     *
     * @param id   The ID of the node to update.
     * @param node The updated Node configuration.
     * @return A 200 OK response on success, or 404 Not Found.
     */
    @PUT
    @Path("/{id}")
    public Response updateNode(@PathParam("id") int id, Node node) {
        boolean updated = nodeService.updateNode(id, node);
        if (updated) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Toggles the active status of a node, automatically tearing down or provisioning
     * its Docker infrastructure as needed.
     *
     * @param id     The ID of the node.
     * @param active True to activate, false to deactivate.
     * @return A 200 OK response on success, or 404 Not Found.
     */
    @PUT
    @Path("/{id}/status")
    public Response toggleNodeStatus(@PathParam("id") int id, @QueryParam("active") boolean active) {
        boolean updated = nodeService.toggleNodeStatus(id, active);
        if (updated) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Deletes a node and forcefully tears down its associated Docker infrastructure.
     *
     * @param id The ID of the node to delete.
     * @return A 200 OK response on success, or 400 Bad Request if the node is actively used in rules.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteNode(@PathParam("id") int id) {
        try {
            boolean deleted = nodeService.deleteNode(id);
            if (deleted) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
