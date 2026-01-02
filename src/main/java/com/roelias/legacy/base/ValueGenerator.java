package com.roelias.legacy.base;

import com.roelias.legacy.metadata.FieldMetadata;

public interface ValueGenerator {
    Object generate(Object entity, FieldMetadata field);

    boolean generateOnInsert();

    boolean generateOnUpdate();
}
