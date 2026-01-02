package com.roelias.legacy.base;

import com.roelias.legacy.exceptions.ValidationException;
import com.roelias.legacy.metadata.FieldMetadata;

public interface Validator {
    void validate(Object value, FieldMetadata field) throws ValidationException;
}
