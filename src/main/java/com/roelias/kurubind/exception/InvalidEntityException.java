package com.roelias.kurubind.exception;

/**
 * Thrown when an entity operation is invalid
 */
public class InvalidEntityException extends KurubindException {
    public InvalidEntityException(String message) {
        super(message);
    }

    public InvalidEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
