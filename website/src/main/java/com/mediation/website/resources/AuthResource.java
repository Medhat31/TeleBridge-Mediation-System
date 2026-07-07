package com.mediation.website.resources;

import com.mediation.website.entity.LoginRequest;
import com.mediation.website.service.AuthService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API endpoints for user authentication.
 * Handles login and logout operations for administrators.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    public AuthResource() {
        this.authService = new AuthService();
    }

    /**
     * Authenticates an administrator using their email and password.
     *
     * @param request The JSON payload containing the user's email and password.
     * @return A JSON response containing a JWT Bearer token upon success, or an error message if unauthorized.
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        if (token != null) {
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");
            return Response.ok(response).build();
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
        }
    }

    /**
     * Logs out the currently authenticated administrator by invalidating their session token.
     *
     * @param token The Authorization Bearer token from the HTTP headers.
     * @return A 200 OK response upon successful logout.
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            authService.logout(token);
        }
        return Response.ok().build();
    }
}
