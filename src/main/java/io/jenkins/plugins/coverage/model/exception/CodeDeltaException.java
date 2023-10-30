package io.jenkins.plugins.coverage.model.exception;

/**
 * Exception which is thrown when preprocessing the code delta failed.
 *
 * @author Florian Orendi
 */
public class CodeDeltaException extends Exception {
    private static final long serialVersionUID = -7255072653278584604L;

    /**
     * Constructor which creates an exception with a message.
     *
     * @param message
     *         The message
     */
    public CodeDeltaException(final String message) {
        super(message);
    }
}
