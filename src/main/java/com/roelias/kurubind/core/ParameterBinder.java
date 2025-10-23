package com.roelias.kurubind.core;

import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Update;

/**
 * Parameter binding interface for JDBI statements.
 */
public interface ParameterBinder {
    void bindInsertParameters(Update update, Object entity, TableMetadata metadata);

    void bindUpdateParameters(Update update, Object entity, TableMetadata metadata);

    void bindBatchParameters(PreparedBatch batch, Object entity, TableMetadata metadata);
}
