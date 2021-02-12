package com.revature.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import com.revature.annotations.*;

/**
 * This class contains public static accessors that allows users to add their POJOs and database credentials
 */
public class MetaSchemaBuilder {
    private static HashMap<String, Class<?>> metaSchemas = new HashMap<>();
    private static MetaSchemaBuilder metaBuilderFactory = new MetaSchemaBuilder();
    private static Properties props;
    /**
     * Adds the class representation of a user defined POJO
     * @param clas Class representation of the user's pojo
     */
    public static void addModel(Class<?> clas) {
        Class<?>[] classes = new Class<?>[] {PK.class, FK.class};
        String className = clas.getSimpleName();
        metaSchemas.put(className, clas);
    }

    /**
     * Adds multiple class representations of usr defined pojos via a Class array
     * @param classes array that contains class POJOs
     */
    public static void addModels(Class<?>[] classes) {
        Arrays.stream(classes).forEach(clas -> addModel(clas));
    }

    /**
     * Adds a user's database credentials
     * @param props contains the application.properties file
     */
    public static void addCredentials(Properties props) {
        MetaSchemaBuilder.props = props;
    }

    /**
     * Internal method used for giving the connection factory a user's credentials
     * @return the application.properties file that contains a user's credentials
     */
    protected static Properties getCredentials() { return MetaSchemaBuilder.props; }
}
