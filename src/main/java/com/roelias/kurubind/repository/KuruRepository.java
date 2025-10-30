package com.roelias.kurubind.repository;

import com.roelias.kurubind.KurubindDatabase;
import org.jdbi.v3.core.Handle;

import java.util.List;
import java.util.function.Consumer;

public class KuruRepository<T> {

    protected final KurubindDatabase db;
    protected final Class<T> type;

    public KuruRepository(KurubindDatabase db, Class<T> type) {
        this.db = db;
        this.type = type;
    }

    /**
     * Inserta una entidad
     */
    public void insert(T entity) {
        db.insert(entity);
    }

    /**
     * Inserta múltiples entidades
     */
    public void insertAll(List<T> entities) {
        db.insertAll(entities);
    }

    /**
     * Actualiza una entidad existente
     */
    public void update(T entity) {
        db.update(entity);
    }

    /**
     * Elimina una entidad
     */
    public void delete(T entity) {
        db.delete(entity);
    }

    /**
     * Elimina múltiples entidades
     */
    public void deleteAll(List<T> entities) {
        db.deleteAll(entities);
    }

    /**
     * Lista todas las entidades
     */
    public List<T> list() {
        return db.list(type);
    }

    /**
     * Ejecuta una query personalizada retornando entidades de tipo T
     */
    public List<T> query(String sql) {
        return db.query(sql, type);
    }

    /**
     * Ejecuta una query personalizada retornando DTOs de otro tipo
     */
    public <R> List<R> query(String sql, Class<R> resultType) {
        return db.query(sql, resultType);
    }

    /**
     * Ejecuta operaciones custom con acceso directo a JDBI Handle
     */
    protected void execute(Consumer<Handle> handleConsumer) {
        db.execute(handleConsumer);
    }

    /**
     * Retorna el tipo de entidad manejado por este repositorio
     */
    public Class<T> getEntityType() {
        return type;
    }
}
