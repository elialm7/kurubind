package com.roelias.kurubind.exception;

/*

 * Thrown when a value generator is not found our fails
 */
public class GeneratorException extends KurubindException {
    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
