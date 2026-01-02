package com.roelias.legacy.base;

import com.roelias.legacy.metadata.EntityMetadata;
import com.roelias.legacy.metadata.FieldMetadata;

import java.util.List;

public interface SQLGenerator {

    String generateInsert(EntityMetadata meta, List<FieldMetadata> fields);

    String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields);

    String generateDelete(EntityMetadata meta);

    String generateSelect(EntityMetadata meta);

    String getPlaceholder(FieldMetadata field);

    String generateSelectById(EntityMetadata meta);

    String generateCount(EntityMetadata meta);

}
