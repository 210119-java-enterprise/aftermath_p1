package com.revature.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

class ResultSetParser<T> {
    private Class<T> clas;
    private Grab<T> operator;

    ResultSetParser(Class<T> clas, Grab<T> operator) {
        this.clas = clas;
        this.operator = operator;
    }

    ArrayList<T> mapResultSet(ResultSet rs) throws SQLException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        T model;
        PKField pkField = operator.getPrimaryKey();

        ArrayList<T> models = new ArrayList<>();
        ArrayList<FKField> fkFields = operator.fkFields;

        while (rs.next()) {
            model = clas.newInstance();
            char[] pkNameArr = pkField.getName().toCharArray();
            pkNameArr[0] = Character.toUpperCase(pkNameArr[0]);
            String pkName = String.valueOf(pkNameArr);
            Method pkSetId = operator.getMethodByFieldName("set" + pkName);

            try {
                int pkk = rs.getInt(pkField.getColumnName());
                pkSetId.invoke(model, pkk);
            } catch (SQLException | InvocationTargetException e) {/* do nothing */}

            for (FKField fk: fkFields) {
                String FKName = fk.getName();
                char[] setterFKNameArr = FKName.toCharArray();

                setterFKNameArr[0] = Character.toUpperCase(setterFKNameArr[0]);
                FKName = String.valueOf(setterFKNameArr);
                Method fkSetId = operator.getMethodByFieldName("set" + FKName);

                try {
                    System.out.println(FKName);
                    fkSetId.invoke(model, rs.getInt(fk.getColumnName()));
                } catch (SQLException | InvocationTargetException e) {
                    // do nothing; added try-catch block since ResultSet throws an exception if the params aren't
                    // in the result set. Sometimes, we don't need to retrieve a FK
                }
            }

            for (AttrField selectedAttr: operator.getAppliedFields()) {
                Class<?> type = selectedAttr.getType();
                char[] getterAttrNameArr = selectedAttr.getName().toCharArray();
                getterAttrNameArr[0] = Character.toUpperCase(getterAttrNameArr[0]);
                String attrMethodName = String.valueOf(getterAttrNameArr);
                Method setAttr = operator.getMethodByFieldName("set" + attrMethodName);

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
