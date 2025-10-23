package com.roelias.kurubind.core;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

/**
        * Main KuruBind interface - the public API.
 */
public interface KuruBind<E, ID> extends CrudOperations<E, ID> {


    /**
     * Gets the table name.
     */
    String getTableName();

    /**
     * Gets the ID column name.
     */
    String getIdColumnName();

    /**
     * Gets the entity class.
     */
    Class<E> getEntityClass();

    /**
     * Gets the ID type class.
     */
    Class<ID> getIdType();

    /**
     * Gets a custom row mapper for DTOs.
     */
    <T> RowMapper<T> getCustomRowMapper(Class<T> targetClass);

    /**
     * Generates DDL CREATE TABLE statement.
     */
    String getTableCreationTemplate();

    /**
     * Builds custom SQL queries with entity mapping.
     */
    QueryBuilder<E> query();

    /**
     * Gets the underlying JDBI instance.
     */
    Jdbi getJdbi();

    /**
     * Gets table metadata.
     */
    TableMetadata getMetadata();
}
