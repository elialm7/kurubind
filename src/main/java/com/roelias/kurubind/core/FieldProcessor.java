package com.roelias.kurubind.core;
import java.lang.reflect.Field;
/**
 * Field processor for annotation-specific logic.
 */
public interface FieldProcessor {

    /**
     * Processes field before INSERT operation.
     */
    void processForInsert(Field field, Object entity) throws IllegalAccessException;

    /**
     * Processes field before UPDATE operation.
     */
    void processForUpdate(Field field, Object entity) throws IllegalAccessException;

    /**
     * Transforms field value for database storage.
     */
    Object transformForStorage(Field field, Object value);

    /**
     * Transforms database value for field assignment.
     */
    Object transformFromStorage(Field field, Object dbValue);

    /**
     * Generates SQL column definition for DDL.
     */
    String generateColumnDefinition(Field field, String columnName);
}
