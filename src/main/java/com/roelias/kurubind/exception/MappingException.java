package com.roelias.kurubind.exception;

/**
 * Thrown when entity mapping fails
 */
public class MappingException extends KurubindException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
