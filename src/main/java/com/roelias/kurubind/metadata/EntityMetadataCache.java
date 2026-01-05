package com.roelias.kurubind.metadata;

import com.roelias.kurubind.annotation.*;
import com.roelias.kurubind.exception.EntityMetadataException;
import com.roelias.kurubind.generator.GeneratorConfig;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches entity metadata to avoid repeated reflection.
 * Thread-safe and optimized for high-performance access.
 */
public class EntityMetadataCache {


    private static final Map<Class<?>, MetaEntity> cache = new ConcurrentHashMap<>();


    /**
     * Get or build metadata for an entity class.
     */
    public static MetaEntity get(Class<?> clazz) {
        return cache.computeIfAbsent(clazz, EntityMetadataCache::build);
    }

    private static MetaEntity build(Class<?> entityClass) {

        try {
            //extract tbale information
            String tableName = extractTableName(entityClass);
            String schema = extractSchema(entityClass);

            boolean isRecord = entityClass.isRecord();
            //build fields
            List<FieldMetadata> fields = isRecord ? buildRecordFields(entityClass) : buildPojoFields(entityClass);

            //find id field
            FieldMetadata idField = fields.stream().filter(FieldMetadata::isId).findFirst().orElse(null);


            //Get Consctructor
            Constructor<?> constructor = null;
            if (isRecord) {
                constructor = findCanonicalConstructor(entityClass, fields);
            } else {
                constructor = entityClass.getDeclaredConstructor();
                constructor.setAccessible(true);
            }
            return new MetaEntity(
                    entityClass,
                    tableName,
                    schema,
                    fields,
                    idField,
                    isRecord,
                    constructor
            );
        } catch (Exception e) {
            throw new EntityMetadataException("Failed to build metadata for entity: " + entityClass.getName()
                    , e);
        }
    }

    private static Constructor<?> findCanonicalConstructor(Class<?> cls, List<FieldMetadata> fields) throws NoSuchMethodException {
        Class<?>[] parameTypes = fields.stream().map(FieldMetadata::type).toArray(Class<?>[]::new);
        return cls.getDeclaredConstructor(parameTypes);
    }

    private static List<FieldMetadata> buildRecordFields(Class<?> entityClass) {
        List<FieldMetadata> fields = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (RecordComponent component : entityClass.getRecordComponents()) {
            try {
                Method accesor = component.getAccessor();
                MethodHandle getter = lookup.unreflect(accesor);
                MethodHandle setter = null;
                Field field = entityClass.getDeclaredField(component.getName());
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                fields.add(buildFieldMetadata(field, getter, setter));

            } catch (Exception e) {
                throw new EntityMetadataException(
                        "Failed to build metadata for record component: " + component.getName(), e
                );
            }
        }

        return fields;
    }

    private static List<FieldMetadata> buildPojoFields(Class<?> entityClass) {
        List<FieldMetadata> fields = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Field field : getAllFields(entityClass)) {

            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                MethodHandle getter = lookup.unreflectGetter(field);
                MethodHandle setter = lookup.unreflectSetter(field);
                fields.add(buildFieldMetadata(field, getter, setter));
            } catch (Exception e) {
                throw new EntityMetadataException(
                        "Failed to build metadata for field: " + field.getName(), e
                );
            }


        }
        return fields;
    }

    private static FieldMetadata buildFieldMetadata(
            Field field,
            MethodHandle getter,
            MethodHandle setter
    ) {
        String fieldName = field.getName();
        String columnName = extractColumnName(field);
        Class<?> fieldType = field.getType();

        boolean isId = field.isAnnotationPresent(Id.class);
        boolean isGenerated = false;

        if (isId) {
            Id id = field.getAnnotation(Id.class);
            isGenerated = id.generated();
        }
        List<GeneratorConfig> generators = extractGenerators(field);

        return new FieldMetadata(
                fieldName,
                columnName,
                fieldType,
                getter,
                setter,
                isId,
                isGenerated,
                generators,
                field
        );

    }


    private static List<GeneratorConfig> extractGenerators(Field field) {
        List<GeneratorConfig> generators = new ArrayList<>();

        Generated[] directGenerators = field.getAnnotationsByType(Generated.class);

        for (Generated gen : directGenerators) {
            generators.add(
                    new GeneratorConfig(
                            gen.value(),
                            gen.onInsert(),
                            gen.onUpdate()
                    )
            );
        }


        // meta annotations check for @generated annotations

        for (Annotation an : field.getAnnotations()) {
            Generated[] metaGen = an.annotationType().getAnnotationsByType(Generated.class);
            for (Generated gen : metaGen) {
                generators.add(
                        new GeneratorConfig(
                                gen.value(),
                                gen.onInsert(),
                                gen.onUpdate())
                );
            }

        }

        return generators;

    }


    private static String extractColumnName(Field field) {


        Column column = field.getAnnotation(Column.class);

        if (column != null && !column.value().isEmpty()) {
            return column.value();
        }

        // Check other annotations for meta column info
        for (Annotation annotation : field.getAnnotations()) {
            Column metaColumn = annotation.annotationType().getAnnotation(Column.class);

            if (metaColumn != null && !metaColumn.value().isEmpty()) {
                return metaColumn.value();
            }

        }

        return field.getName();


    }

    private static List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = cls;


        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;

    }

    private static String extractSchema(Class<?> cls) {
        Table table = cls.getAnnotation(Table.class);
        if (table != null && !table.schema().isEmpty()) {
            return table.schema();
        }
        return "";
    }

    private static String extractTableName(Class<?> cls) {
        Table table = cls.getAnnotation(Table.class);
        if (table != null && !table.value().isEmpty()) {
            return table.value();
        }
        return cls.getSimpleName();

    }


}
