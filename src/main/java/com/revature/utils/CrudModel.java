package com.revature.utils;
import com.revature.annotations.*;
import com.revature.exceptions.*;
import java.lang.String;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class acts as the hub between the user and the JDBC.
 * @param <T> Type of a user-defined annotated model
 */
public class CrudModel<T> {
    private Class<T> clas;
    private ArrayList<FKField> fkFields;
    private ArrayList<AttrField> attrFields;
    private ArrayList<AttrField> appliedAttrs; // contains the fields/attributes a user wants to select/insert/update/delete
    private HashMap<String, Savepoint> savepoints; // Hashmap of savepoints
    private ArrayList<Integer> filteredUpdateAttrIndices;
    private Method[] methods;
    private PreparedStatement ps;
    private Connection conn;

    /**
     * The constructor creates all the necessary instance-level objects needed for CRUD operations
     * @param clas Class of the user's model/POJO
     */
    public CrudModel(Class<T> clas) {
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

    /**
     * Turns off auto commit. Call this method during transactional CRUD operations
     * @throws SQLException
     */
    public void turnOffAutoCommit() throws SQLException {
        conn.setAutoCommit(false);
    }

    /**
     * Turns on auto commit
     * @throws SQLException
     */
    public void turnOnAutoCommit() throws SQLException {
        conn.setAutoCommit(true);
    }

    /**
     * Execute a commit to the database
     * @throws SQLException
     */
    public void runCommit() throws SQLException {
        conn.commit();
    }

    /**
     * Creates a savepoint
     * @param name the name of the savepoint; pass the value of this savepoint's name into a rollback() method
     * @throws SQLException
     */
    public void addSavepoint(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Savepoint needs an associated name");
        }

        savepoints.put(name, conn.setSavepoint());
    }

    /**
     * Remove a savepoint
     * @param name
     */
    public void removeSavepoint(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not value");
        }

        savepoints.remove(name);
    }

    /**
     * Triggers a rollback to the latest savepoint
     * @param name The name of the savepoint you are trying to rollback to
     * @throws SQLException
     */
    public void rollback(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not a value");
        }

        Savepoint selectedSavepoint = savepoints.get(name);
        conn.rollback(selectedSavepoint);
    }

    /**
     * Corresponds to a select SQL statement
     * @param attrs separate string arguments or a string array that contains one or more attribute/table column
     * @return calling object to enable method chain calling
     */

    public CrudModel<T> grab(String... attrs) {
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

    /**
     * Corresponds to an insert statement.
     * @param attrs separate string arguments or a string array that contains one or more attribute/table column
     * @return calling object to enable method chain calling
     */
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

    /**
     * Used in conjunction with add() and previous addValues()
     * @param values the values to insert into a table row
     * @return separate string arguments or a string array that contains one or more attribute/table column
     * @throws RuntimeException
     */
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

    /**
     * Corresponds to an update statement
     * @param attrs separate string arguments or a string array that contains one or more attribute/table column
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
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

    /**
     * Used in conjunction with change()
     * @param values separate string arguments or a string array that contains one or more attribute/table column
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
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

    /**
     * Corresponds to a delete statement
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
    public CrudModel<T> remove() throws SQLException {
        appliedAttrs.clear();
        ps = null;
        Table table = clas.getAnnotation(Table.class);
        String tableName = table.tableName();
        ps = conn.prepareStatement("delete from " + tableName);
        return this;
    }

    /**
     * Corresponds to a where clause. Use in conjunction with grab(), change(), and remove(). Call the
     * zero param method if you intend on using the not() method right after this
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
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

    /**
     * A where clause. Use in conjunction with grab(), change(), and delete()
     * @param cond Enum value that represents an inequality operator
     * @param attr table column
     * @param value table values
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
    public CrudModel<T> where(Conditions cond, String attr, String value) throws SQLException {
        where();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    /**
     * Logical AND. Use zero param if not() is called after this
     * @return calling object to enable method chain calling
     * @throws SQLException
     */
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

    /**
     * Logical AND
     * @param cond Enum value that represents an inequality operator
     * @param attr Table column
     * @param value Table value
     * @return Calling object to enable method chain calling
     * @throws SQLException
     */
    public CrudModel<T> and(Conditions cond, String attr, String value) throws SQLException {
        and();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    /**
     * Logical OR. Call zero param if not() is called after this
     * @return
     * @throws SQLException
     */
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

    /**
     * Logical OR
     * @param cond Enum value that represents an inequality operator
     * @param attr Table column
     * @param value Table value
     * @return Calling object to enable method chain calling
     * @throws SQLException
     */
    public CrudModel<T> or(Conditions cond, String attr, String value) throws SQLException {
        or();
        builtWhereClause(cond, "", attr, value);
        return this;
    }

    /**
     * Logical NOT
     * @param cond Enum value that represents an inequality operator
     * @param attr Table column
     * @param value Table value
     * @return Calling object to enable method chain calling
     * @throws SQLException
     */
    public CrudModel<T> not(Conditions cond, String attr, String value) throws SQLException {
        builtWhereClause(cond, "not ", attr, value);
        return this;
    }

    /**
     * Helper method to where(), or(), and(), and not(). This method parses the field data types
     * @param cond
     * @param logicalOp
     * @param attr
     * @param value
     * @throws SQLException
     */
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

    /**
     * Didn't have time to implement joins. DON'T USE
     * @param tableName
     * @return
     * @throws SQLException
     */
    public CrudModel<T> join(String tableName) throws SQLException {
        if (!ps.toString().startsWith("select")) {
            throw new BadMethodChainCallException("join() can only be called from a grab()");
        }

        if (ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call join() after where() is called.");
        }

        ps = conn.prepareStatement(ps.toString() + " join " + tableName + " ");

        return this;
    }

    public CrudModel<T> on (String... keys) throws SQLException {
        if (!ps.toString().startsWith("select")) {
            throw new BadMethodChainCallException("join() can only be called from a grab()");
        }

        if (ps.toString().contains("where")) {
            throw new BadMethodChainCallException("cannot call join() after where() is called.");
        }

        if (keys.length < 2) {
            throw new InvalidInputException("on() required two key params.");
        }

        PKField pkField = getPrimaryKey();
        int validKeys = 0;

        for (String key : keys) {
            if (pkField.getColumnName().equals(key)) {
                ++validKeys;
                continue;
            }
            else {
                ArrayList<FKField> fkFields = getForeignKeys();

                for (FKField fk : fkFields) {
                    if (fk.getColumnName().equals(key)) {
                        ++validKeys;
                        break;
                    }
                }
            }
        }

        if (validKeys < 2) {
            throw new InvalidInputException("One or both of the passed in values are not primary or foreign keys");
        }

        ps = conn.prepareStatement(ps.toString() + " on (? = ?) ");
        ps.setString(1, keys[0]);
        ps.setString(2, keys[1]);
        removeSingleQuotesFromJoin();

        return this;
    }

    /**
     * Didn't get around to implementing joins. DON'T USE
     * @param key
     * @return
     * @throws SQLException
     */
    public CrudModel<T> using(String key) throws SQLException {
        if (!ps.toString().startsWith("select"))
        {
            throw new BadMethodChainCallException("join() can only be called from a grab()");
        }

        if (ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call join() after where() is called.");
        }

        PKField pkField = getPrimaryKey();

        if (pkField.getColumnName().equals(key)) {
            ps = conn.prepareStatement(ps.toString() + " using (?) ");
            ps.setString(1, key);
            removeSingleQuotesFromJoin();
            return this;
        } else {
            ArrayList<FKField> fkfields = getForeignKeys();

            for (FKField fk: fkFields) {
                if (fk.getColumnName().equals(key)) {
                    ps = conn.prepareStatement(ps.toString() + " using (?) ");
                    ps.setString(1, key);
                    removeSingleQuotesFromJoin();
                    return this;
                }
            }
            throw new InvalidInputException("Passed in value is not a primary or foreign key");
        }
    }

    /**
     * Prepared statements tend to insert single quotes around their string arguments. This is good
     * for regular arguments, but not for arguments that represent a column/attribute value.
     * This method will remove those annoying single quotes
     */
    private void removeSingleQuotesFromJoin() throws SQLException {
        String psStr = ps.toString();
        boolean contains = psStr.contains("'");

        while (contains) {
            psStr = psStr.replace("'","");
            contains = psStr.contains("'");
        }

        ps = conn.prepareStatement(psStr);
    }

    /**
     *
     * @return list of all the foreign keys
     */
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

    /**
     *
     * @param name column name
     * @return Attribute/column that matches name
     */
    private AttrField getAttributeByColumnName(String name) {
        for (AttrField attr : attrFields) {
            if (attr.getColumnName().equals(name)) {
                return attr;
            }
        }

        return null;
    }

    /**
     *
     * @return primary key
     */
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

    /**
     *
     * @return list of annotated columns
     */
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

    /**
     *
     * @return SQL statement generated by JDBC. Used mostly in integration tests
     */
    public String getPreparedStatement() {
        return ps.toString();
    }

    /**
     * Executes insert statement
     * @return ArrayList of models
     */
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

    /**
     * Executes insert statement
     * @return number of rows inserted
     * @throws Exception
     */
    public int runAdd() throws Exception {
        if (!ps.toString().startsWith("insert")) {
            throw new BadMethodChainCallException("runAdd() can only be called from addValues()");
        }

        String psStr = ps.toString();
        System.out.println(psStr);
        ps = conn.prepareStatement(psStr.substring(0, psStr.length() - 2));

        return ps.executeUpdate();
    }

    /**
     * EXecutes update statement
     * @return number of rows updated
     * @throws Exception
     */
    public int runChange() throws Exception {
        if (!ps.toString().startsWith("update")) {
            throw new BadMethodChainCallException("runChange() can only be called from set()");
        }

        return ps.executeUpdate();
    }

    /**
     * Executes delete statements
     * @return number of rows deleted
     * @throws Exception
     */
    public int runRemove() throws Exception {
        if (!ps.toString().startsWith("delete")) {
            throw new BadMethodChainCallException("runRemove() can only be called when remove() is the head of the method chain.");
        }

        return ps.executeUpdate();
    }

    /**
     *
     * @param fieldName
     * @return method that matches param
     */
    private Method getMethodByFieldName(String fieldName) {
        for (Method currentMethod : methods) {
            if (currentMethod.getName().equals(fieldName)) {
                return currentMethod;
            }
        }

        return null;
    }

    /**
     * Grabs the results from ResultSet, and calls the models setters and getters via Reflection
     * Dependent on users using standard setter and getter names
     * Rationale - It's common place for Java developers to autogenerate their setters and getters, so this
     * method is taking advantage of the naming convention these IDE generators use
     * @param rs ResultSet
     * @return Arraylist of the user's models
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
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
