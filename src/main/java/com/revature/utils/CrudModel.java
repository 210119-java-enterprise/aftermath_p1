package com.revature.utils;
import com.revature.annotations.*;
import com.revature.exceptions.*;
import com.sun.istack.internal.Nullable;
import java.lang.String;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CrudModel<T> {
    private Class<T> clas;
    private ArrayList<FKField> fkFields;
    private ArrayList<AttrField> attrFields;
    private ArrayList<AttrField> appliedAttrs; // contains the fields/attributes a user wants to select/insert/update/delete
    private HashMap<String, Savepoint> savepoints; // Hashmap of savepoints
    private ArrayList<Integer> filteredUpdateAttrIndices;
    private Method[] methods;
    protected PreparedStatement ps;
    protected Connection conn;

    public CrudModel(Class<T> clas) {
        this.clas = clas;
        ModelScraper.setTargetClass(clas);
        this.attrFields = new ArrayList<>();
        this.appliedAttrs = new ArrayList<>();
        this.methods = clas.getMethods();
        this.conn = ConnectionFactory.getInstance().getConnection();
        filteredUpdateAttrIndices = new ArrayList<>();
        savepoints = new HashMap<>();
    }

    public void turnOffAutoCommit() throws SQLException {
        conn.setAutoCommit(false);
    }

    public void turnOnAutoCommit() throws SQLException {
        conn.setAutoCommit(true);
    }

    public void runCommit() throws SQLException {
        conn.commit();
    }

    public void addSavepoint(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Savepoint needs an associated name");
        }

        savepoints.put(name, conn.setSavepoint());
    }

    public void removeSavepoint(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not value");
        }

        savepoints.remove(name);
    }

    public void rollback(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not a value");
        }

        Savepoint selectedSavepoint = savepoints.get(name);
        conn.rollback(selectedSavepoint);
    }

    public CrudModel<T> add(String... attrs) {
        ps = null;
        appliedAttrs.clear();

        try {
            Table table = clas.getAnnotation(Table.class);
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

        return this;
    }

    public CrudModel<T> addValues(String... values) throws RuntimeException {
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

        return this;
    }

    public CrudModel<T> change(String... attrs) throws SQLException {
        if (attrs.length == 0) {
            throw new InvalidInputException("change() requires params given that it is constructing an update statement");
        }

        filteredUpdateAttrIndices.clear();

        ps = null;
        Table table = clas.getAnnotation(Table.class);
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

        return this;
    }

    public CrudModel<T> set(String... values) throws SQLException {
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

        return this;
    }

    public CrudModel<T> remove() throws SQLException {
        appliedAttrs.clear();
        ps = null;
        Table table = clas.getAnnotation(Table.class);
        String tableName = table.tableName();
        ps = conn.prepareStatement("delete from " + tableName);
        return this;
    }

    public CrudModel<T> where() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("where() can't be called off of add() since insert statements can't have where clauses");
        }

        if (ps.toString().contains("where")) {
            throw new BadMethodChainCallException("where() can only be called once in a method chain call." +
                    " Use and(), or(), or not()");
        }

        ps = conn.prepareStatement(ps.toString() + " where ");
        return this;
    }

    public CrudModel<T> where(Conditions cond, String attr, String value) throws SQLException {
        where();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public CrudModel<T> and() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("cannot call and() on add() methods");
        }

        if (!ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call and() if there is no where clause");
        }

        ps = conn.prepareStatement(ps.toString() + " and ");
        return this;
    }

    public CrudModel<T> and(Conditions cond, String attr, String value) throws SQLException {
        and();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public CrudModel<T> or() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("cannot call or() on add() methods");
        }

        if (!ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call or() if there is no where clause");
        }

        ps = conn.prepareStatement(ps.toString() + " or ");
        return this;
    }

    public CrudModel<T> or(Conditions cond, String attr, String value) throws SQLException {
        or();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public CrudModel<T> not(Conditions cond, String attr, String value) throws SQLException {
        builtWhereClause(cond, "not ", attr, value);
        return this;
    }

    private void builtWhereClause(Conditions cond, String logicalOp, String attr, String value) throws SQLException {
        AttrField selectedField = null;

        String psStr = ps.toString() + logicalOp;
        switch (cond) {
            case EQUALS:
                ps = conn.prepareStatement(psStr + attr + " = ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case NOT_EQUALS:
                ps = conn.prepareStatement(psStr + attr + " <> ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case GT:
                ps = conn.prepareStatement(psStr + attr + " > ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case LT:
                ps = conn.prepareStatement(psStr + attr + " < ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case GTE:
                ps = conn.prepareStatement(psStr + attr + " >= ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case LTE:
                ps = conn.prepareStatement(psStr + attr + " <= ?");
                selectedField = getAttributeByColumnName(attr);
                break;
        }

        Class<?> type = selectedField.getType();

        if (type == String.class) {
            ps.setString(1, value);
        } else if (type == int.class) {
            ps.setInt(1, Integer.parseInt(value));
        } else if (type == double.class) {
            ps.setDouble(1, Double.parseDouble(value));
        }
    }

    public String getPreparedStatement() {
        return ps.toString();
    }

    public ArrayList<T> runGrab() {
        if (!ps.toString().startsWith("select")) {
            throw new BadMethodChainCallException("runGrab() can only be called when grab() is the head of the method chain.");
        }

        ArrayList<T> models = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            models = mapResultSet(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            System.out.println(e.getMessage());
        }

        return models;
    }

    public int runAdd() throws Exception {
        if (!ps.toString().startsWith("insert")) {
            throw new BadMethodChainCallException("runAdd() can only be called from addValues()");
        }

        String psStr = ps.toString();
        System.out.println(psStr);
        ps = conn.prepareStatement(psStr.substring(0, psStr.length() - 2));

        return ps.executeUpdate();
    }

    public int runChange() throws Exception {
        if (!ps.toString().startsWith("update")) {
            throw new BadMethodChainCallException("runChange() can only be called from set()");
        }

        return ps.executeUpdate();
    }

    public int runRemove() throws Exception {
        if (!ps.toString().startsWith("delete")) {
            throw new BadMethodChainCallException("runRemove() can only be called when remove() is the head of the method chain.");
        }

        return ps.executeUpdate();
    }




}
