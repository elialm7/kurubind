package com.roelias.kurubind.core;
/**
 * Type mapper for custom type conversions.
 */
public interface TypeMapper<T> {
    Object toDatabase(T value);

    T fromDatabase(Object dbValue);

    String getSqlType();
}
