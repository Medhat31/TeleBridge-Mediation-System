package com.mediation.website.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton class responsible for managing the HikariCP database connection pool for the website backend.
 * Provides database connections to all repository classes.
 */
public class DBConnection {

    private static final HikariDataSource dataSource;

    static {
        try {
            Class.forName("org.postgresql.Driver");

            Properties props = new Properties();
            
            try (java.io.InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                } else {
                    System.err.println("WARNING: db.properties file NOT FOUND in classpath!");
                }
            } catch (IOException e) {
                System.err.println("Could not read db.properties file: " + e.getMessage());
            }

            String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : props.getProperty("db.url", "jdbc:postgresql://localhost:5434/mediation_db");
            String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : props.getProperty("db.user", "telecom_user");
            String dbPassword = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : props.getProperty("db.password", "1234");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {} 

    /**
     * Gets an active database connection from the connection pool.
     *
     * @return A SQL Connection object ready to be used for queries.
     * @throws SQLException If the connection pool cannot provide a connection.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
