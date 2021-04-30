package com.revature.utils;

import com.revature.annotations.Table;
import com.revature.exceptions.BadMethodChainCallException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

class Remove<T> extends ModelScraper {
    PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;

    Remove (CrudModel<T> ref) {
        this.conn = ref.conn;
        this.ref = ref;
        appliedAttrs = new ArrayList<>();
    }

    CrudModel<T> remove() throws SQLException {
        appliedAttrs.clear();
        ps = null;
        Table table = clas.getAnnotation(Table.class);
        String tableName = table.tableName();
        ps = conn.prepareStatement("delete from " + tableName);
        return ref;
    }

    int runRemove() throws Exception {
        if (!ps.toString().startsWith("delete")) {
            throw new BadMethodChainCallException("runRemove() can only be called when remove() is the head of the method chain.");
        }

        return ps.executeUpdate();
    }
}
