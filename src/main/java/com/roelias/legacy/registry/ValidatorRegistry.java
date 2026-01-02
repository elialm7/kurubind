package com.roelias.legacy.registry;

import com.roelias.legacy.base.Validator;
import com.roelias.legacy.metadata.FieldMetadata;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidatorRegistry {
    private final Map<Class<? extends Annotation>, Validator> validators;

    public ValidatorRegistry() {
        this.validators = new HashMap<>();
    }

    public void register(Class<? extends Annotation> annotationType, Validator validator) {
        validators.put(annotationType, validator);
    }

    public List<Validator> getValidators(FieldMetadata field) {
        List<Validator> fieldValidators = new ArrayList<>();
        for (Annotation annotation : field.getAllAnnotations()) {
            Validator validator = validators.get(annotation.annotationType());
            if (validator != null) {
                fieldValidators.add(validator);
            }
        }
        return fieldValidators;
    }
}
