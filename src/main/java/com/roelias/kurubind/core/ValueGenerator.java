package com.roelias.kurubind.core;

import com.roelias.kurubind.exceptions.ValidationException;
import com.roelias.kurubind.metadata.FieldMetadata;

public interface ValueGenerator {
    Object generate(Object entity, FieldMetadata field);
    boolean generateOnInsert();
    boolean generateOnUpdate();
}
