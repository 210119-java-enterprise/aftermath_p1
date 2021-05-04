package com.revature.utils;

import com.revature.annotations.Table;
import com.revature.exceptions.BadMethodChainCallException;
import com.revature.exceptions.InvalidInputException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

class Change<T> extends ModelScraper {
    PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;
    private ArrayList<Integer> filteredUpdateAttrIndices;

    Change (CrudModel<T> ref) {
        setTargetClass(ref.clas);
        this.conn = ref.conn;
        this.ref = ref;
        appliedAttrs = new ArrayList<>();
        filteredUpdateAttrIndices = new ArrayList<>();
    }

    CrudModel<T> change(String... attrs) throws SQLException {
        if (attrs.length == 0) {
            throw new InvalidInputException("change() requires params given that it is constructing an update statement");
        }

        filteredUpdateAttrIndices.clear();

        ps = null;
        Table table = ref.clas.getAnnotation(Table.class);
        String tableName = table.tableName();
        appliedAttrs.clear();
        int count = 0;

        for (String attr : attrs) {
            AttrField currentField = getAttributeByColumnName(attr);

            if (currentField != null) {
                appliedAttrs.add(currentField);
            } else {
                filteredUpdateAttrIndices.add(count);
            }
            ++count;
        }

        StringBuilder setString = new StringBuilder("update " + tableName + " set ");
        appliedAttrs.stream().forEach(attr -> { setString.append(attr.getColumnName()); setString.append(" = ?, "); });
        ps = conn.prepareStatement(setString.toString().substring(0, setString.length()-2));

        return ref;
    }

    CrudModel<T> set(String... values) throws SQLException {
        String psStr = ps.toString();

        if (!psStr.startsWith("update")) {
            throw new BadMethodChainCallException("set() can only be called off of change()");
        }

        if (!psStr.contains("?")) {
            throw new BadMethodChainCallException("cannot call set() off of change() twice");
        }

        ArrayList<String> filteredValues = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            if (filteredUpdateAttrIndices.contains(i)) {
                continue;
            }

            filteredValues.add(values[i]);
        }

        for (int i = 0; i < appliedAttrs.size(); i++) {
            AttrField currentAttr = appliedAttrs.get(i);
            Class<?> type = currentAttr.getType();

            if (type == String.class) {
                ps.setString(i+1, filteredValues.get(i));
            } else if (type == int.class) {
                ps.setInt(i+1, Integer.parseInt(filteredValues.get(i)));
            } else if (type == double.class) {
                ps.setDouble(i+1, Double.parseDouble(filteredValues.get(i)));
            }
        }

        return ref;
    }

    int runChange() throws Exception {
        if (!ps.toString().startsWith("update")) {
            throw new BadMethodChainCallException("runChange() can only be called from set()");
        }

        return ps.executeUpdate();
    }
}
