package io.jenkins.plugins.coverage.exception;

public class CoverageException extends Exception {

    public CoverageException() {
    }

    public CoverageException(String message) {
        super(message);
    }

    public CoverageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoverageException(Throwable cause) {
        super(cause);
    }

    public CoverageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
