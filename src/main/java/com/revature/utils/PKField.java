package com.revature.utils;

import java.lang.reflect.Field;
import com.revature.annotations.PK;

public class PKField {
    private Field pkField;

    public PKField(Field field) {
        if (field.getAnnotation(PK.class) == null) {
            throw new IllegalStateException("@PK annotation not set! Aftermath can't create your primary key.");
        }

        pkField = field;
    }

    public String getName() {
        return pkField.getName();
    }

    public Class<?> getType() {
        return pkField.getType();
    }

    public String getColumnName() {
        return pkField.getAnnotation(PK.class).columnName();
    }
}
