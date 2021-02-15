package com.revature.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.lang.Class;

/**
 * This class contains public static accessors that allows users to add their POJOs and database credentials
 */
public class MetaSchemaBuilder<T> {
    private HashMap<String, MetaModel<T>> metaSchemas;
    private static Properties props;
    /**
     * Adds the class representation of a user defined POJO
     * @param clas Class representation of the user's pojo
     */

    public MetaSchemaBuilder() {
        metaSchemas = new HashMap<>();

    }
    public <T> void addModel(Class clas) {
        String className = clas.getSimpleName();
        metaSchemas.put(className, new MetaModel<>(clas));
    }

    /**
     * Adds multiple class representations of usr defined pojos via a Class array
     * @param classes array that contains class POJOs
     */
    public void addModels(Class<T>[] classes) {
        Arrays.stream(classes).forEach(clas -> addModel(clas));
    }

    /**
     * Gives the user a meta model based on the classname provided in the params
     * @param className name of class that's associated with the metamodel
     * @return meta model with CRUD functionality
     */
    public MetaModel<T> getModel(String className) {
        return metaSchemas.get(className);
    }

    public int count() { return metaSchemas.size(); }
}