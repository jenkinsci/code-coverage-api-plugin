package io.jenkins.plugins.coverage.model.exception;

/**
 * Exception which is thrown when there are files with ambiguous paths which results in a not unique coverage report.
 *
 * @author Florian Orendi
 */
public class AmbiguousPathException extends RuntimeException {
    private static final long serialVersionUID = -7251876482978584604L;

    /**
     * Constructor which creates an exception with a message.
     *
     * @param message
     *         The message
     */
    public AmbiguousPathException(final String message) {
        super(message);
    }
}
