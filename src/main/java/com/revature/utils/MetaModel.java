package com.revature.utils;
import com.revature.annotations.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MetaModel<T> {
    private Class<T> clas;
    private List<FKField> fkFields;
    private List<AttrField> attrFields;
    private PreparedStatement ps;
    private Connection conn;

    public static <T> MetaModel<T> getModel(Class<T> clas) {
        if (clas.getAnnotation(Table.class) == null) {
            throw new IllegalStateException("Cannot create Metamodel object! Provided class, " + clas.getName() + "is not annotated with @Table");
        }
        return new MetaModel<>(clas);
    }

    public MetaModel(Class<T> clas) {
        this.clas = clas;
        this.attrFields = getColumns();
        //this.fkFields = get;
        this.conn = ConnectionFactory.getInstance().getConnection();
    }

    /**
     * Corresponds to a select SQL statement. The 0-parameter overloaded method is equivalent to a select * from <table> statement
     * @return calling object to enable method chain calling
     */
    public MetaModel<T> grab() {
        ps = null; // clear the prepared statement

        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();

            ps = conn.prepareStatement("select * from ?");
            ps.setString(1, tableName);
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

        try {
            Table table = clas.getAnnotation(Table.class);
            String tableName = table.tableName();
            StringBuilder queryPlaceholders = new StringBuilder();
            String delimiter;

            for (int i=0; i<attrs.length; i++) {
                delimiter = (i < attrs.length - 1) ? ", " : "";
                for (AttrField attr : attrFields) {
                    if (attr.getColumnName().equals(attrs[i])) {
                        queryPlaceholders.append("?" + delimiter);
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

    public List<AttrField> getColumns() {

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

    private void buildMultAttrPreparedStatement(String[] selectedAttrs) {
        for (int i=0; i<selectedAttrs.length; i++) {
            String currentAttrStr = selectedAttrs[i];

            for (AttrField attr : attrFields) {
                if (attr.getColumnName().equals(currentAttrStr)) {
                    try {
                        String fieldTypeStr = attr.getType().getSimpleName();
                        if (fieldTypeStr.equals("String")) {
                            ps.setString(i + 1, selectedAttrs[i]);
                        } else if (fieldTypeStr.equals("int")) {
                            ps.setInt(i + 1, Integer.parseInt(selectedAttrs[i]));
                        } else if (fieldTypeStr.equals("double")) {
                            ps.setDouble(i + 1, Double.parseDouble(selectedAttrs[i]));
                        }
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                }
            }
        }
    }

    public void save() {
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("firstname"));
                System.out.println(rs.getString("lastname"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*private ArrayList<T> mapResults(ResultSet rs) {
        ArrayList<T> list = new ArrayList<>();
    }*/
}