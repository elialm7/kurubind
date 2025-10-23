package com.roelias.kurubind.core;

import java.lang.reflect.Field;

/**
 * Dialect-specific implementation interface.
 */
public interface DialectImplementation {
    String getName();

    SqlGenerator getSqlGenerator();

    ParameterBinder getParameterBinder();

    <E> EntityMapper<E> getEntityMapper(Class<E> entityClass);

    String getAutoIncrementSyntax();

    String getCurrentTimestampFunction();

    String getUuidGenerationFunction();

    String formatTableName(String schema, String table);

    String formatColumnType(Field field, String baseType);
}
