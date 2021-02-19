package com.revature.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ResultSetParser<T> {
    private Class<T> clas;

    protected ResultSetParser(Class<T> clas) {
        this.clas = clas;
    }

    protected ArrayList<T> mapResultSet(ResultSet rs) throws SQLException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        T model;
        PKField pkField = ModelScraper.getPrimaryKey();
        ArrayList<T> models = new ArrayList<>();
        ArrayList<FKField> fkFields = ModelScraper.fkFields;

        while (rs.next()) {
            model = clas.newInstance();
            char[] pkNameArr = pkField.getName().toCharArray();
            pkNameArr[0] = Character.toUpperCase(pkNameArr[0]);
            String pkName = String.valueOf(pkNameArr);
            Method pkSetId = ModelScraper.getMethodByFieldName("set" + pkName);

            try {
                int pkk = rs.getInt(pkField.getColumnName());
                pkSetId.invoke(model, pkk);
            } catch (SQLException | InvocationTargetException e) {/* do nothing */}

            for (FKField fk: fkFields) {
                String FKName = fk.getName();
                char[] setterFKNameArr = FKName.toCharArray();

                setterFKNameArr[0] = Character.toUpperCase(setterFKNameArr[0]);
                FKName = String.valueOf(setterFKNameArr);
                Method fkSetId = ModelScraper.getMethodByFieldName("set" + FKName);

                try {
                    fkSetId.invoke(model, rs.getInt(fk.getColumnName()));
                } catch (SQLException | InvocationTargetException e) {
                    // do nothing; added try-catch block since ResultSet throws an exception if the params aren't
                    // in the result set. Sometimes, we don't need to retrieve a FK
                }
            }

            for (AttrField selectedAttr: ModelScraper.getAppliedFields()) {
                Class<?> type = selectedAttr.getType();
                char[] getterAttrNameArr = selectedAttr.getName().toCharArray();
                getterAttrNameArr[0] = Character.toUpperCase(getterAttrNameArr[0]);
                String attrMethodName = String.valueOf(getterAttrNameArr);
                Method setAttr = ModelScraper.getMethodByFieldName("set" + attrMethodName);

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
