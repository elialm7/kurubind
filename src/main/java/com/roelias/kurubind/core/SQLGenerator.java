package com.roelias.kurubind.core;

import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.util.List;

public interface SQLGenerator {

    String generateInsert(EntityMetadata meta, List<FieldMetadata> fields);
    String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields);
    String generateDelete(EntityMetadata meta);
    String generateSelect(EntityMetadata meta);
    String getPlaceholder(FieldMetadata field);

}
