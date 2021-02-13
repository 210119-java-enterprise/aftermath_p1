package com.revature.utils;

import org.w3c.dom.Element;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ConnectionFactory.java
 * A factory that is used to obtain a Connection object
 */
public class ConnectionFactory {

    private static ConnectionFactory connFactory = new ConnectionFactory();
    private static Connection conn = null;

    /**
     * This is used to read the application.properties file in the resources folder
     */
    private Properties props = new Properties();

    /**
     * This static block ensures that the database driver is loaded
     * as soon as the class loads in memory
     */
    static {
        try {
            // in the future if time permits, replace string with user defined database engine flag which tells
            // what database engine to use
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * this constructor loads the application.properties file and parses the database credentials.
     */
    private ConnectionFactory() {

    }

    public static ConnectionFactory getInstance() {
        return connFactory;
    }

    /**
     * Returns a connection to the user specified schema on the main database
     * instance
     */
    public Connection getConnection() {
        props = MetaSchemaBuilder.getCredentials();

        if (conn == null) {
            try {
                conn = DriverManager.getConnection(
                        props.getProperty("url"),
                        props.getProperty("username"),
                        props.getProperty("password")
                );
                conn.setSchema(props.getProperty("currentSchema"));

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return conn;
    }
}