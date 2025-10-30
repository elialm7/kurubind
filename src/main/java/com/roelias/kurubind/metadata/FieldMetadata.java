package com.roelias.kurubind.metadata;

import com.roelias.kurubind.annotations.Column;
import com.roelias.kurubind.annotations.Id;
import com.roelias.kurubind.annotations.Transient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        for(Annotation annotation : field.getAnnotations()) {
            this.annotations.put(annotation.annotationType(), annotation);
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

    public Collection<Annotation> getAllAnnotations() {
        return annotations.values();
    }

}
