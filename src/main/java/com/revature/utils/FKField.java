package com.revature.utils;

import java.lang.reflect.Field;
import com.revature.annotations.FK;

public class FKField {
    private Field fkField;

    public FKField(Field field) {
        if (field.getAnnotation(FK.class) == null) {
            throw new IllegalStateException("@FK annotation not set! Aftermath can't create your foreign key.");
        }

        fkField = field;
    }

    public String getName() {
        return fkField.getName();
    }

    public Class<?> getType() {
        return fkField.getType();
    }

    public String getColumnName() {
        return fkField.getAnnotation(FK.class).columnName();
    }
}
