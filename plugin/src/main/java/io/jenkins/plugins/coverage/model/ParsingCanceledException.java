package io.jenkins.plugins.coverage.model;

/**
 * Indicates that parsing has been canceled due to a user initiated interrupt.
 *
 * @author Ullrich Hafner
 */
public class ParsingCanceledException extends RuntimeException {
    private static final long serialVersionUID = 3341274949787014225L;

    /**
     * Creates a new instance of {@link ParsingCanceledException}.
     */
    public ParsingCanceledException() {
        super("Canceling parsing since build has been aborted.");
    }

    /**
     * Creates a new instance of {@link ParsingCanceledException}.
     *
     * @param cause
     *         the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public ParsingCanceledException(final Throwable cause) {
        super("Canceling parsing since build has been aborted.", cause);
    }
}

