package com.mediation.website.resources;

import com.mediation.website.service.AuthService;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Intercepts incoming HTTP requests to ensure they are authenticated.
 * Acts as a security gateway protecting all REST API endpoints except login.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private final AuthService authService;

    public AuthFilter() {
        this.authService = new AuthService();
    }

    /**
     * Inspects the Authorization header of the incoming request.
     * Rejects requests with missing or invalid JWT Bearer tokens.
     *
     * @param requestContext The context of the incoming HTTP request.
     * @throws IOException If an I/O error occurs during filtering.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        if (path.contains("auth/login")) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing or invalid Authorization header\"}")
                        .build()
            );
            return;
        }

        String token = authHeader.substring("Bearer".length()).trim();

        if (!authService.isValidSession(token)) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Invalid or expired session token\"}")
                        .build()
            );
        }
    }
}
