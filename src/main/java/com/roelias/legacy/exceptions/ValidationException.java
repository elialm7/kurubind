package com.roelias.legacy.exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }

    public ValidationException(String message) {
        super(message);
        this.errors = java.util.Collections.singletonList(
                new ValidationError(null, message)
        );
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    private static String buildMessage(List<ValidationError> errors) {
        return "Validation failed: " +
                errors.stream()
                        .map(ValidationError::toString)
                        .collect(java.util.stream.Collectors.joining("; "));
    }

}
