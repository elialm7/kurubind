package com.roelias.legacy.metadata;

import com.roelias.legacy.annotations.Column;
import com.roelias.legacy.annotations.Id;
import com.roelias.legacy.annotations.Transient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class FieldMetadata {

    private final Field field;
    private final String fieldName;
    private final String columnName;
    private final boolean isId;
    private final boolean isTransient;
    private final Map<Class<? extends Annotation>, Annotation> annotations;


    public FieldMetadata(Field field) {
        this.field = field;
        this.field.setAccessible(true);
        this.fieldName = field.getName();
        Column columnAnnotation = field.getAnnotation(Column.class);
        this.columnName = columnAnnotation != null ? columnAnnotation.value() : field.getName();
        this.isId = field.isAnnotationPresent(Id.class);
        this.isTransient = field.isAnnotationPresent(Transient.class);

        this.annotations = new HashMap<>();
        collectAnnotationRecursively(field.getAnnotations(), this.annotations, new HashSet<>());
    }

    private void collectAnnotationRecursively(
            Annotation[] annotationsToScan,
            Map<Class<? extends Annotation>, Annotation> collected,
            Set<Class<? extends Annotation>> visited
    ) {
        for (Annotation annotation : annotationsToScan) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type.getPackage().getName().startsWith("java.lang.annotation") || !visited.add(type)) {
                continue;
            }
            if (!collected.containsKey(type)) {
                collected.put(type, annotation);
            }
            collectAnnotationRecursively(type.getAnnotations(), collected, visited);
        }

    }


    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isId() {
        return isId;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.containsKey(annotationType);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) annotations.get(annotationType);
    }

    public Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get value from field: " + fieldName, e);
        }
    }

    public void setValue(Object entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set value to field: " + fieldName, e);
        }
    }

    public Class<?> getFieldType() {
        return field.getType();
    }

    public Type getGenericType() {
        return field.getGenericType();
    }

    public Collection<Annotation> getAllAnnotations() {
        return annotations.values();
    }

}
