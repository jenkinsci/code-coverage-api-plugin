package io.jenkins.plugins.coverage.exception;

public class QualityGatesInvalidException extends Exception {
    private static final long serialVersionUID = 7136603737385920442L;

    public QualityGatesInvalidException(final String message) {
        super(message);
    }

}
