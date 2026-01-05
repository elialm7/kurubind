package com.roelias.kurubind.metadata;


import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

public record MetaEntity(


        Class<?> entityClass,
        String tableName,
        String schema,
        List<FieldMetadata> fields,
        FieldMetadata idField,
        boolean isRecord,
        Constructor<?> constructor

) {


    public boolean hasId() {
        return idField != null;
    }

    public boolean hasGeneratedId() {
        return hasId() && idField.isGenerated();
    }

    public String fullTableName() {
        return schema.isEmpty() ? tableName : schema + "." + tableName;
    }

    public List<FieldMetadata> insertableFields() {
        return fields.stream()
                .filter(f -> !f.isGenerated()).collect(Collectors.toList());
    }

    public List<FieldMetadata> updatableFields() {
        return fields.stream().filter(f -> !f.isId()).collect(Collectors.toList());
    }
}
