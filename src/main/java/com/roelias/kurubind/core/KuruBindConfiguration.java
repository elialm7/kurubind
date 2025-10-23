package com.roelias.kurubind.core;

import java.util.Optional;

/**
 *Main configuration interface for KuruBind.
 * Allows registration of custom dialects, processor, and mappers.
 */

public interface KuruBindConfiguration {


    /**
     * Registers a dialect implementation.
     */
    void registerDialect(String name, DialectImplementation dialect);

    /**
     * Registers a field processor for specific annotations.
     */
    void registerFieldProcessor(Class<? extends java.lang.annotation.Annotation> annotation,
                                FieldProcessor processor);

    /**
     * Registers a type mapper for custom type conversions.
     */
    void registerTypeMapper(Class<?> type, TypeMapper<?> mapper);

    /**
     * Gets a registered dialect by name.
     */
    Optional<DialectImplementation> getDialect(String name);


}
