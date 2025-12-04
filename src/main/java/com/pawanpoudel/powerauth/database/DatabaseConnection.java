package com.pawanpoudel.powerauth.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnection {

    /**
     * Get a database connection
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    Connection getConnection() throws SQLException;

    /**
     * Initialize the database connection
     */
    void initialize();

    /**
     * Close the database connection
     */
    void close();

    /**
     * Test if the connection is valid
     * 
     * @return true if connection is valid
     */
    boolean isValid();
}
