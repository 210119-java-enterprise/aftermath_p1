package com.revature.utils;
import com.revature.exceptions.*;

import java.lang.String;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CrudModel<T> {
    Class<T> clas;
    private HashMap<String, Savepoint> savepoints; // Hashmap of savepoints
    protected PreparedStatement ps;
    protected Connection conn;
    private Grab<T> select;
    private Add<T> insert;
    private Remove<T> delete;
    private Change<T> update;
    private Where<T> criteria;
    private ModelScraper currentOperation;

    public CrudModel(Class<T> clas) throws SQLException {
        this.conn = ConnectionFactory.getInstance().getConnection();
        this.clas = clas;

        select = new Grab<>(this);
        insert = new Add<>(this);
        update = new Change<>(this);
        delete = new Remove<>(this);
        criteria = new Where<>(this);
        savepoints = new HashMap<>();
    }

    public void turnOffAutoCommit() throws SQLException {
        conn.setAutoCommit(false);
    }

    public void turnOnAutoCommit() throws SQLException {
        conn.setAutoCommit(true);
    }

    public void runCommit() throws SQLException {
        conn.commit();
    }

    public void addSavepoint(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Savepoint needs an associated name");
        }

        savepoints.put(name, conn.setSavepoint());
    }

    public void removeSavepoint(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not value");
        }

        savepoints.remove(name);
    }

    public void rollback(String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            throw new InvalidInputException("Key is not a value");
        }

        Savepoint selectedSavepoint = savepoints.get(name);
        conn.rollback(selectedSavepoint);
    }

    public CrudModel<T> grab(String ...attrs) {
        currentOperation = select;
        return select.grab(attrs);
    }

    public CrudModel<T> add(String... attrs) {
        currentOperation = insert;
        return insert.add(attrs);
    }

    public CrudModel<T> addValues(String... values) throws RuntimeException {
        return insert.addValues(values);
    }

    public CrudModel<T> change(String... attrs) throws SQLException {
        currentOperation = update;
        return update.change(attrs);
    }

    public CrudModel<T> set(String... values) throws SQLException {
        return update.set(values);
    }

    public CrudModel<T> remove() throws SQLException {
        currentOperation = delete;
        return delete.remove();
    }

    public CrudModel<T> where() throws SQLException, ClassNotFoundException {
        criteria.setPreparedStatementStr(getPreparedStatement());
        CrudModel<T> ref = criteria.where();
        setPreparedStatement(ref.ps);
        return ref;
    }

    public CrudModel<T> where(Conditions cond, String attr, String value) throws SQLException, ClassNotFoundException {
        criteria.setPreparedStatementStr(getPreparedStatement());
        CrudModel<T> ref = criteria.where(cond, attr, value);
        setPreparedStatement(ref.ps);
        return ref;
    }

    public CrudModel<T> and() throws SQLException {
        return criteria.and();
    }

    public CrudModel<T> and(Conditions cond, String attr, String value) throws SQLException {
        return criteria.and(cond, attr, value);
    }

    public CrudModel<T> or() throws SQLException {
        return criteria.or();
    }

    public CrudModel<T> or(Conditions cond, String attr, String value) throws SQLException {
        return criteria.or(cond, attr, value);
    }

    public CrudModel<T> not(Conditions cond, String attr, String value) throws SQLException {
        return criteria.not(cond, attr, value);
    }

    public void setPreparedStatement(PreparedStatement ps) throws ClassNotFoundException {
        if (currentOperation == null) {
            throw new BadMethodChainCallException("No operation has been initialized.");
        }

        switch(currentOperation.getClass().getSimpleName()) {
            case "Grab":
                select.ps = ps;
                break;
            case "Add":
                insert.ps = ps;
                break;
            case "Change":
                update.ps = ps;
                break;
            case "Remove":
                delete.ps = ps;
                break;
            default:
                throw new ClassNotFoundException("Class not located");
        }
    }

    public String getPreparedStatement() throws ClassNotFoundException {
        if (currentOperation == null) {
            throw new BadMethodChainCallException("No operation has been initialized.");
        }

        switch(currentOperation.getClass().getSimpleName()) {
            case "Grab":
                return select.ps.toString();
            case "Add":
                return insert.ps.toString();
            case "Change":
                return update.ps.toString();
            case "Remove":
                return delete.ps.toString();
            default:
                throw new ClassNotFoundException("Class not located");
        }
    }

    public ArrayList<T> runGrab() {
        return select.runGrab();
    }

    public int runAdd() throws Exception {
        return insert.runAdd();
    }

    public int runChange() throws Exception {
        return update.runChange();
    }

    public int runRemove() throws Exception {
        return delete.runRemove(getPreparedStatement());
    }
}