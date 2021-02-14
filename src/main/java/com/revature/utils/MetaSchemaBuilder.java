package com.revature.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

/**
 * This class contains public static accessors that allows users to add their POJOs and database credentials
 */
public class MetaSchemaBuilder {
    private static HashMap<String, MetaModel<Class<?>>> metaSchemas = new HashMap<>();
    private static MetaSchemaBuilder metaBuilderFactory = new MetaSchemaBuilder();
    private static Properties props;
    /**
     * Adds the class representation of a user defined POJO
     * @param clas Class representation of the user's pojo
     */
    public static <T> void addModel(Class<T> clas) {
        String className = clas.getSimpleName();
        metaSchemas.put(className, (MetaModel<Class<?>>) new MetaModel<T>(clas));
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

    /**
     * Gives the user a meta model based on the classname provided in the params
     * @param className name of class that's associated with the metamodel
     * @return meta model with CRUD functionality
     */
    public static MetaModel<Class<?>> getModel(String className) {
        return metaSchemas.get(className);
    }
}
