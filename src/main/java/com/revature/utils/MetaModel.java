package com.revature.utils;
import com.revature.annotations.*;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

public class MetaModel<T> {
    private Class<T> clas;
    private List<FKField> fkFields;
    private List<AttrField> attrFields;

    public static <T> MetaModel<T> getModel(Class<T> clas) {
        if (clas.getAnnotation(Table.class) == null) {
            throw new IllegalStateException("Cannot create Metamodel object! Provided class, " + clas.getName() + "is not annotated with @Table");
        }
        return new MetaModel<>(clas);
    }

    public MetaModel(Class<T> clas) {
        this.clas = clas;
        this.attrFields = new LinkedList<>();
        this.fkFields = new LinkedList<>();
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
}