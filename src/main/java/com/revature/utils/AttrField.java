package com.revature.utils;

import java.lang.reflect.Field;
import com.revature.annotations.Attr;

public class AttrField {
    private Field attrField;

    public AttrField(Field field) {
        if (field.getAnnotation(Attr.class) == null) {
            throw new IllegalStateException("@Attr annotation not set! Aftermath can't create your attribute.");
        }

        attrField = field;
    }

    public String getName() {
        return attrField.getName();
    }

    public Class<?> getType() {
        return attrField.getType();
    }

    public String getColumnName() {
        return attrField.getAnnotation(Attr.class).columnName();
    }
}
