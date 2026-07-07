package com.mediation.core.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton class responsible for initializing and managing the HikariCP database connection pool.
 */
public class DBConnection {

    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);
    private static final HikariDataSource dataSource;

    static {
        Properties props = new Properties();
        
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            
            if (input == null) {
                throw new RuntimeException("db.properties file not found in classpath.");
            }
            props.load(input);

            HikariConfig config = new HikariConfig();

            String dbUrl = props.getProperty("db.url").replace("\"", "").trim();
            String dbUser = props.getProperty("db.user").replace("\"", "").trim();
            String dbPassword = props.getProperty("db.password").replace("\"", "").trim();
     
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            dataSource = new HikariDataSource(config);
            log.info("HikariCP Connection Pool initialized -> {}", dbUrl);
            
        } catch (Exception e) {
            log.error("Failed to configure HikariCP.", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {} 

    /**
     * Dispenses an active database connection from the HikariCP pool.
     *
     * @return A SQL Connection object.
     * @throws SQLException If the pool cannot provide a connection.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Gracefully shuts down the HikariCP connection pool, closing all open connections.
     * Bound to the JVM shutdown hook in the main application loop.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down HikariCP Connection Pool...");
            dataSource.close();
        }
    }
}