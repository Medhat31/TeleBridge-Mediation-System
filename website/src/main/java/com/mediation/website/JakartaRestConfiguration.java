package com.mediation.website;

import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.ApplicationPath;

/**
 * Configures Jakarta RESTful Web Services for the application.
 */
@ApplicationPath("api")
public class JakartaRestConfiguration extends ResourceConfig {
    
    public JakartaRestConfiguration() {
        // Tell Jersey to scan this package for REST resources
        packages("com.mediation.website.resources");
    }
}
