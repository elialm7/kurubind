package com.roelias.legacy.repository;

import com.roelias.legacy.KurubindDatabase;
import org.jdbi.v3.core.Handle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class KuruRepository<T> {

    protected final KurubindDatabase db;
    protected final Class<T> type;


    public KuruRepository(KurubindDatabase db, Class<T> type) {
        this.db = db;
        this.type = type;
    }


    public void insert(T entity) {
        db.insert(entity);
    }


    public void update(T entity) {
        db.update(entity);
    }


    public void delete(T entity) {
        db.delete(entity);
    }


    public void deleteById(Object id) {
        db.deleteById(type, id);
    }


    public void insertAll(List<T> entities) {
        db.insertAll(entities);
    }


    public void updateAll(List<T> entities) {
        db.updateAll(entities);
    }


    public void deleteAll(List<T> entities) {
        db.deleteAll(entities);
    }


    public void deleteByIds(List<Object> ids) {
        // Este método se ejecuta en una transacción desde KurubindDatabase
        db.deleteByIds(type, ids);
    }

    public List<T> findAll() {
        // Renombrado de list() a findAll() para consistencia
        return db.findAll(type);
    }


    public List<T> findAll(int limit) {
        return db.findAll(type, limit);
    }


    public List<T> findAll(int limit, int offset) {
        return db.findAll(type, limit, offset);
    }


    public Optional<T> findById(Object id) {
        return db.findById(type, id);
    }

    public List<T> findAllByIds(List<Object> ids) {
        return db.findAllByIds(type, ids);
    }


    public Optional<T> findFirst() {
        return db.findFirst(type);
    }


    public List<T> query(String sql) {
        return db.query(sql, type);
    }


    public List<T> query(String sql, Map<String, Object> params) {
        return db.query(sql, type, params);
    }


    public Optional<T> queryOne(String sql) {
        return db.queryOne(sql, type);
    }


    public Optional<T> queryOne(String sql, Map<String, Object> params) {
        return db.queryOne(sql, type, params);
    }


    public <R> List<R> query(String sql, Class<R> resultType) {
        return db.query(sql, resultType);
    }


    public <R> List<R> query(String sql, Class<R> resultType, Map<String, Object> params) {
        return db.query(sql, resultType, params);
    }


    public KurubindDatabase.PageResult<T> findAllPaged(int page, int pageSize) {
        return db.queryPage(type, page, pageSize);
    }


    public KurubindDatabase.PageResult<T> queryPage(String sql, int page, int pageSize) {
        return db.queryPage(sql, type, Collections.emptyMap(), page, pageSize);
    }


    public KurubindDatabase.PageResult<T> queryPage(
            String sql, Map<String, Object> params, int page, int pageSize) {
        return db.queryPage(sql, type, params, page, pageSize);
    }


    public Long countAll() {
        return db.countAll(type);
    }


    public boolean exists(Object id) {
        return db.exists(type, id);
    }


    public Class<T> getEntityType() {
        return type;
    }

    public KurubindDatabase getDb() {
        return db;
    }


    protected void executeInTransaction(Consumer<Handle> handleConsumer) {
        db.executeInTransaction(handleConsumer);
    }
}
