package com.revature.utils;

import com.revature.annotations.Table;
import com.revature.exceptions.BadMethodChainCallException;
import com.revature.exceptions.InvalidInputException;
import com.revature.exceptions.MismatchedInsertArgumentsException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

class Add<T> extends ModelScraper {
    PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;

    Add (CrudModel<T> ref) {
        setTargetClass(ref.clas);
        this.conn = ref.conn;
        this.ref = ref;
        this.ps = ref.ps;
        appliedAttrs = new ArrayList<>();
    }

    CrudModel<T> add(String... attrs) {
        if (attrs.length == 0) {
            throw new InvalidInputException("add() requires at least one input");
        }

        ps = null;
        appliedAttrs.clear();

        try {
            Table table = ref.clas.getAnnotation(Table.class);
            ArrayList<String> attrFilter = new ArrayList<>();
            StringBuilder queryPlaceholders = new StringBuilder();
            String tableName = table.tableName();
            String delimiter;

            for (String attrStr: attrs) {
                attrFields.stream()
                        .filter(attr -> attr.getColumnName().equals(attrStr))
                        .forEach(attr -> { attrFilter.add(attrStr); appliedAttrs.add(attr); });
            }

            for (int i=0; i<attrFilter.size(); i++) {
                delimiter = (i < attrFilter.size() - 1) ? ", " : "";
                queryPlaceholders.append(attrFilter.get(i) + delimiter);
            }

            ps = conn.prepareStatement("insert into " + tableName
                    + " (" + queryPlaceholders.toString() + ") values ");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return ref;
    }

    CrudModel<T> addValues(String... values) throws RuntimeException {
        if (values.length != appliedAttrs.size()) {
            throw new MismatchedInsertArgumentsException();
        }

        if (!ps.toString().startsWith("insert")) {
            throw new BadMethodChainCallException("addValues() needs to be called off of either an add() method"
                    + " or another addValues() method.");
        }

        StringBuilder rowInsertPlaceholder = new StringBuilder();
        String delimiter;

        for (int i=0; i<values.length; i++) {
            delimiter = (i < values.length - 1) ? ", " : "";
            rowInsertPlaceholder.append("?" + delimiter);
        }

        try {
            String psStr = ps.toString();
            ps = conn.prepareStatement(psStr + "(" + rowInsertPlaceholder.toString() + "), ");

            for (int i=0; i<appliedAttrs.size(); i++) {
                Class<?> type = appliedAttrs.get(i).getType();

                if (type == String.class) {
                    ps.setString(i+1, values[i]);
                } else if (type == int.class) {
                    ps.setInt(i+1, Integer.parseInt(values[i]));
                } else if (type == double.class) {
                    ps.setDouble(i+1, Double.parseDouble(values[i]));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return ref;
    }

    int runAdd() throws Exception {
        if (!ps.toString().startsWith("insert")) {
            throw new BadMethodChainCallException("runAdd() can only be called from addValues()");
        }

        String psStr = ps.toString();
        ps = conn.prepareStatement(psStr.substring(0, psStr.length() - 2));

        return ps.executeUpdate();
    }
}
