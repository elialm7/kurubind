package com.roelias.kurubind;

import com.roelias.kurubind.common.PageResult;
import com.roelias.kurubind.exception.KurubindException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generic repository class that provides common CRUD operations
 * with automatic transaction management and error handling.
 *
 * @param <T> the entity type
 */
public class KuruRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(KuruRepository.class);

    private final Jdbi jdbi;
    private final Class<T> entityClass;

    public KuruRepository(Jdbi jdbi, Class<T> entityClass) {
        if (jdbi == null) {
            throw new IllegalArgumentException("Jdbi instance cannot be null");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        this.jdbi = jdbi;
        this.entityClass = entityClass;
    }

    /**
     * Save an entity (insert if new, update if exists).
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws KurubindException if save operation fails
     */
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.save(entity);
            });
        } catch (Exception e) {
            log.error("Failed to save entity: {}", entity.getClass().getSimpleName(), e);
            throw new KurubindException("Failed to save entity", e);
        }
    }

    /**
     * Insert a new entity.
     *
     * @param entity the entity to insert
     * @return the inserted entity with generated ID
     * @throws KurubindException if insert operation fails
     */
    public T insert(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.insert(entity);
            });
        } catch (Exception e) {
            log.error("Failed to insert entity: {}", entity.getClass().getSimpleName(), e);
            throw new KurubindException("Failed to insert entity", e);
        }
    }

    /**
     * Update an existing entity.
     *
     * @param entity the entity to update
     * @return the updated entity
     * @throws KurubindException if update operation fails
     */
    public T update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.update(entity);
            });
        } catch (Exception e) {
            log.error("Failed to update entity: {}", entity.getClass().getSimpleName(), e);
            throw new KurubindException("Failed to update entity", e);
        }
    }

    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            return jdbi.inTransaction(handle -> {
                try {
                    var db = KurubindDatabase.of(handle);
                    db.delete(entity);
                    return true;
                } catch (KurubindException e) {
                    log.warn("Failed to delete entity: {}", e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error during delete operation", e);
            return false;
        }
    }

    /**
     * Delete an entity by its ID.
     *
     * @param id the entity ID
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteById(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        try {
            return jdbi.inTransaction(handle -> {
                try {
                    var db = KurubindDatabase.of(handle);
                    db.deleteById(entityClass, id);
                    return true;
                } catch (KurubindException e) {
                    log.warn("Failed to delete entity by ID {}: {}", id, e.getMessage());
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error during delete by ID operation", e);
            return false;
        }
    }

    /**
     * Find an entity by its ID.
     *
     * @param id the entity ID
     * @return Optional containing the entity if found, empty otherwise
     */
    public Optional<T> findById(Object id) {
        if (id == null) {
            return Optional.empty();
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.findById(entityClass, id);
            });
        } catch (Exception e) {
            log.error("Failed to find entity by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Check if an entity exists by ID.
     *
     * @param id the entity ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(Object id) {
        if (id == null) {
            return false;
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.existsById(entityClass, id);
            });
        } catch (Exception e) {
            log.error("Failed to check existence by ID: {}", id, e);
            return false;
        }
    }

    /**
     * Find all entities.
     *
     * @return list of all entities, empty list if none found
     */
    public List<T> findAll() {
        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.findAll(entityClass);
            });
        } catch (Exception e) {
            log.error("Failed to find all entities: {}", entityClass.getSimpleName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Find first entity.
     *
     * @return Optional containing the first entity if found, empty otherwise
     */
    public Optional<T> findFirst() {
        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.findFirst(entityClass);
            });
        } catch (Exception e) {
            log.error("Failed to find first entity: {}", entityClass.getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * Count all entities.
     *
     * @return total count of entities
     */
    public long count() {
        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.count(entityClass);
            });
        } catch (Exception e) {
            log.error("Failed to count entities: {}", entityClass.getSimpleName(), e);
            return 0L;
        }
    }

    /**
     * Find entities with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return PageResult containing the page data
     * @throws IllegalArgumentException if page or size are invalid
     */
    public PageResult<T> findAll(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.findAllPaginated(entityClass, page, size);
            });
        } catch (Exception e) {
            log.error("Failed to find paginated entities: {}", entityClass.getSimpleName(), e);
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }
    }

    /**
     * Execute a custom query and return results.
     *
     * @param sql    the SQL query
     * @param params the query parameters
     * @return list of results, empty list if none found
     */
    public List<T> query(String sql, Map<String, Object> params) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.query(sql, entityClass, params);
            });
        } catch (Exception e) {
            log.error("Failed to execute custom query: {}", sql, e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute a custom query and return single result.
     *
     * @param sql    the SQL query
     * @param params the query parameters
     * @return Optional containing the result if found, empty otherwise
     */
    public Optional<T> queryOne(String sql, Map<String, Object> params) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.queryOne(sql, entityClass, params);
            });
        } catch (Exception e) {
            log.error("Failed to execute custom query: {}", sql, e);
            return Optional.empty();
        }
    }

    /**
     * Execute custom query with pagination.
     *
     * @param sql    the SQL query
     * @param params the query parameters
     * @param page   the page number (0-based)
     * @param size   the page size
     * @return PageResult containing the page data
     */
    public PageResult<T> query(String sql, Map<String, Object> params, int page, int size) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0");
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                return db.findByPage(sql, entityClass, params, page, size);
            });
        } catch (Exception e) {
            log.error("Failed to execute paginated custom query: {}", sql, e);
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }
    }

    /**
     * Save multiple entities (batch insert).
     *
     * @param entities the entities to save
     * @return number of entities saved successfully
     */
    public int saveAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                int count = 0;
                for (T entity : entities) {
                    try {
                        db.insert(entity);
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to insert entity in batch: {}", e.getMessage());
                    }
                }
                return count;
            });
        } catch (Exception e) {
            log.error("Failed to save all entities", e);
            return 0;
        }
    }

    /**
     * Update multiple entities (batch update).
     *
     * @param entities the entities to update
     * @return number of entities updated successfully
     */
    public int updateAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                int count = 0;
                for (T entity : entities) {
                    try {
                        db.update(entity);
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to update entity in batch: {}", e.getMessage());
                    }
                }
                return count;
            });
        } catch (Exception e) {
            log.error("Failed to update all entities", e);
            return 0;
        }
    }

    /**
     * Delete multiple entities (batch delete).
     *
     * @param entities the entities to delete
     * @return number of entities deleted successfully
     */
    public int deleteAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        try {
            return jdbi.inTransaction(handle -> {
                var db = KurubindDatabase.of(handle);
                int count = 0;
                for (T entity : entities) {
                    try {
                        db.delete(entity);
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to delete entity in batch: {}", e.getMessage());
                    }
                }
                return count;
            });
        } catch (Exception e) {
            log.error("Failed to delete all entities", e);
            return 0;
        }
    }

    /**
     * Delete all entities of this type.
     * WARNING: This will delete all records from the table.
     *
     * @return number of entities deleted
     */
    public int deleteAll() {
        try {
            List<T> allEntities = findAll();
            return deleteAll(allEntities);
        } catch (Exception e) {
            log.error("Failed to delete all entities: {}", entityClass.getSimpleName(), e);
            return 0;
        }
    }

    /**
     * Get the entity class managed by this repository.
     *
     * @return the entity class
     */
    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the underlying Jdbi instance.
     *
     * @return the Jdbi instance
     */
    public Jdbi getJdbi() {
        return jdbi;
    }
}