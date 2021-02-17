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

public class MetaModel<T> {
    private Class<T> clas;
    private ArrayList<FKField> fkFields;
    private ArrayList<AttrField> attrFields;
    private ArrayList<AttrField> appliedAttrs; // contains the fields/attributes a user wants to select/insert/update/delete
    private HashMap<String, Savepoint> savepoints; // Hashmap of savepoints
    private ArrayList<Integer> filteredUpdateAttrIndices;
    private Method[] methods;
    private PreparedStatement ps;
    private Connection conn;

    public MetaModel(Class<T> clas) {
        this.clas = clas;
        this.attrFields = new ArrayList<>();
        getColumns();
        this.appliedAttrs = new ArrayList<>();
        this.methods = clas.getMethods();
        this.fkFields = getForeignKeys();
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
            throw new InvalidInputException("Key is not value");
        }

        Savepoint selectedSavepoint = savepoints.get(name);
        conn.rollback(selectedSavepoint);
    }

    /**
     * Corresponds to a select SQL statement
     * @param attrs String that contains one attribute/table column
     * @return calling object to enable method chain calling
     */

    public MetaModel<T> grab(String... attrs) {
        ps = null; // clear the prepared statement
        appliedAttrs.clear();

        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();

            if (attrs.length == 0) {
                ps = conn.prepareStatement("select * from " + tableName);
                attrFields.stream().forEach(attr -> appliedAttrs.add(attr));
                return this;
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

        return this;
    }

    public MetaModel<T> add(String... attrs) {
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

    public MetaModel<T> addValues(String... values) throws RuntimeException {
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

    public MetaModel<T> change(String... attrs) throws SQLException {
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

    public MetaModel<T> set(String... values) throws SQLException {
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

    public MetaModel<T> remove() throws SQLException {
        appliedAttrs.clear();
        ps = null;
        Table table = clas.getAnnotation(Table.class);
        String tableName = table.tableName();
        ps = conn.prepareStatement("delete from " + tableName);
        return this;
    }

    public MetaModel<T> where() throws SQLException {
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

    public MetaModel<T> where(Conditions cond, String attr, String value) throws SQLException {
        where();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public MetaModel<T> and() throws SQLException {
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

    public MetaModel<T> and(Conditions cond, String attr, String value) throws SQLException {
        and();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public MetaModel<T> or() throws SQLException {
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

    public MetaModel<T> or(Conditions cond, String attr, String value) throws SQLException {
        or();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    public MetaModel<T> not(Conditions cond, String attr, String value) throws SQLException {
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

    public ArrayList<FKField> getForeignKeys() {

        ArrayList<FKField> foreignKeyFields = new ArrayList<>();
        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            FK attr = field.getAnnotation(FK.class);
            if (attr != null) {
                foreignKeyFields.add(new FKField(field));
            }
        }

        return foreignKeyFields;
    }

    private AttrField getAttributeByColumnName(String name) {
        for (AttrField attr : attrFields) {
            if (attr.getColumnName().equals(name)) {
                return attr;
            }
        }

        return null;
    }

    public PKField getPrimaryKey() {

        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            PK primaryKey = field.getAnnotation(PK.class);
            if (primaryKey != null) {
                return new PKField(field);
            }
        }
        throw new RuntimeException("Did not find a field annotated with @Id in: " + clas.getName());
    }

    public ArrayList<AttrField> getColumns() {

        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            Attr column = field.getAnnotation(Attr.class);
            if (column != null) {
                attrFields.add(new AttrField(field));
            }
        }

        if (attrFields.isEmpty()) {
            throw new RuntimeException("No columns found in: " + clas.getName());
        }

        return attrFields;
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

    @Nullable
    private Method getMethodByFieldName(String fieldName) {
        for (Method currentMethod : methods) {
            if (currentMethod.getName().equals(fieldName)) {
                return currentMethod;
            }
        }

        return null;
    }

    private ArrayList<T> mapResultSet(ResultSet rs) throws SQLException, IllegalAccessException,
                                                           InstantiationException, InvocationTargetException {
        T model;
        PKField pkField = getPrimaryKey();
        ArrayList<T> models = new ArrayList<>();

        while (rs.next()) {
            model = clas.newInstance();
            char[] pkNameArr = pkField.getName().toCharArray();
            pkNameArr[0] = Character.toUpperCase(pkNameArr[0]);
            String pkName = String.valueOf(pkNameArr);
            Method pkSetId = getMethodByFieldName("set" + pkName);

            try {
                int pkk = rs.getInt(pkField.getColumnName());
                pkSetId.invoke(model, pkk);
            } catch (SQLException | InvocationTargetException e) {/* do nothing */}

            for (FKField fk: fkFields) {
                String FKName = fk.getName();
                char[] setterFKNameArr = FKName.toCharArray();

                setterFKNameArr[0] = Character.toUpperCase(setterFKNameArr[0]);
                FKName = String.valueOf(setterFKNameArr);
                Method fkSetId = getMethodByFieldName("set" + FKName);

                try {
                    fkSetId.invoke(model, rs.getInt(fk.getColumnName()));
                } catch (SQLException | InvocationTargetException e) {
                    // do nothing; added try-catch block since ResultSet throws an exception if the params aren't
                    // in the result set. Sometimes, we don't need to retrieve a FK
                }
            }

            for (AttrField selectedAttr: appliedAttrs) {
                Class<?> type = selectedAttr.getType();
                char[] getterAttrNameArr = selectedAttr.getName().toCharArray();
                getterAttrNameArr[0] = Character.toUpperCase(getterAttrNameArr[0]);
                String attrMethodName = String.valueOf(getterAttrNameArr);
                Method setAttr = getMethodByFieldName("set" + attrMethodName);

                if (type == String.class) {
                    setAttr.invoke(model, rs.getString(selectedAttr.getColumnName()));
                } else if (type == int.class) {
                    setAttr.invoke(model, rs.getInt(selectedAttr.getColumnName()));
                } else if (type == double.class) {
                    setAttr.invoke(model, rs.getDouble(selectedAttr.getColumnName()));
                }
            }
            models.add(model);
        }

        return models;
    }
}
