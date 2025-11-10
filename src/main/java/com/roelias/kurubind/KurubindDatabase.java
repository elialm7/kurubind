package com.roelias.kurubind;

import com.roelias.kurubind.annotations.DefaultValue;
import com.roelias.kurubind.annotations.Generated;
import com.roelias.kurubind.core.*;
import com.roelias.kurubind.exceptions.ValidationError;
import com.roelias.kurubind.exceptions.ValidationException;
import com.roelias.kurubind.mapper.KurubindRowMapper;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.registry.HandlerRegistry;
import com.roelias.kurubind.registry.SQLGeneratorRegistry;
import com.roelias.kurubind.registry.ValidatorRegistry;
import com.roelias.kurubind.registry.ValueGeneratorRegistry;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Núcleo de Kurubind. Proporciona una base de datos coordinada para operaciones CRUD, consultas
 * personalizadas, validación y generación de valores.
 *
 * <p>Esta clase está diseñada para ser construida usando el {@link Builder}.
 *
 * @author El autor original
 * @version 1.0
 */
public class KurubindDatabase {
    private final JdbiProvider jdbiProvider;
    private final HandlerRegistry handlerRegistry;
    private final SQLGeneratorRegistry sqlGeneratorRegistry;
    private final ValidatorRegistry validatorRegistry;
    private final ValueGeneratorRegistry valueGeneratorRegistry;
    private final Dialect dialect;

    private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();

    private KurubindDatabase(Builder builder) {
        this.jdbiProvider = builder.jdbiProvider;
        this.handlerRegistry = builder.handlerRegistry;
        this.sqlGeneratorRegistry = builder.sqlGeneratorRegistry;
        this.validatorRegistry = builder.validatorRegistry;
        this.valueGeneratorRegistry = builder.valueGeneratorRegistry;
        this.dialect = builder.dialect;
    }

    /**
     * Crea un nuevo constructor de KurubindDatabase.
     *
     * @return retorna una nueva instancia de {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Obtiene los metadatos cacheados para una clase de entidad.
     *
     * @param entityClass La clase de la entidad.
     * @return El {@link EntityMetadata} para esa clase.
     */
    private EntityMetadata getMetadata(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, EntityMetadata::new);
    }

    // =================================================================================
    // ========== Operaciones CRUD (Create, Read, Update, Delete) ==========
    // =================================================================================

    /**
     * Inserta una única entidad en la base de datos. Ejecuta la generación de valores y la
     * validación antes de insertar.
     *
     * @param entity La entidad a insertar.
     * @param <T> El tipo de la entidad.
     * @throws ValidationException Si la entidad falla la validación.
     */
    public <T> void insert(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden insertar entidades @QueryResponse");
        }
        generateValues(entity, metadata, true, false);
        validateEntity(entity, metadata);
        executeInTransaction(
                handle -> {
                    internalInsert(handle, entity, metadata);
                });
    }

    /**
     * Inserta una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a insertar.
     * @param <T> El tipo de la entidad.
     * @throws ValidationException Si alguna entidad falla la validación.
     */
    public <T> void insertAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden insertar entidades @QueryResponse");
        }

        // Validar todas primero
        for (T entity : entities) {
            generateValues(entity, metadata, true, false);
            validateEntity(entity, metadata);
        }
        // Insertar todas en una transacción
        executeInTransaction(
                (Consumer<Handle>)
                        handle ->
                                entities.forEach(
                                        entity -> internalInsert(handle, entity, metadata)));
    }

    /**
     * Actualiza una entidad existente basada en su campo {@literal @Id}. Ejecuta la generación de
     * valores (para onUpdate) y la validación.
     *
     * @param entity La entidad a actualizar.
     * @param <T> El tipo de la entidad.
     * @throws ValidationException Si la entidad falla la validación.
     * @throws IllegalArgumentException Si la entidad no tiene un campo {@literal @Id}.
     */
    public <T> void update(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden actualizar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException(
                    "La entidad debe tener un campo @Id para actualizar");
        }

        generateValues(entity, metadata, false, true);
        validateEntity(entity, metadata);

        executeInTransaction(
                handle -> {
                    internalUpdate(handle, entity, metadata);
                });
    }

    /**
     * Actualiza una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a actualizar.
     * @param <T> El tipo de la entidad.
     * @throws ValidationException Si alguna entidad falla la validación.
     */
    public <T> void updateAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());

        for (T entity : entities) {
            if (metadata.isQueryResponse()) {
                throw new IllegalArgumentException(
                        "No se pueden actualizar entidades @QueryResponse");
            }
            if (!metadata.hasIdField()) {
                throw new IllegalArgumentException(
                        "La entidad debe tener un campo @Id para actualizar");
            }
            generateValues(entity, metadata, false, true);
            validateEntity(entity, metadata);
        }
        executeInTransaction(
                (Consumer<Handle>)
                        handle ->
                                entities.forEach(
                                        entity -> internalUpdate(handle, entity, metadata)));
    }

    /**
     * Elimina una entidad de la base de datos basada en su campo {@literal @Id}.
     *
     * @param entity La entidad a eliminar.
     * @param <T> El tipo de la entidad.
     * @throws IllegalArgumentException Si la entidad no tiene un campo {@literal @Id}.
     */
    public <T> void delete(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        executeInTransaction(
                handle -> {
                    internalDelete(handle, entity, metadata);
                });
    }

    /**
     * Elimina una lista de entidades en una única transacción.
     *
     * @param entities La lista de entidades a eliminar.
     * @param <T> El tipo de la entidad.
     */
    public <T> void deleteAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        executeInTransaction(
                (Consumer<Handle>)
                        handle ->
                                entities.forEach(
                                        entity -> internalDelete(handle, entity, metadata)));
    }

    /**
     * Elimina una lista de entidades por sus IDs en una única transacción.
     *
     * @param entityClass La clase de la entidad a eliminar.
     * @param ids La lista de IDs a eliminar.
     * @param <T> El tipo de la entidad.
     */
    public <T> void deleteByIds(Class<T> entityClass, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1. Obtener metadatos y validaciones
        EntityMetadata metadata = getMetadata(entityClass); //
        if (metadata.isQueryResponse()) { //
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) { //
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        // 2. Obtener el SQL para un solo borrado por ID
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect); //
        String sql = generator.generateDelete(metadata); //
        String idColumnName = metadata.getIdField().getColumnName(); //

        // 3. Ejecutar todos los borrados dentro de una única transacción
        executeInTransaction(
                handle -> { //
                    ids.forEach(
                            id -> {
                                handle.createUpdate(sql).bind(idColumnName, id).execute();
                            });
                });
    }

    /**
     * Elimina una entidad por su ID sin necesidad de tener el objeto entidad.
     *
     * @param entityClass La clase de la entidad a eliminar.
     * @param id El ID de la entidad a eliminar.
     * @param <T> El tipo de la entidad.
     */
    public <T> void deleteById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = getMetadata(entityClass);

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateDelete(metadata);

        executeInTransaction(
                handle -> {
                    handle.createUpdate(sql)
                            .bind(metadata.getIdField().getColumnName(), id)
                            .execute();
                });
    }

    // =================================================================================
    // ========== Operaciones de Consulta de Entidad (Find) ==========
    // =================================================================================

    /**
     * Retorna todas las instancias de una entidad. ¡Precaución! No usar en tablas grandes. Usar
     * paginación o streams.
     *
     * @param entityClass La clase de la entidad a consultar.
     * @param <T> El tipo de la entidad.
     * @return Una lista de todas las entidades.
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden listar entidades @QueryResponse. Use query() en su lugar.");
        }
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        return query(sql, entityClass);
    }

    /**
     * Retorna una lista de entidades con un límite. (Equivalente a findAll(entityClass, limit, 0))
     *
     * @param entityClass La clase de la entidad a consultar.
     * @param limit El número máximo de registros a retornar.
     * @param <T> El tipo de la entidad.
     * @return Una lista de entidades.
     */
    public <T> List<T> findAll(Class<T> entityClass, int limit) {
        return findAll(entityClass, limit, 0);
    }

    /**
     * Retorna una lista de entidades con límite y offset para paginación simple. Esta versión NO
     * calcula el total de páginas.
     *
     * @param entityClass La clase de la entidad a consultar.
     * @param limit El número máximo de registros a retornar.
     * @param offset El número de registros a saltar.
     * @param <T> El tipo de la entidad.
     * @return Una lista de entidades.
     */
    public <T> List<T> findAll(Class<T> entityClass, int limit, int offset) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden listar entidades @QueryResponse. Use query() en su lugar.");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        String pagedSql = buildPaginatedQuery(sql, 1, 1); // Helper para añadir LIMIT/OFFSET

        Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);

        return query(pagedSql, entityClass, params);
    }

    /**
     * Recupera una entidad por su ID.
     *
     * @param entityClass La clase de la entidad.
     * @param id El ID de la entidad.
     * @param <T> El tipo de la entidad.
     * @return Un {@link Optional} conteniendo la entidad si se encuentra, o vacío si no.
     */
    public <T> Optional<T> findById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden consultar @QueryResponse por ID. Use query() en su lugar.");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException(
                    "La entidad debe tener un campo @Id para consultar por ID");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelectById(metadata);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);

        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).bind("id", id).map(mapper).findOne());
    }

    /**
     * Recupera múltiples entidades por una lista de IDs.
     *
     * @param entityClass La clase de la entidad.
     * @param ids La lista de IDs a buscar.
     * @param <T> El tipo de la entidad.
     * @return Una lista de las entidades encontradas.
     */
    public <T> List<T> findAllByIds(Class<T> entityClass, List<Object> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        List<T> results = new ArrayList<>();
        for (Object id : ids) {
            findById(entityClass, id).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Retorna la primera entidad encontrada en la tabla.
     *
     * @param entityClass La clase de la entidad.
     * @param <T> El tipo de la entidad.
     * @return Un {@link Optional} con la primera entidad, o vacío si la tabla está vacía.
     */
    public <T> Optional<T> findFirst(Class<T> entityClass) {
        // Corrección de nombre: findFirts -> findFirst
        List<T> results = findAll(entityClass, 1); // Usa el método optimizado
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Comprueba si una entidad existe por su ID.
     *
     * @param entityClass La clase de la entidad.
     * @param id El ID a comprobar.
     * @param <T> El tipo de la entidad.
     * @return true si la entidad existe, false en caso contrario.
     */
    public <T> boolean exists(Class<T> entityClass, Object id) {
        return findById(entityClass, id).isPresent();
    }

    /**
     * Cuenta el número total de registros para una entidad.
     *
     * @param entityClass La clase de la entidad.
     * @return El número total de registros.
     */
    public Long countAll(Class<?> entityClass) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden contar entidades @QueryResponse.");
        }
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateCount(metadata);
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapTo(Long.class).one());
    }

    // =================================================================================
    // ========== Operaciones de Consulta SQL Personalizada (Query) ==========
    // =================================================================================

    /**
     * Ejecuta una consulta SQL personalizada y mapea los resultados a una clase (Entidad o DTO).
     *
     * @param sql La consulta SQL (ej: "SELECT * FROM users WHERE status = 'active'").
     * @param resultClass La clase ({@literal @Table} o {@literal @QueryResponse}) a la que mapear.
     * @param <T> El tipo del resultado.
     * @return Una lista de objetos T.
     */
    public <T> List<T> query(String sql, Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).map(mapper).list());
    }

    /**
     * Ejecuta una consulta SQL personalizada con parámetros y mapea los resultados.
     *
     * @param sql La consulta SQL con placeholders (ej: "SELECT * FROM users WHERE status =
     *     :status").
     * @param resultClass La clase ({@literal @Table} o {@literal @QueryResponse}) a la que mapear.
     * @param params Un mapa de parámetros (ej: Map.of("status", "active")).
     * @param <T> El tipo del resultado.
     * @return Una lista de objetos T.
     */
    public <T> List<T> query(String sql, Class<T> resultClass, Map<String, Object> params) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.map(mapper).list();
                        });
    }

    /**
     * Ejecuta una consulta SQL personalizada esperando un único resultado.
     *
     * @param sql La consulta SQL.
     * @param resultClass La clase del resultado.
     * @param <T> El tipo del resultado.
     * @return Un {@link Optional} conteniendo el resultado, o vacío si no se encuentra.
     */
    public <T> Optional<T> queryOne(String sql, Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).map(mapper).findOne());
    }

    /**
     * Ejecuta una consulta SQL personalizada con parámetros esperando un único resultado.
     *
     * @param sql La consulta SQL con placeholders.
     * @param resultClass La clase del resultado.
     * @param params Un mapa de parámetros.
     * @param <T> El tipo del resultado.
     * @return Un {@link Optional} conteniendo el resultado, o vacío si no se encuentra.
     */
    public <T> Optional<T> queryOne(String sql, Class<T> resultClass, Map<String, Object> params) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.map(mapper).findOne();
                        });
    }

    /**
     * Ejecuta una consulta SQL con parámetros y procesa los resultados como un Stream.
     *
     * <p>¡Importante! El Stream debe ser cerrado (ej: usando try-with-resources) para liberar la
     * conexión a la base de datos.
     *
     * @param sql La consulta SQL con placeholders.
     * @param resultClass La clase a la que mapear.
     * @param params Un mapa de parámetros.
     * @param <T> El tipo del resultado.
     * @return Un {@link Stream} de objetos T que debe ser cerrado.
     */
    public <T> Stream<T> queryStream(String sql, Class<T> resultClass, Map<String, Object> params) {
        // 1. Obtener el mapper, igual que antes
        EntityMetadata metadata = getMetadata(resultClass); //
        KurubindRowMapper<T> mapper =
                new KurubindRowMapper<>(
                        metadata, handlerRegistry, dialect //
                        );

        // 2. Abrir un Handle manualmente (NO usar withHandle)
        Handle handle = jdbiProvider.getJdbi().open(); //

        try {
            // 3. Crear la consulta y vincular parámetros
            Query query = handle.createQuery(sql);
            params.forEach(query::bind); //

            // 4. Crear el stream y adjuntar el cierre del handle a su ciclo de vida
            return query.map(mapper).stream().onClose(handle::close); // <-- Esta es la clave

        } catch (Exception e) {
            // Si algo falla ANTES de retornar el stream, cierra el handle
            handle.close();
            throw new RuntimeException("Error al crear el stream de la consulta", e);
        }
    }
    /**
     * Ejecuta una consulta SQL y procesa los resultados como un Stream.
     *
     * ¡Importante! El Stream debe ser cerrado (ej: usando try-with-resources)
     * para liberar la conexión a la base de datos.
     *
     * @param sql         La consulta SQL.
     * @param resultClass La clase a la que mapear.
     * @param <T>         El tipo del resultado.
     * @return Un {@link Stream} de objetos T que debe ser cerrado.
     */
    public <T> Stream<T> queryStream(String sql, Class<T> resultClass) {
        // Simplemente delega al método con parámetros usando un mapa vacío
        return queryStream(sql, resultClass, Collections.emptyMap());
    }

    // =================================================================================
    // ========== Operaciones de Consulta Raw (Mapas) ==========
    // =================================================================================

    /**
     * Ejecuta una consulta SQL y retorna los resultados como una lista de Mapas. Útil para
     * reportes, JOINS complejos o DTOs dinámicos.
     *
     * @param sql La consulta SQL.
     * @return Una lista de Map<String, Object>.
     */
    public List<Map<String, Object>> queryForMaps(String sql) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapToMap().list());
    }

    /**
     * Ejecuta una consulta SQL con parámetros y retorna una lista de Mapas.
     *
     * @param sql La consulta SQL con placeholders.
     * @param params Un mapa de parámetros.
     * @return Una lista de Map<String, Object>.
     */
    public List<Map<String, Object>> queryForMaps(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapToMap().list();
                        });
    }

    /**
     * Ejecuta una consulta SQL y retorna un único resultado como un Mapa.
     *
     * @param sql La consulta SQL.
     * @return Un {@link Optional} conteniendo el mapa, o vacío.
     */
    public Optional<Map<String, Object>> queryForMap(String sql) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapToMap().findOne());
    }

    /**
     * Ejecuta una consulta SQL con parámetros y retorna un único resultado como un Mapa.
     *
     * @param sql La consulta SQL con placeholders.
     * @param params Un mapa de parámetros.
     * @return Un {@link Optional} conteniendo el mapa, o vacío.
     */
    public Optional<Map<String, Object>> queryForMap(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapToMap().findOne();
                        });
    }

    // =================================================================================
    // ========== Operaciones de Consulta Escalar (Valores Únicos) ==========
    // =================================================================================

    /**
     * Ejecuta una consulta esperando un único valor (ej: un String, Long, Boolean).
     *
     * @param sql La consulta SQL (ej: "SELECT name FROM users WHERE id = 1").
     * @param type La clase del tipo de retorno (ej: String.class).
     * @param <T> El tipo de retorno.
     * @return Un {@link Optional} conteniendo el valor, o vacío.
     */
    public <T> Optional<T> queryForObject(String sql, Class<T> type) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapTo(type).findOne());
    }

    /**
     * Ejecuta una consulta parametrizada esperando un único valor.
     *
     * @param sql La consulta SQL (ej: "SELECT name FROM users WHERE id = :id").
     * @param type La clase del tipo de retorno (ej: String.class).
     * @param params Un mapa de parámetros (ej: Map.of("id", 1)).
     * @param <T> El tipo de retorno.
     * @return Un {@link Optional} conteniendo el valor, o vacío.
     */
    public <T> Optional<T> queryForObject(String sql, Class<T> type, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapTo(type).findOne();
                        });
    }

    /**
     * Ejecuta una consulta esperando una lista de valores escalares.
     *
     * @param sql La consulta SQL (ej: "SELECT email FROM users").
     * @param type La clase del tipo de retorno (ej: String.class).
     * @param <T> El tipo de retorno.
     * @return Una lista de valores T.
     */
    public <T> List<T> queryForList(String sql, Class<T> type) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapTo(type).list());
    }

    /**
     * Ejecuta una consulta parametrizada esperando una lista de valores escalares.
     *
     * @param sql La consulta SQL (ej: "SELECT email FROM users WHERE status = :status").
     * @param type La clase del tipo de retorno (ej: String.class).
     * @param params Un mapa de parámetros.
     * @param <T> El tipo de retorno.
     * @return Una lista de valores T.
     */
    public <T> List<T> queryForList(String sql, Class<T> type, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapTo(type).list();
                        });
    }

    /**
     * Helper para consultas que retornan un Long (ej: COUNT(*)). Retorna 0L si no hay resultados.
     *
     * @param sql La consulta SQL.
     * @return El resultado Long o 0L.
     */
    public Long queryForLong(String sql) {
        return queryForObject(sql, Long.class).orElse(0L);
    }

    /**
     * Helper para consultas parametrizadas que retornan un Long. Retorna 0L si no hay resultados.
     *
     * @param sql La consulta SQL.
     * @param params Un mapa de parámetros.
     * @return El resultado Long o 0L.
     */
    public Long queryForLong(String sql, Map<String, Object> params) {
        return queryForObject(sql, Long.class, params).orElse(0L);
    }

    // ... (Puedes añadir queryForInt, queryForString de la misma manera) ...

    public Integer queryForInt(String sql) {
        return queryForObject(sql, Integer.class).orElse(0);
    }

    public Integer queryForInt(String sql, Map<String, Object> params) {
        return queryForObject(sql, Integer.class, params).orElse(0);
    }

    public String queryForString(String sql) {
        return queryForObject(sql, String.class).orElse(null);
    }

    public String queryForString(String sql, Map<String, Object> params) {
        return queryForObject(sql, String.class, params).orElse(null);
    }

    // =================================================================================
    // ========== Paginación ==========
    // =================================================================================

    /**
     * Obtiene una página de resultados para una entidad. Ejecuta una consulta de conteo y una
     * consulta de datos.
     *
     * @param entityClass La clase de la entidad.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @param <T> El tipo de la entidad.
     * @return Un {@link PageResult} con los datos y metadatos de paginación.
     */
    public <T> PageResult<T> queryPage(Class<T> entityClass, int page, int pageSize) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden paginar entidades @QueryResponse.");
        }
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        return queryPage(sql, entityClass, Collections.emptyMap(), page, pageSize);
    }

    /**
     * Obtiene una página de resultados para una consulta SQL personalizada.
     *
     * @param sql La consulta SQL base.
     * @param resultClass La clase a la que mapear.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @param <T> El tipo del resultado.
     * @return Un {@link PageResult}.
     */
    public <T> PageResult<T> queryPage(String sql, Class<T> resultClass, int page, int pageSize) {
        return queryPage(sql, resultClass, Collections.emptyMap(), page, pageSize);
    }

    /**
     * Obtiene una página de resultados para una consulta SQL personalizada con parámetros.
     *
     * @param sql La consulta SQL base con placeholders.
     * @param resultClass La clase a la que mapear.
     * @param params Un mapa de parámetros para la consulta.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @param <T> El tipo del resultado.
     * @return Un {@link PageResult}.
     */
    public <T> PageResult<T> queryPage(
            String sql, Class<T> resultClass, Map<String, Object> params, int page, int pageSize) {
        // Contar total de resultados
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        long total = queryForLong(countSql, params);

        // Construir consulta paginada
        String pagedSql = buildPaginatedQuery(sql, page, pageSize);

        // Crear parámetros con límite y offset
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("limit", pageSize);
        allParams.put("offset", (page - 1) * pageSize);

        List<T> results = query(pagedSql, resultClass, allParams);

        return new PageResult<>(results, page, pageSize, total);
    }

    /**
     * Obtiene una página de resultados como una lista de Mapas.
     *
     * @param sql La consulta SQL base.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @return Un {@link PageResult} de tipo Map<String, Object>.
     */
    public PageResult<Map<String, Object>> queryPageForMaps(String sql, int page, int pageSize) {
        return queryPageForMaps(sql, Collections.emptyMap(), page, pageSize);
    }

    /**
     * Obtiene una página de resultados parametrizada como una lista de Mapas.
     *
     * @param sql La consulta SQL base con placeholders.
     * @param params Un mapa de parámetros.
     * @param page El número de página (comenzando en 1).
     * @param pageSize El número de items por página.
     * @return Un {@link PageResult} de tipo Map<String, Object>.
     */
    public PageResult<Map<String, Object>> queryPageForMaps(
            String sql, Map<String, Object> params, int page, int pageSize) {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        long total = queryForLong(countSql, params);

        String pagedSql = buildPaginatedQuery(sql, page, pageSize);
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("limit", pageSize);
        allParams.put("offset", (page - 1) * pageSize);

        List<Map<String, Object>> results = queryForMaps(pagedSql, allParams);
        return new PageResult<>(results, page, pageSize, total);
    }

    // =================================================================================
    // ========== Operaciones DML (Update, Batch) ==========
    // =================================================================================

    /**
     * Ejecuta una sentencia DML (UPDATE, DELETE, INSERT) con SQL personalizado.
     *
     * @param sql La sentencia SQL.
     * @return El número de filas afectadas.
     */
    public int executeUpdate(String sql) {
        return jdbiProvider.getJdbi().withHandle(handle -> handle.createUpdate(sql).execute());
    }

    /**
     * Ejecuta una sentencia DML parametrizada.
     *
     * @param sql La sentencia SQL con placeholders (ej: "UPDATE users SET status = :status").
     * @param params Un mapa de parámetros.
     * @return El número de filas afectadas.
     */
    public int executeUpdate(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Update update = handle.createUpdate(sql);
                            params.forEach(update::bind);
                            return update.execute();
                        });
    }

    /**
     * Ejecuta un lote de sentencias DML parametrizadas.
     *
     * @param sql La sentencia SQL base (ej: "INSERT INTO logs (msg) VALUES (:msg)").
     * @param batchParams Una lista de Mapas de parámetros (un mapa por cada ejecución).
     * @return Una lista de enteros, cada uno representando las filas afectadas por su respectiva
     *     ejecución.
     */
    public List<Integer> executeBatch(String sql, List<Map<String, Object>> batchParams) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            List<Integer> results = new ArrayList<>();
                            for (Map<String, Object> params : batchParams) {
                                Update update = handle.createUpdate(sql);
                                params.forEach(update::bind);
                                results.add(update.execute());
                            }
                            return results;
                        });
    }

    // =================================================================================
    // ========== Procedimientos Almacenados y Funciones ==========
    // =================================================================================

    /**
     * Llama a un procedimiento almacenado que no retorna resultados.
     *
     * @param procedureName El nombre del procedimiento.
     */
    public void callProcedure(String procedureName) {
        jdbiProvider
                .getJdbi()
                .useHandle(handle -> handle.createCall("{call " + procedureName + "()}").invoke());
    }

    /**
     * Llama a un procedimiento almacenado con parámetros.
     *
     * @param procedureName El nombre del procedimiento.
     * @param params Un mapa de parámetros (el orden importa).
     */
    public void callProcedure(String procedureName, Map<String, Object> params) {
        jdbiProvider
                .getJdbi()
                .useHandle(
                        handle -> {
                            StringBuilder sql =
                                    new StringBuilder("{call ").append(procedureName).append("(");
                            sql.append(String.join(",", Collections.nCopies(params.size(), "?")));
                            sql.append(")}");

                            Call call = handle.createCall(sql.toString());
                            params.forEach(
                                    call::bind); // Asume que el mapa está ordenado o que JDBI
                            // maneja el binding
                            call.invoke();
                        });
    }

    /**
     * Llama a una función de la base de datos y retorna su valor.
     *
     * @param functionName El nombre de la función.
     * @param returnType La clase del tipo de retorno.
     * @param <T> El tipo de retorno.
     * @return Un {@link Optional} con el valor de retorno.
     */
    public <T> Optional<T> callFunction(String functionName, Class<T> returnType) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle ->
                                handle.createQuery("SELECT " + functionName + "()")
                                        .mapTo(returnType)
                                        .findOne());
    }

    /**
     * Llama a una función de la base de datos con parámetros.
     *
     * @param functionName El nombre de la función.
     * @param returnType La clase del tipo de retorno.
     * @param params Un mapa de parámetros (ej: Map.of("arg1", val1)).
     * @param <T> El tipo de retorno.
     * @return Un {@link Optional} con el valor de retorno.
     */
    public <T> Optional<T> callFunction(
            String functionName, Class<T> returnType, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            String placeholders =
                                    params.keySet().stream()
                                            .map(k -> ":" + k)
                                            .collect(Collectors.joining(","));
                            String sql = "SELECT " + functionName + "(" + placeholders + ")";

                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapTo(returnType).findOne();
                        });
    }

    // =================================================================================
    // ========== Manejo de Transacciones y Handles ==========
    // =================================================================================

    /**
     * Ejecuta un bloque de código dentro de una transacción JDBI. El trabajo será confirmado
     * (commit) al final, o revertido (rollback) si ocurre una excepción.
     *
     * @param handleConsumer Un {@link Consumer} que recibe el {@link Handle} transaccional.
     */
    public void executeInTransaction(Consumer<Handle> handleConsumer) {
        // Refactor de nombre: execute -> executeInTransaction
        jdbiProvider.getJdbi().useTransaction(handleConsumer::accept);
    }

    /**
     * Ejecuta un bloque de código dentro de una transacción JDBI y retorna un resultado.
     *
     * @param handleFunction Un {@link Function} que recibe el {@link Handle} y retorna un valor.
     * @param <T> El tipo del valor de retorno.
     * @return El resultado de la función.
     */
    public <T> T executeInTransaction(Function<Handle, T> handleFunction) {
        return jdbiProvider.getJdbi().inTransaction(handleFunction::apply);
    }

    /**
     * Ejecuta un bloque de código con un {@link Handle} de JDBI (sin transacción explícita). El
     * handle se obtiene y se cierra automáticamente.
     *
     * @param handleConsumer Un {@link Consumer} que recibe el {@link Handle}.
     */
    public void executeWithHandle(Consumer<Handle> handleConsumer) {
        jdbiProvider.getJdbi().useHandle(handleConsumer::accept);
    }

    /**
     * Ejecuta un bloque de código con un {@link Handle} de JDBI y retorna un resultado. (Sin
     * transacción explícita).
     *
     * @param handleFunction Un {@link Function} que recibe el {@link Handle} y retorna un valor.
     * @param <T> El tipo del valor de retorno.
     * @return El resultado de la función.
     */
    public <T> T withHandle(Function<Handle, T> handleFunction) {
        return jdbiProvider.getJdbi().withHandle(handleFunction::apply);
    }

    // =================================================================================
    // ========== Utilidades ==========
    // =================================================================================

    /**
     * Retorna la instancia de Jdbi subyacente.
     *
     * @return El {@link Jdbi} principal.
     */
    public Jdbi getJdbi() {
        return jdbiProvider.getJdbi();
    }

    /**
     * Obtiene un {@link RowMapper} de Kurubind para un tipo de resultado. Útil para integrar con
     * APIs de JDBI que requieren un RowMapper.
     *
     * @param resultClass La clase del resultado.
     * @param <T> El tipo del resultado.
     * @return Un {@link RowMapper} configurado.
     */
    public <T> RowMapper<T> getRowMapper(Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        return (resultSet, ctx) -> {
            KurubindRowMapper<T> mapper =
                    new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
            return mapper.map(resultSet, ctx);
        };
    }

    // =================================================================================
    // ========== Métodos Privados (Helpers) ==========
    // =================================================================================

    private <T> void internalInsert(Handle hande, T entity, EntityMetadata metadata) {
        List<FieldMetadata> fieldsToInsert = getFieldsForInsert(metadata);
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateInsert(metadata, fieldsToInsert);

        Update update = hande.createUpdate(sql);
        bindFields(update, entity, fieldsToInsert);

        if (metadata.hasAutoGeneratedId()) {
            Object generatedId =
                    update.executeAndReturnGeneratedKeys(metadata.getIdField().getColumnName())
                            .mapTo(metadata.getIdField().getFieldType())
                            .one();
            metadata.getIdField().setValue(entity, generatedId);
        } else {
            update.execute();
        }
    }

    private <T> void internalUpdate(Handle handle, T entity, EntityMetadata metadata) {
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateUpdate(metadata, metadata.getFields());

        Update update = handle.createUpdate(sql);
        bindFields(update, entity, metadata.getNonIdFields());
        update.bind(metadata.getIdField().getColumnName(), metadata.getIdField().getValue(entity));
        update.execute();
    }

    private <T> void internalDelete(Handle handle, T entity, EntityMetadata metadata) {
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateDelete(metadata);

        handle.createUpdate(sql)
                .bind(metadata.getIdField().getColumnName(), metadata.getIdField().getValue(entity))
                .execute();
    }

    private String buildPaginatedQuery(String sql, int page, int pageSize) {
        // Asume que todos los dialectos soportados usan LIMIT/OFFSET
        return sql + " LIMIT :limit OFFSET :offset";
    }

    private <T> void generateValues(
            T entity, EntityMetadata metadata, boolean isInsert, boolean isUpdate) {
        for (FieldMetadata field : metadata.getFields()) {
            if (field.hasAnnotation(DefaultValue.class)) {
                DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
                Object currentValue = field.getValue(entity);

                if (currentValue == null) {
                    if (!defaultValue.value().isEmpty() && !defaultValue.generator().isEmpty()) {
                        throw new IllegalStateException(
                                "El campo "
                                        + field.getFieldName()
                                        + " tiene 'value' y 'generator' en @DefaultValue");
                    }

                    if (!defaultValue.value().isEmpty()) {
                        Object parsedValue =
                                parseLiteral(defaultValue.value(), field.getFieldType());
                        field.setValue(entity, parsedValue);
                    } else if (!defaultValue.generator().isEmpty()) {
                        ValueGenerator generator =
                                valueGeneratorRegistry.getGenerator(defaultValue.generator());
                        Object generatedValue = generator.generate(entity, field);
                        field.setValue(entity, generatedValue);
                    }
                }
            }

            if (field.hasAnnotation(Generated.class)) {
                Generated generated = field.getAnnotation(Generated.class);
                boolean shouldGenerate =
                        (isInsert && generated.onInsert()) || (isUpdate && generated.onUpdate());

                if (shouldGenerate) {
                    ValueGenerator generator =
                            valueGeneratorRegistry.getGenerator(generated.generator());
                    Object generatedValue = generator.generate(entity, field);
                    field.setValue(entity, generatedValue);
                }
            }
        }
    }

    private Object parseLiteral(String literal, Class<?> targetType) {
        if (targetType == String.class) {
            return literal;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(literal);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(literal);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(literal);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(literal);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(literal);
        }
        return literal;
    }

    private <T> void validateEntity(T entity, EntityMetadata metadata) {
        List<ValidationError> errors = new ArrayList<>();

        for (FieldMetadata field : metadata.getFields()) {
            Object value = field.getValue(entity);
            List<Validator> validators = validatorRegistry.getValidators(field);

            for (Validator validator : validators) {
                try {
                    validator.validate(value, field);
                } catch (ValidationException e) {
                    errors.addAll(e.getErrors());
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private List<FieldMetadata> getFieldsForInsert(EntityMetadata metadata) {
        return metadata.getFields().stream()
                .filter(f -> !f.isId() || !metadata.hasAutoGeneratedId())
                .collect(Collectors.toList());
    }

    private void bindFields(Update update, Object entity, List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            Object value = field.getValue(entity);

            List<Handler> handlers = handlerRegistry.getHandlersForField(field, dialect);
            for (Handler handler : handlers) {
                value = handler.handleWrite(value, field);
            }

            update.bind(field.getColumnName(), value);
        }
    }

    // =================================================================================
    // ========== Clases Anidadas (Builder y PageResult) ==========
    // =================================================================================

    /**
     * Un contenedor para resultados paginados, que incluye la lista de resultados y los metadatos
     * de paginación (página actual, total de páginas, etc.).
     *
     * @param <T> El tipo de los resultados en la página.
     */
    public static class PageResult<T> {
        private final List<T> results;
        private final int page;
        private final int pageSize;
        private final long totalElements;
        private final long totalPages;

        public PageResult(List<T> results, int page, int pageSize, long totalElements) {
            this.results = results;
            this.page = page;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (totalElements + pageSize - 1) / pageSize;
        }

        /**
         * @return La lista de resultados para esta página.
         */
        public List<T> getResults() {
            return results;
        }

        /**
         * @return El número de la página actual (comenzando en 1).
         */
        public int getPage() {
            return page;
        }

        /**
         * @return El número de items solicitados por página.
         */
        public int getPageSize() {
            return pageSize;
        }

        /**
         * @return El número total de elementos en todas las páginas.
         */
        public long getTotalElements() {
            return totalElements;
        }

        /**
         * @return El número total de páginas.
         */
        public long getTotalPages() {
            return totalPages;
        }

        /**
         * @return true si hay una página siguiente.
         */
        public boolean hasNext() {
            return page < totalPages;
        }

        /**
         * @return true si hay una página anterior.
         */
        public boolean hasPrevious() {
            return page > 1;
        }

        @Override
        public String toString() {
            return "PageResult{"
                    + "page="
                    + page
                    + ", pageSize="
                    + pageSize
                    + ", totalElements="
                    + totalElements
                    + ", totalPages="
                    + totalPages
                    + ", results="
                    + results.size()
                    + '}';
        }
    }

    /**
     * Constructor (Builder) para {@link KurubindDatabase}. Proporciona una forma fluida de
     * configurar la instancia.
     */
    public static class Builder {
        private JdbiProvider jdbiProvider;
        private HandlerRegistry handlerRegistry = new HandlerRegistry();
        private SQLGeneratorRegistry sqlGeneratorRegistry = new SQLGeneratorRegistry();
        private ValidatorRegistry validatorRegistry = new ValidatorRegistry();
        private ValueGeneratorRegistry valueGeneratorRegistry = new ValueGeneratorRegistry();
        private Dialect dialect;

        /**
         * Proporciona una instancia de {@link Jdbi}. Mutuamente excluyente con {@link
         * #withJdbiProvider(JdbiProvider)}.
         *
         * @param jdbi La instancia de Jdbi.
         * @return Este builder.
         */
        public Builder withJdbi(Jdbi jdbi) {
            this.jdbiProvider = () -> jdbi;
            return this;
        }

        /**
         * Proporciona un proveedor de Jdbi, útil para multitenancy. Mutuamente excluyente con
         * {@link #withJdbi(Jdbi)}.
         *
         * @param jdbiProvider El proveedor de Jdbi.
         * @return Este builder.
         */
        public Builder withJdbiProvider(JdbiProvider jdbiProvider) {
            this.jdbiProvider = jdbiProvider;
            return this;
        }

        /**
         * (Opcional) Proporciona un registro de Handlers personalizado.
         *
         * @param handlerRegistry El registro a usar.
         * @return Este builder.
         */
        public Builder withHandlerRegistry(HandlerRegistry handlerRegistry) {
            this.handlerRegistry = handlerRegistry;
            return this;
        }

        /**
         * (Opcional) Proporciona un registro de SQLGenerators personalizado.
         *
         * @param sqlGeneratorRegistry El registro a usar.
         * @return Este builder.
         */
        public Builder withSQLGeneratorRegistry(SQLGeneratorRegistry sqlGeneratorRegistry) {
            this.sqlGeneratorRegistry = sqlGeneratorRegistry;
            return this;
        }

        /**
         * (Opcional) Proporciona un registro de Validators personalizado.
         *
         * @param validatorRegistry El registro a usar.
         * @return Este builder.
         */
        public Builder withValidatorRegistry(ValidatorRegistry validatorRegistry) {
            this.validatorRegistry = validatorRegistry;
            return this;
        }

        /**
         * (Opcional) Proporciona un registro de ValueGenerators personalizado.
         *
         * @param valueGeneratorRegistry El registro a usar.
         * @return Este builder.
         */
        public Builder withValueGeneratorRegistry(ValueGeneratorRegistry valueGeneratorRegistry) {
            this.valueGeneratorRegistry = valueGeneratorRegistry;
            return this;
        }

        /**
         * (Opcional) Especifica el dialecto SQL a usar (ej: "POSTGRESQL").
         *
         * @param dialect El dialecto.
         * @return Este builder.
         */
        public Builder withDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        /**
         * Construye la instancia final de {@link KurubindDatabase}.
         *
         * @return Una instancia de KurubindDatabase lista para usar.
         * @throws IllegalStateException Si no se proporcionó {@code withJdbi} o {@code
         *     withJdbiProvider}.
         */
        public KurubindDatabase build() {
            if (jdbiProvider == null) {
                throw new IllegalStateException("Se debe proporcionar JDBI o JdbiProvider");
            }
            // Aquí se podrían inicializar los validadores base si no se proporcionan
            // ej: if (validatorRegistry.isEmpty()) { addBaseValidators(validatorRegistry); }
            return new KurubindDatabase(this);
        }
    }
}
