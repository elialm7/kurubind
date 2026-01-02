package com.roelias.kurubind.exception;

/*
 * Custom exception class for Kurubind-related errors.
 */
public class KurubindException extends RuntimeException {
    public KurubindException(String message) {
        super(message);
    }

    public KurubindException(String message, Throwable cause) {
        super(message, cause);
    }
}
