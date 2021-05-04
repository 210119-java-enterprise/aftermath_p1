package com.revature.utils;

import com.revature.exceptions.BadMethodChainCallException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

class Where<T> extends ModelScraper {
    private PreparedStatement ps;
    private Connection conn;
    private CrudModel<T> ref;
    private ArrayList<AttrField> appliedAttrs;
    private String connStr;

    Where(CrudModel<T> ref) throws SQLException {
        this.conn = ref.conn;
        this.ref = ref;
        setTargetClass(ref.clas);
        appliedAttrs = new ArrayList<>();
        connStr = "";
    }

    void setPreparedStatementStr(String ps) throws SQLException {
        connStr += ps + " ";
        this.ps = conn.prepareStatement(connStr);
    }

    CrudModel<T> where() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("where() can't be called off of add() since insert statements can't have where clauses");
        }

        if (ps.toString().contains("where")) {
            throw new BadMethodChainCallException("where() can only be called once in a method chain call." +
                    " Use and(), or(), or not()");
        }

        ps = conn.prepareStatement(ps.toString() + " where ");
        ref.ps = ps;
        return ref;
    }

    CrudModel<T> where(Conditions cond, String attr, String value) throws SQLException {
        where();
        builtWhereClause(cond, "", attr, value);
        ref.ps = ps;
        return ref;
    }

    CrudModel<T> and() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("cannot call and() on add() methods");
        }

        if (!ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call and() if there is no where clause");
        }

        ps = conn.prepareStatement(ps.toString() + " and ");
        return ref;
    }

    CrudModel<T> and(Conditions cond, String attr, String value) throws SQLException {
        and();
        builtWhereClause(cond, "", attr, value);
        return ref;
    }

    CrudModel<T> or() throws SQLException {
        if (ps.toString().startsWith("insert"))
        {
            throw new BadMethodChainCallException("cannot call or() on add() methods");
        }

        if (!ps.toString().contains("where"))
        {
            throw new BadMethodChainCallException("cannot call or() if there is no where clause");
        }

        ps = conn.prepareStatement(ps.toString() + " or ");
        return ref;
    }

    CrudModel<T> or(Conditions cond, String attr, String value) throws SQLException {
        or();
        builtWhereClause(cond, "", attr, value);
        return ref;
    }

    CrudModel<T> not(Conditions cond, String attr, String value) throws SQLException {
        builtWhereClause(cond, "not ", attr, value);
        return ref;
    }

    private void builtWhereClause(Conditions cond, String logicalOp, String attr, String value) throws SQLException {
        AttrField selectedField = null;

        if (attrFields.size() == 0) {
            setAppliedFields(appliedAttrs);
        }

        String psStr = ps.toString() + logicalOp;
        switch (cond) {
            case EQUALS:
                ps = conn.prepareStatement(psStr + attr + " = ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case NOT_EQUALS:
                ps = conn.prepareStatement(psStr + attr + " <> ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case GT:
                ps = conn.prepareStatement(psStr + attr + " > ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case LT:
                ps = conn.prepareStatement(psStr + attr + " < ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case GTE:
                ps = conn.prepareStatement(psStr + attr + " >= ?");
                selectedField = getAttributeByColumnName(attr);
                break;
            case LTE:
                ps = conn.prepareStatement(psStr + attr + " <= ?");
                selectedField = getAttributeByColumnName(attr);
                break;
        }

        Class<?> type = selectedField.getType();

        if (type == String.class) {
            ps.setString(1, value);
        } else if (type == int.class) {
            ps.setInt(1, Integer.parseInt(value));
        } else if (type == double.class) {
            ps.setDouble(1, Double.parseDouble(value));
        }
    }
}
