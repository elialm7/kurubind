package com.roelias.kurubind.exception;

/**
 * Thrown when entity metadata cannot be extracted or is invalid.
 */

public class EntityMetadataException extends KurubindException {
    public EntityMetadataException(String message) {
        super(message);
    }

    public EntityMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
