package com.roelias.kurubind.exception;

/*
 * Thrown when sql generation fails
 */
public class SqlGenerationException extends KurubindException {
    public SqlGenerationException(String message) {
        super(message);
    }

    public SqlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
