package com.roelias.kurubind.metadata;

import com.roelias.kurubind.exception.EntityMetadataException;
import com.roelias.kurubind.generator.GeneratorConfig;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public record FieldMetadata(
        String fieldName,
        String columnName,
        Class<?> type,
        MethodHandle getter,
        MethodHandle setter,
        boolean isId,
        boolean isGenerated,
        List<GeneratorConfig> generators,
        Field field
) {


    public Object getValue(Object entity) {
        try {
            return getter.invoke(entity);
        } catch (Throwable e) {
            throw new EntityMetadataException(
                    "Failed  to get value for field: " + fieldName, e
            );
        }
    }

    public void setValue(Object entity, Object value) {
        if (setter == null) {
            throw new EntityMetadataException(
                    "Cannot set value for inmutable field" + fieldName
            );
        }

        try {
            setter.invoke(entity, value);
        } catch (Throwable e) {
            throw new EntityMetadataException(
                    "Failed to set value for field: " + fieldName, e
            );
        }
    }

    public boolean hasGenerators() {
        return !generators.isEmpty();
    }

    public List<GeneratorConfig> getGeneratorsForInsert() {
        return generators.stream().filter(GeneratorConfig::onInsert).collect(Collectors.toList());
    }

    public List<GeneratorConfig> getGeneratorsForUpdate() {
        return generators.stream().filter(GeneratorConfig::onUpdate).collect(Collectors.toList());
    }


}
