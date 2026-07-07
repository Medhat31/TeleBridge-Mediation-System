package com.mediation.website.service;

import com.mediation.website.entity.Admin;
import com.mediation.website.repository.AdminRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for managing authentication and user sessions.
 * Maintains an in-memory session store for logged-in administrators.
 */
public class AuthService {

    private final AdminRepository adminRepository;
    
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    public AuthService() {
        this.adminRepository = new AdminRepository();
    }

    /**
     * Authenticates an administrator against the database and creates a session.
     *
     * @param email    The administrator's email address.
     * @param password The plaintext password to verify.
     * @return A secure UUID session token if credentials are valid, or null if invalid.
     */
    public String login(String email, String password) {
        Admin admin = adminRepository.findByEmail(email);
        
        if (admin != null && admin.getPassword().equals(password)) {
            String token = UUID.randomUUID().toString();
            activeSessions.put(token, email);
            return token;
        }
        return null; 
    }

    /**
     * Checks if a given session token is currently active and valid.
     * Used by the authentication filter to secure API endpoints.
     *
     * @param token The session token to validate.
     * @return True if the token exists in the active sessions map, false otherwise.
     */
    public boolean isValidSession(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return activeSessions.containsKey(token);
    }

    /**
     * Invalidates a user's session, effectively logging them out.
     *
     * @param token The session token to remove.
     */
    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }
}
