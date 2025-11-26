package com.roelias.kurubind.base;

import com.roelias.kurubind.metadata.FieldMetadata;

public interface ValueGenerator {
    Object generate(Object entity, FieldMetadata field);

    boolean generateOnInsert();

    boolean generateOnUpdate();
}
