package com.revature.utils;

import com.revature.annotations.Attr;
import com.revature.annotations.FK;
import com.revature.annotations.PK;
import com.sun.istack.internal.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ModelScraper {
    protected Class<?> clas;
    protected ArrayList<AttrField> attrFields = new ArrayList<>();
    protected ArrayList<FKField> fkFields = new ArrayList<>();
    protected Method[] methods;
    private ArrayList<AttrField> appliedFields;

    ModelScraper() { }

    protected void setTargetClass(Class<?> clas) {
        this.clas = clas;
        attrFields.clear();
        methods = clas.getMethods();
        fkFields.clear();
        getColumns();
        fkFields = getForeignKeys();
    }

    protected void setAppliedFields(ArrayList<AttrField> appliedFields) {
        this.appliedFields = appliedFields;
    }
    protected void setAttrFields(ArrayList<AttrField> attrFields) { this.attrFields = attrFields; }

    protected ArrayList<AttrField> getAppliedFields() {
        return appliedFields;
    }

    protected ArrayList<FKField> getForeignKeys() {

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

    protected AttrField getAttributeByColumnName(String name) {
        for (AttrField attr : attrFields) {
            if (attr.getColumnName().equals(name)) {
                return attr;
            }
        }

        return null;
    }

    protected PKField getPrimaryKey() {

        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            PK primaryKey = field.getAnnotation(PK.class);
            if (primaryKey != null) {
                return new PKField(field);
            }
        }
        throw new RuntimeException("Did not find a field annotated with @Id in: " + clas.getName());
    }

    protected ArrayList<AttrField> getColumns() {

        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            Attr column = field.getAnnotation(Attr.class);
            if (column != null) {
                attrFields.add(new AttrField(field));
                System.out.println(column.columnName());
            }
        }

        if (attrFields.isEmpty()) {
            throw new RuntimeException("No columns found in: " + clas.getName());
        }

        return attrFields;
    }

    @Nullable
    protected Method getMethodByFieldName(String fieldName) {
        for (Method currentMethod : methods) {
            if (currentMethod.getName().equals(fieldName)) {
                return currentMethod;
            }
        }

        return null;
    }
}
