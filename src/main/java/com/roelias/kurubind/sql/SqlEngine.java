package com.roelias.kurubind.sql;

import com.roelias.kurubind.metadata.MetaEntity;

import java.util.concurrent.ConcurrentHashMap;

public class SqlEngine {
    private final SqlDialect dialect;
    private final ConcurrentHashMap<String, String> sqlCache;

    public SqlEngine(SqlDialect dialect) {
        this.dialect = dialect;
        this.sqlCache = new ConcurrentHashMap<>();
    }

    /**
     * Get INSERT SQL for an entity (cached).
     */
    public String getInsertSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("insert", meta),
                k -> dialect.buildInsert(meta)
        );
    }

    /**
     * Get UPDATE SQL for an entity (cached).
     */
    public String getUpdateSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("update", meta),
                k -> dialect.buildUpdate(meta)
        );
    }

    /**
     * Get DELETE SQL for an entity (cached).
     */
    public String getDeleteSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("delete", meta),
                k -> dialect.buildDelete(meta)
        );
    }

    /**
     * Get SELECT by ID SQL for an entity (cached).
     */
    public String getSelectByIdSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("selectById", meta),
                k -> dialect.buildSelectById(meta)
        );
    }

    /**
     * Get SELECT all SQL for an entity (cached).
     */
    public String getSelectAllSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("selectAll", meta),
                k -> dialect.buildSelectAll(meta)
        );
    }

    /**
     * Get COUNT SQL for an entity (cached).
     */
    public String getCountSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("count", meta),
                k -> dialect.buildCount(meta)
        );
    }

    /**
     * Get EXISTS by ID SQL for an entity (cached).
     */
    public String getExistsByIdSql(MetaEntity meta) {
        return sqlCache.computeIfAbsent(
                cacheKey("existsById", meta),
                k -> dialect.buildExistsById(meta)
        );
    }

    /**
     * Build pagination clause (not cached as it varies by parameters).
     */
    public String buildPagination(int limit, int offset) {
        return dialect.buildPagination(limit, offset);
    }

    /**
     * Get the underlying dialect.
     */
    public SqlDialect getDialect() {
        return dialect;
    }

    /**
     * Clear the SQL cache (useful for testing).
     */
    public void clearCache() {
        sqlCache.clear();
    }

    /**
     * Generate cache key for SQL statements.
     */
    private String cacheKey(String operation, MetaEntity meta) {
        return operation + ":" + meta.entityClass().getName();
    }
}
