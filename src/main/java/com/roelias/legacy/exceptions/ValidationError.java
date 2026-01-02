package com.roelias.legacy.exceptions;

public class ValidationError {

    private final String fieldName;
    private final String message;

    public ValidationError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        if (fieldName != null) {
            return fieldName + ": " + message;
        }
        return message;
    }


}
