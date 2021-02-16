package com.revature.utils;
import com.revature.annotations.*;
import com.revature.exceptions.BadMethodChainCallException;
import com.revature.exceptions.MismathedInsertArgumentsException;
import com.sun.istack.internal.Nullable;
import java.lang.String;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MetaModel<T> {
    private Class<T> clas;
    private ArrayList<FKField> fkFields;
    private ArrayList<AttrField> attrFields;
    private ArrayList<AttrField> appliedAttrs; // contains the fields/attributes a user wants to select/insert/update/delete
    private Method[] methods;
    private PreparedStatement ps;
    private Connection conn;

    public MetaModel(Class<T> clas) {
        this.clas = clas;
        this.attrFields = new ArrayList<AttrField>();
        getColumns();
        this.appliedAttrs = new ArrayList<>();
        this.methods = clas.getMethods();
        this.fkFields = getForeignKeys();
        this.conn = ConnectionFactory.getInstance().getConnection();
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

    /**
     * Corresponds to a select SQL statement. The 0-parameter overloaded method is equivalent to a select * from <table> statement
     * @return calling object to enable method chain calling
     */
    public MetaModel<T> grab() {
        ps = null; // clear the prepared statement
        appliedAttrs.clear();

        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();
            ps = conn.prepareStatement("select * from " + tableName);

            for (AttrField attr : attrFields) {
                appliedAttrs.add(attr);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return this;
    }

    /**
     * Corresponds to a select SQL statement
     * @param attr String that contains one attribute/table column
     * @return calling object to enable method chain calling
     */
    public MetaModel<T> grab(String attr) {
        ps = null; // clear the prepared statement

        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();
            int psi = 1; // prepared statement index

            if (attr.isEmpty()) {
                ps = conn.prepareStatement("select * from ?");
            } else {
                ps = conn.prepareStatement("select ? from ?");
                ps.setString(psi++, "*");
            }

            ps.setString(psi, tableName);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return this;
    }

    public MetaModel<T> grab(String[] attrs) {
        ps = null; // clear the prepared statement
        appliedAttrs.clear();
        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();
            StringBuilder queryPlaceholders = new StringBuilder();
            String delimiter;

            for (int i=0; i<attrs.length; i++) {
                delimiter = (i < attrs.length - 1) ? ", " : "";
                for (AttrField attr : attrFields) {
                    if (attr.getColumnName().equals(attrs[i])) {
                        queryPlaceholders.append(attrs[i] + delimiter);
                        appliedAttrs.add(attr);
                        break;
                    }
                }
            }

            ps = conn.prepareStatement("select " + queryPlaceholders.toString() + " from " + tableName);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return this;
    }

    public MetaModel<T> add(String[] attrs) {
        ps = null;
        appliedAttrs.clear();

        try {
            Table table = clas.getAnnotation(Table.class);
            ArrayList<String> attrFilter = new ArrayList<>();
            StringBuilder queryPlaceholders = new StringBuilder();
            String tableName = table.tableName();
            String delimiter;

            for (int i=0; i<attrs.length; i++) {
                for (AttrField attr : attrFields) {
                    if (attr.getColumnName().equals(attrs[i])) {
                        attrFilter.add(attrs[i]);
                        appliedAttrs.add(attr);
                        break;
                    }
                }
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

    public MetaModel<T> addValues(String[] values) throws RuntimeException {
        if (values.length != appliedAttrs.size()) {
            throw new MismathedInsertArgumentsException();
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

    public String getClassName() {
        return clas.getName();
    }

    public String getSimpleClassName() {
        return clas.getSimpleName();
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
        ArrayList<T> models = new ArrayList<>();
        try {
            ResultSet rs = ps.executeQuery();
            T model;
            PKField pkField = getPrimaryKey();

            while (rs.next()) {
                model = clas.newInstance();
                char[] pkNameArr = pkField.getName().toCharArray();
                pkNameArr[0] = Character.toUpperCase(pkNameArr[0]);
                String pkName = String.valueOf(pkNameArr);
                Method pkSetId = getMethodByFieldName("set" + pkName);

                try {
                    int pkk = rs.getInt(pkField.getColumnName());
                    pkSetId.invoke(model, pkk);
                } catch (SQLException e) {
                    // do nothing; added try-catch block since ResultSet throws an exception if the params aren't
                    // in the result set. Sometimes, we don't need to retrieve a PK
                }

                for (FKField fk: fkFields) {
                    String FKName = fk.getName();
                    char[] setterFKNameArr = FKName.toCharArray();

                    setterFKNameArr[0] = Character.toUpperCase(setterFKNameArr[0]);
                    FKName = String.valueOf(setterFKNameArr);
                    Method fkSetId = getMethodByFieldName("set" + FKName);
                    fkSetId.invoke(model, rs.getInt(fk.getColumnName()));
                }

                for (AttrField selectedAttr: appliedAttrs) {
                    Class<?> type = selectedAttr.getType();
                    char[] getterAttrNameArr = selectedAttr.getName().toCharArray();
                    getterAttrNameArr[0] = Character.toUpperCase(getterAttrNameArr[0]);
                    String attrMethodName = "set" + String.valueOf(getterAttrNameArr);
                    Method setAttr = getMethodByFieldName(attrMethodName);

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
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            System.out.println(e.getMessage());
        }

        return models;
    }

    public ArrayList<T> runAdd() throws Exception {
        if (!ps.toString().startsWith("insert")) {
            throw new BadMethodChainCallException("runAdd() can only be called from addValues()");
        }

        String psStr = ps.toString();
        ArrayList<T> insertedRows = new ArrayList<>();
        ps = conn.prepareStatement(psStr.substring(0, psStr.length() - 2));

        int rowsInserted = ps.executeUpdate();

        if (rowsInserted != 0) {
            ResultSet rs = ps.getGeneratedKeys();
            T model;
            PKField pkField = getPrimaryKey();

            while (rs.next()) {
                model = clas.newInstance();
                char[] pkNameArr = pkField.getName().toCharArray();
                pkNameArr[0] = Character.toUpperCase(pkNameArr[0]);
                String pkName = String.valueOf(pkNameArr);
                Method pkSetId = getMethodByFieldName("set" + pkName);

                try {
                    int pkk = rs.getInt(pkField.getColumnName());
                    pkSetId.invoke(model, pkk);
                } catch (SQLException e) {
                    // do nothing; added try-catch block since ResultSet throws an exception if the params aren't
                    // in the result set. Sometimes, we don't need to retrieve a PK
                }

                for (AttrField selectedAttr: appliedAttrs) {
                    Class<?> type = selectedAttr.getType();
                    char[] getterAttrNameArr = selectedAttr.getName().toCharArray();
                    getterAttrNameArr[0] = Character.toUpperCase(getterAttrNameArr[0]);
                    String attrMethodName = "set" + String.valueOf(getterAttrNameArr);
                    Method setAttr = getMethodByFieldName(attrMethodName);

                    if (type == String.class) {
                        setAttr.invoke(model, rs.getString(selectedAttr.getColumnName()));
                    } else if (type == int.class) {
                        setAttr.invoke(model, rs.getInt(selectedAttr.getColumnName()));
                    } else if (type == double.class) {
                        setAttr.invoke(model, rs.getDouble(selectedAttr.getColumnName()));
                    }
                }
                insertedRows.add(model);
            }

            return insertedRows;
        }

        return null;
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
}