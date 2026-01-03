package com.roelias.kurubind.generator;

import com.roelias.kurubind.metadata.FieldMetadata;
import org.jdbi.v3.core.Handle;

/**
 * Functional interface for generating values for entity fields.
 */
@FunctionalInterface
public interface ValueGenerator {


    /**
     * Generate a value for a field.
     *
     * @param entity The entity being saved
     * @param field  Field metadata
     * @param handle Jdbi Handle for database access
     * @return Generated value
     */
    Object generate(Object entity, FieldMetadata field, Handle handle) throws Exception;
}
