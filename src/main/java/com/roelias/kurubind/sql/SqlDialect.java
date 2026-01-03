package com.roelias.kurubind.sql;


import com.roelias.kurubind.metadata.EntityMetaData;

/**
 * Interface for database-specific SQL generation.
 */
public interface SqlDialect {


    /**
     * Quote an identifier (table/column name) according to database rules.
     */
    String quoteIdentifier(String identifier);

    /**
     * Build INSERT statement.
     */
    String buildInsert(EntityMetaData meta);

    /**
     * Build UPDATE statement.
     */
    String buildUpdate(EntityMetaData meta);

    /**
     * Build DELETE statement.
     */
    String buildDelete(EntityMetaData meta);

    /**
     * Build SELECT by ID statement.
     */
    String buildSelectById(EntityMetaData meta);

    /**
     * Build SELECT all statement.
     */
    String buildSelectAll(EntityMetaData meta);

    /**
     * Build COUNT statement.
     */
    String buildCount(EntityMetaData meta);

    /**
     * Build EXISTS by ID statement.
     */
    String buildExistsById(EntityMetaData meta);

    /**
     * Build pagination clause (LIMIT/OFFSET).
     */
    String buildPagination(int limit, int offset);
}
