package com.revature.utils;

import com.revature.annotations.Table;
import com.revature.exceptions.BadMethodChainCallException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

class Grab<T> extends ModelScraper {
    PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;

    Grab (CrudModel<T> ref) {
        this.setTargetClass(ref.clas);
        this.conn = ref.conn;
        this.ref = ref;
        appliedAttrs = new ArrayList<>();
    }

    /**
     * Corresponds to a select SQL statement
     * @param attrs String that contains one attribute/table column
     * @return calling object to enable method chain calling
     */

    CrudModel<T> grab(String... attrs) {
        appliedAttrs.clear();

        try {
            Table table = ref.clas.getAnnotation(Table.class);
            String tableName = table.tableName();

            if (attrs.length == 0) {
                ps = conn.prepareStatement("select * from " + tableName);
                attrFields.stream().forEach(attr -> appliedAttrs.add(attr));
                return ref;
            }

            StringBuilder queryPlaceholders = new StringBuilder();
            String delimiter;

            for (int i=0; i<attrs.length; i++) {
                delimiter = (i < attrs.length - 1) ? ", " : "";

                AttrField currentAttr = getAttributeByColumnName(attrs[i]);

                if (currentAttr != null) {
                    queryPlaceholders.append(attrs[i] + delimiter);
                    appliedAttrs.add(currentAttr);
                }
            }

            ps = conn.prepareStatement("select " + queryPlaceholders.toString() + " from " + tableName);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return ref;
    }

    ArrayList<T> runGrab() {
        if (!ps.toString().startsWith("select")) {
            throw new BadMethodChainCallException("runGrab() can only be called when grab() is the head of the method chain.");
        }

        ArrayList<T> models = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            setAppliedFields(appliedAttrs);

            ResultSetParser<T> mapClas = new ResultSetParser<T>(ref.clas, this);
            models = mapClas.mapResultSet(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            System.out.println(e.getMessage());
        }

        return models;
    }
}
