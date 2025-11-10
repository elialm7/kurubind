package com.roelias.kurubind.repository;

import com.roelias.kurubind.KurubindDatabase;

import org.jdbi.v3.core.Handle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implementación genérica del patrón Repository que envuelve KurubindDatabase para proporcionar
 * operaciones CRUD fuertemente tipadas para una entidad específica.
 *
 * <p>Esta clase está diseñada para ser extendida por repositorios concretos que pueden añadir
 * métodos de consulta específicos del negocio.
 *
 * @param <T> El tipo de la entidad que maneja este repositorio.
 */
public class KuruRepository<T> {

    protected final KurubindDatabase db;
    protected final Class<T> type;

    /**
     * Crea un nuevo repositorio.
     *
     * @param db La instancia de KurubindDatabase.
     * @param type El .class de la entidad (ej: User.class).
     */
    public KuruRepository(KurubindDatabase db, Class<T> type) {
        this.db = db;
        this.type = type;
    }

    // =================================================================================
    // ========== Operaciones CRUD (Create, Update, Delete) ==========
    // =================================================================================

    /**
     * Inserta una nueva entidad en la base de datos. Ejecuta validaciones y generación de valores.
     *
     * @param entity La entidad a insertar.
     */
    public void insert(T entity) {
        db.insert(entity);
    }

    /**
     * Actualiza una entidad existente en la base de datos. Se basa en el campo @Id de la entidad.
     *
     * @param entity La entidad a actualizar.
     */
    public void update(T entity) {
        db.update(entity);
    }

    /**
     * Elimina una entidad de la base de datos. Se basa en el campo @Id de la entidad.
     *
     * @param entity La entidad a eliminar.
     */
    public void delete(T entity) {
        db.delete(entity);
    }

    /**
     * Elimina una entidad por su ID.
     *
     * @param id El ID de la entidad a eliminar.
     */
    public void deleteById(Object id) {
        db.deleteById(type, id);
    }

    // =================================================================================
    // ========== Operaciones en Lote (Batch) ==========
    // =================================================================================

    /**
     * Inserta una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a insertar.
     */
    public void insertAll(List<T> entities) {
        db.insertAll(entities);
    }

    /**
     * Actualiza una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a actualizar.
     */
    public void updateAll(List<T> entities) {
        db.updateAll(entities);
    }

    /**
     * Elimina una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a eliminar.
     */
    public void deleteAll(List<T> entities) {
        db.deleteAll(entities);
    }

    /**
     * Elimina entidades por una lista de IDs en una única transacción.
     *
     * @param ids La lista de IDs a eliminar.
     */
    public void deleteByIds(List<Object> ids) {
        // Este método se ejecuta en una transacción desde KurubindDatabase
        db.deleteByIds(type, ids);
    }

    // =================================================================================
    // ========== Operaciones de Búsqueda (Find/Query) ==========
    // =================================================================================

    /**
     * Retorna todas las instancias de la entidad. ¡Precaución! No usar en tablas grandes. Usar
     * paginación o streams.
     *
     * @return Una lista de todas las entidades.
     */
    public List<T> findAll() {
        // Renombrado de list() a findAll() para consistencia
        return db.findAll(type);
    }

    /**
     * Retorna una lista de entidades con un límite.
     *
     * @param limit El número máximo de registros a retornar.
     * @return Una lista de entidades.
     */
    public List<T> findAll(int limit) {
        return db.findAll(type, limit);
    }

    /**
     * Retorna una lista de entidades con límite y offset.
     *
     * @param limit El número máximo de registros a retornar.
     * @param offset El número de registros a saltar.
     * @return Una lista de entidades.
     */
    public List<T> findAll(int limit, int offset) {
        return db.findAll(type, limit, offset);
    }

    /**
     * Recupera una entidad por su ID.
     *
     * @param id El ID de la entidad.
     * @return Un {@link Optional} conteniendo la entidad si se encuentra.
     */
    public Optional<T> findById(Object id) {
        return db.findById(type, id);
    }

    /**
     * Recupera múltiples entidades por una lista de IDs.
     *
     * @param ids La lista de IDs a buscar.
     * @return Una lista de las entidades encontradas.
     */
    public List<T> findAllByIds(List<Object> ids) {
        return db.findAllByIds(type, ids);
    }

    /**
     * Retorna la primera entidad encontrada en la tabla.
     *
     * @return Un {@link Optional} con la primera entidad, o vacío.
     */
    public Optional<T> findFirst() {
        return db.findFirst(type);
    }

    /**
     * Ejecuta una consulta SQL personalizada y mapea los resultados al tipo del repositorio.
     *
     * @param sql La consulta SQL.
     * @return Una lista de entidades T.
     */
    public List<T> query(String sql) {
        return db.query(sql, type);
    }

    /**
     * Ejecuta una consulta SQL personalizada con parámetros.
     *
     * @param sql La consulta SQL con placeholders.
     * @param params Un mapa de parámetros.
     * @return Una lista de entidades T.
     */
    public List<T> query(String sql, Map<String, Object> params) {
        return db.query(sql, type, params);
    }

    /**
     * Ejecuta una consulta SQL personalizada esperando un único resultado.
     *
     * @param sql La consulta SQL.
     * @return Un {@link Optional} conteniendo el resultado.
     */
    public Optional<T> queryOne(String sql) {
        return db.queryOne(sql, type);
    }

    /**
     * Ejecuta una consulta SQL con parámetros esperando un único resultado.
     *
     * @param sql La consulta SQL con placeholders.
     * @param params Un mapa de parámetros.
     * @return Un {@link Optional} conteniendo el resultado.
     */
    public Optional<T> queryOne(String sql, Map<String, Object> params) {
        return db.queryOne(sql, type, params);
    }

    /**
     * Ejecuta una consulta SQL personalizada y mapea a un DTO o clase diferente.
     *
     * @param sql La consulta SQL.
     * @param resultType La clase del DTO (ej: UserSummary.class).
     * @param <R> El tipo del DTO.
     * @return Una lista de DTOs.
     */
    public <R> List<R> query(String sql, Class<R> resultType) {
        return db.query(sql, resultType);
    }

    /**
     * Ejecuta una consulta SQL con parámetros y mapea a un DTO.
     *
     * @param sql La consulta SQL con placeholders.
     * @param resultType La clase del DTO.
     * @param params Un mapa de parámetros.
     * @param <R> El tipo del DTO.
     * @return Una lista de DTOs.
     */
    public <R> List<R> query(String sql, Class<R> resultType, Map<String, Object> params) {
        return db.query(sql, resultType, params);
    }

    // =================================================================================
    // ========== Paginación ==========
    // =================================================================================

    /**
     * Obtiene una página de resultados para la entidad del repositorio.
     *
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @return Un {@link KurubindDatabase.PageResult} con los datos y metadatos de paginación.
     */
    public KurubindDatabase.PageResult<T> findAllPaged(int page, int pageSize) {
        return db.queryPage(type, page, pageSize);
    }

    /**
     * Obtiene una página de resultados para una consulta SQL personalizada.
     *
     * @param sql La consulta SQL base.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @return Un {@link KurubindDatabase.PageResult}.
     */
    public KurubindDatabase.PageResult<T> queryPage(String sql, int page, int pageSize) {
        return db.queryPage(sql, type, Collections.emptyMap(), page, pageSize);
    }

    /**
     * Obtiene una página de resultados para una consulta SQL con parámetros.
     *
     * @param sql La consulta SQL base con placeholders.
     * @param params Un mapa de parámetros.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @return Un {@link KurubindDatabase.PageResult}.
     */
    public KurubindDatabase.PageResult<T> queryPage(
            String sql, Map<String, Object> params, int page, int pageSize) {
        return db.queryPage(sql, type, params, page, pageSize);
    }

    // =================================================================================
    // ========== Utilidades ==========
    // =================================================================================

    /**
     * Cuenta el número total de registros para la entidad del repositorio.
     *
     * @return El número total de registros.
     */
    public Long countAll() {
        return db.countAll(type);
    }

    /**
     * Comprueba si una entidad existe por su ID.
     *
     * @param id El ID a comprobar.
     * @return true si la entidad existe, false en caso contrario.
     */
    public boolean exists(Object id) {
        return db.exists(type, id);
    }

    /**
     * Retorna el tipo de entidad (Class<T>) manejado por este repositorio.
     *
     * @return La clase de la entidad.
     */
    public Class<T> getEntityType() {
        return type;
    }

    /**
     * Retorna la instancia de KurubindDatabase subyacente para operaciones avanzadas.
     *
     * @return La instancia de KurubindDatabase.
     */
    public KurubindDatabase getDb() {
        return db;
    }

    // =================================================================================
    // ========== Acceso Transaccional ==========
    // =================================================================================

    /**
     * Ejecuta un bloque de código dentro de una transacción JDBI.
     *
     * @param handleConsumer Un {@link Consumer} que recibe el {@link Handle} transaccional.
     */
    protected void executeInTransaction(Consumer<Handle> handleConsumer) {
        // Actualizado para llamar al método renombrado
        db.executeInTransaction(handleConsumer);
    }
}
