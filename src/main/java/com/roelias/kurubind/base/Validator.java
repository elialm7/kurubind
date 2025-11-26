package com.roelias.kurubind.base;

import com.roelias.kurubind.exceptions.ValidationException;
import com.roelias.kurubind.metadata.FieldMetadata;

public interface Validator {
    void validate(Object value, FieldMetadata field) throws ValidationException;
}
