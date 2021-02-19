package com.revature.utils;

import com.revature.annotations.Attr;
import com.revature.annotations.FK;
import com.revature.annotations.PK;
import com.sun.istack.internal.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ModelScraper {
    protected static Class<?> clas;
    protected static ArrayList<AttrField> attrFields = new ArrayList<>();
    protected static ArrayList<FKField> fkFields = new ArrayList<>();
    protected static ArrayList<Method> methods = new ArrayList<>();
    private static ArrayList<AttrField> appliedFields;

    protected static void setTargetClass(Class<?> clas) {
        ModelScraper.clas = clas;
        attrFields.clear();
        methods.clear();
        fkFields.clear();
        getColumns();
        fkFields = getForeignKeys();
    }

    protected static void setAppliedFields(ArrayList<AttrField> appliedFields) {
        ModelScraper.appliedFields = appliedFields;
    }

    protected static ArrayList<AttrField> getAppliedFields() {
        return appliedFields;
    }

    protected static ArrayList<FKField> getForeignKeys() {

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

    protected static AttrField getAttributeByColumnName(String name) {
        for (AttrField attr : attrFields) {
            if (attr.getColumnName().equals(name)) {
                return attr;
            }
        }

        return null;
    }

    protected static PKField getPrimaryKey() {

        Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            PK primaryKey = field.getAnnotation(PK.class);
            if (primaryKey != null) {
                return new PKField(field);
            }
        }
        throw new RuntimeException("Did not find a field annotated with @Id in: " + clas.getName());
    }

    protected static ArrayList<AttrField> getColumns() {

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

    @Nullable
    protected static Method getMethodByFieldName(String fieldName) {
        for (Method currentMethod : methods) {
            if (currentMethod.getName().equals(fieldName)) {
                return currentMethod;
            }
        }

        return null;
    }
}
