package com.roelias.kurubind;

import com.roelias.kurubind.common.PageResult;
import com.roelias.kurubind.exception.InvalidEntityException;
import com.roelias.kurubind.exception.KurubindException;
import com.roelias.kurubind.generator.GeneratorRegistry;
import com.roelias.kurubind.metadata.EntityMetadataCache;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.metadata.MetaEntity;
import com.roelias.kurubind.sql.SqlDialect;
import com.roelias.kurubind.sql.SqlEngine;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KurubindDatabase {


    private final Handle handle;
    private final SqlEngine sqlEngine;

    private KurubindDatabase(Handle handle) {
        this.handle = handle;
        this.sqlEngine = new SqlEngine(SqlDialect.detect(handle));
    }

    /**
     * Create a KurubindDatabase instance from a Jdbi Handle.
     */
    public static KurubindDatabase of(Handle handle) {
        return new KurubindDatabase(handle);
    }

    /**
     * Get the underlying Jdbi Handle for direct access.
     */
    public Handle handle() {
        return handle;
    }

    /**
     * Save an entity (INSERT if new, UPDATE if exists).
     * Applies lifecycle generators automatically.
     */
    public <T> T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        MetaEntity meta = EntityMetadataCache.get(entity.getClass());

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entity.getClass().getName()
            );
        }

        Object idValue = meta.idField().getValue(entity);

        // If ID is null or zero, treat as new entity (INSERT)
        boolean isNew = idValue == null ||
                (idValue instanceof Number && ((Number) idValue).longValue() == 0);

        if (isNew) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    /**
     * Insert a new entity.
     * Applies onInsert generators and returns generated ID if applicable.
     */
    public <T> T insert(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        MetaEntity meta = EntityMetadataCache.get(entity.getClass());

        // Apply INSERT generators
        applyInsertGenerators(entity, meta);

        String sql = sqlEngine.getInsertSql(meta);
        Update update = handle.createUpdate(sql);

        // Bind all insertable fields
        for (FieldMetadata field : meta.insertableFields()) {
            Object value = field.getValue(entity);
            update.bind(field.fieldName(), value);
        }

        // Handle generated ID
        if (meta.hasGeneratedId()) {
            try {
                // For PostgreSQL with RETURNING clause
                if (sqlEngine.getDialect() instanceof com.roelias.kurubind.sql.PostgresDialect) {

                    Object generatedId = update.executeAndReturnGeneratedKeys()
                            .mapTo(meta.idField().type())
                            .one();

                    meta.idField().setValue(entity, generatedId);
                } else {
                    // For MySQL, SQL Server, SQLite - use getGeneratedKeys
                    update.executeAndReturnGeneratedKeys()
                            .map((rs, ctx) -> {
                                try {
                                    Object generatedId = rs.getObject(1, meta.idField().type());
                                    meta.idField().setValue(entity, generatedId);
                                    return generatedId;
                                } catch (Exception e) {
                                    throw new KurubindException("Failed to retrieve generated ID", e);
                                }
                            })
                            .one();
                }
            } catch (Exception e) {
                throw new KurubindException("Failed to insert entity", e);
            }
        } else {
            update.execute();
        }

        return entity;
    }

    /**
     * Update an existing entity.
     * Applies onUpdate generators.
     */
    public <T> T update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        MetaEntity meta = EntityMetadataCache.get(entity.getClass());

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entity.getClass().getName()
            );
        }

        Object idValue = meta.idField().getValue(entity);
        if (idValue == null) {
            throw new InvalidEntityException("Entity ID cannot be null for update");
        }

        // Apply UPDATE generators
        applyUpdateGenerators(entity, meta);

        String sql = sqlEngine.getUpdateSql(meta);
        Update update = handle.createUpdate(sql);

        // Bind ID
        update.bind(meta.idField().fieldName(), idValue);

        // Bind all updatable fields
        for (FieldMetadata field : meta.updatableFields()) {
            Object value = field.getValue(entity);
            update.bind(field.fieldName(), value);
        }

        int rowsAffected = update.execute();
        if (rowsAffected == 0) {
            throw new KurubindException(
                    "Update failed - no rows affected. Entity may not exist."
            );
        }

        return entity;
    }

    /**
     * Delete an entity by its instance.
     */
    public <T> void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        MetaEntity meta = EntityMetadataCache.get(entity.getClass());

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entity.getClass().getName()
            );
        }

        Object idValue = meta.idField().getValue(entity);
        if (idValue == null) {
            throw new InvalidEntityException("Entity ID cannot be null for delete");
        }

        deleteById(entity.getClass(), idValue);
    }

    /**
     * Delete an entity by its ID.
     */
    public <T> void deleteById(Class<T> entityClass, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        MetaEntity meta = EntityMetadataCache.get(entityClass);

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entityClass.getName()
            );
        }

        String sql = sqlEngine.getDeleteSql(meta);
        int rowsAffected = handle.createUpdate(sql)
                .bind(meta.idField().fieldName(), id)
                .execute();

        if (rowsAffected == 0) {
            throw new KurubindException(
                    "Delete failed - no rows affected. Entity may not exist."
            );
        }
    }

    /**
     * Find an entity by its ID.
     */
    public <T> Optional<T> findById(Class<T> entityClass, Object id) {
        if (id == null) {
            return Optional.empty();
        }

        MetaEntity meta = EntityMetadataCache.get(entityClass);

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entityClass.getName()
            );
        }

        String sql = sqlEngine.getSelectByIdSql(meta);
        return handle.createQuery(sql)
                .bind(meta.idField().fieldName(), id)
                .mapTo(entityClass)
                .findOne();
    }

    /**
     * Check if an entity exists by ID.
     */
    public <T> boolean existsById(Class<T> entityClass, Object id) {
        if (id == null) {
            return false;
        }

        MetaEntity meta = EntityMetadataCache.get(entityClass);

        if (!meta.hasId()) {
            throw new InvalidEntityException(
                    "Entity must have an @Id field: " + entityClass.getName()
            );
        }

        String sql = sqlEngine.getExistsByIdSql(meta);
        return handle.createQuery(sql)
                .bind(meta.idField().fieldName(), id)
                .mapTo(Boolean.class)
                .one();
    }

    /**
     * Find all entities of a type.
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        MetaEntity meta = EntityMetadataCache.get(entityClass);
        String sql = sqlEngine.getSelectAllSql(meta);
        return handle.createQuery(sql)
                .mapTo(entityClass)
                .list();
    }

    /**
     * Find first entity of a type.
     */
    public <T> Optional<T> findFirst(Class<T> entityClass) {
        MetaEntity meta = EntityMetadataCache.get(entityClass);
        String sql = sqlEngine.getSelectAllSql(meta) + " " +
                sqlEngine.buildPagination(1, 0);
        return handle.createQuery(sql)
                .mapTo(entityClass)
                .findOne();
    }

    /**
     * Count all entities of a type.
     */
    public <T> long count(Class<T> entityClass) {
        MetaEntity meta = EntityMetadataCache.get(entityClass);
        String sql = sqlEngine.getCountSql(meta);
        return handle.createQuery(sql)
                .mapTo(Long.class)
                .one();
    }

    /**
     * Execute a custom query and map results.
     */
    public <T> List<T> query(String sql, Class<T> resultClass, Map<String, Object> params) {
        Query query = handle.createQuery(sql);

        if (params != null) {
            params.forEach(query::bind);
        }

        return query.mapTo(resultClass).list();
    }

    /**
     * Execute a custom query and map to single result.
     */
    public <T> Optional<T> queryOne(String sql, Class<T> resultClass, Map<String, Object> params) {
        Query query = handle.createQuery(sql);

        if (params != null) {
            params.forEach(query::bind);
        }

        return query.mapTo(resultClass).findOne();
    }

    /**
     * Find all entities with pagination.
     */
    public <T> PageResult<T> findAllPaginated(Class<T> entityClass, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }

        MetaEntity meta = EntityMetadataCache.get(entityClass);

        // Get total count
        long total = count(entityClass);

        // Get page data
        String sql = sqlEngine.getSelectAllSql(meta) + " " +
                sqlEngine.buildPagination(size, page * size);

        List<T> content = handle.createQuery(sql)
                .mapTo(entityClass)
                .list();

        return new PageResult<>(content, page, size, total);
    }

    /**
     * Execute custom query with pagination.
     */
    public <T> PageResult<T> findByPage(
            String sql,
            Class<T> resultClass,
            Map<String, Object> params,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }

        // Build count query
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        Query countQuery = handle.createQuery(countSql);
        if (params != null) {
            params.forEach(countQuery::bind);
        }
        long total = countQuery.mapTo(Long.class).one();

        // Build paginated query
        String paginatedSql = sql + " " + sqlEngine.buildPagination(size, page * size);
        Query dataQuery = handle.createQuery(paginatedSql);
        if (params != null) {
            params.forEach(dataQuery::bind);
        }
        List<T> content = dataQuery.mapTo(resultClass).list();

        return new PageResult<>(content, page, size, total);
    }

    /**
     * Batch insert entities.
     */
    public <T> void saveAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (T entity : entities) {
            insert(entity);
        }
    }

    /**
     * Batch update entities.
     */
    public <T> void updateAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (T entity : entities) {
            update(entity);
        }
    }

    /**
     * Batch delete entities.
     */
    public <T> void deleteAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (T entity : entities) {
            delete(entity);
        }
    }

    /**
     * Apply generators for INSERT operation.
     */
    private <T> void applyInsertGenerators(T entity, MetaEntity meta) {
        for (FieldMetadata field : meta.fields()) {
            if (field.hasGenerators() && !field.getGeneratorsForInsert().isEmpty()) {
                GeneratorRegistry.applyInsertGenerators(entity, field, handle);
            }
        }
    }

    /**
     * Apply generators for UPDATE operation.
     */
    private <T> void applyUpdateGenerators(T entity, MetaEntity meta) {
        for (FieldMetadata field : meta.fields()) {
            if (field.hasGenerators() && !field.getGeneratorsForUpdate().isEmpty()) {
                GeneratorRegistry.applyUpdateGenerators(entity, field, handle);
            }
        }
    }
}

