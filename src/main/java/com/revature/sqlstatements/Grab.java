package com.revature.sqlstatements;

import com.revature.annotations.Table;
import com.revature.utils.AttrField;
import com.revature.utils.CrudModel;
import com.revature.utils.ModelScraper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class Grab<T> extends ModelScraper {
    private PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;

    public Grab (Connection conn, CrudModel<T> ref) {
        this.conn = conn;
        this.ref = ref;
        appliedAttrs = new ArrayList<>();
    }

    /**
     * Corresponds to a select SQL statement
     * @param attrs String that contains one attribute/table column
     * @return calling object to enable method chain calling
     */
    public CrudModel<T> grab(String... attrs) {
        appliedAttrs.clear();

        try {
            Table table = clas.getAnnotation(Table.class);
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
}
