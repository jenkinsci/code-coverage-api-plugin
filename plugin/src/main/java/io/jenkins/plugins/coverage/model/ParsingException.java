package io.jenkins.plugins.coverage.model;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.errorprone.annotations.FormatMethod;

/**
 * Indicates that during parsing a non recoverable error has been occurred.
 *
 * @author Ullrich Hafner
 */
public class ParsingException extends RuntimeException {
    private static final long serialVersionUID = -9016364685084958944L;

    /**
     * Constructs a new {@link ParsingException} with the specified cause.
     *
     * @param cause
     *         the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public ParsingException(final Throwable cause) {
        super(createMessage(cause, "Exception occurred during parsing"), cause);
    }

    /**
     * Constructs a new {@link ParsingException} with the specified cause and message.
     *
     * @param cause
     *         the cause (which is saved for later retrieval by the {@link #getCause()} method).
     * @param messageFormat
     *         the message as a format string as described in <a href="../util/Formatter.html#syntax">Format string
     *         syntax</a>
     * @param args
     *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
     *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be zero.
     *         The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification</cite>. The behaviour on a {@code null} argument
     *         depends on the <a href="../util/Formatter.html#syntax">conversion</a>.
     */
    @FormatMethod
    public ParsingException(final Throwable cause, final String messageFormat, final Object... args) {
        super(createMessage(cause, String.format(messageFormat, args)), cause);
    }

    /**
     * Constructs a new {@link ParsingException} with the specified message.
     *
     * @param messageFormat
     *         the message as a format string as described in <a href="../util/Formatter.html#syntax">Format string
     *         syntax</a>
     * @param args
     *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
     *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be zero.
     *         The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification</cite>. The behaviour on a {@code null} argument
     *         depends on the <a href="../util/Formatter.html#syntax">conversion</a>.
     */
    @FormatMethod
    public ParsingException(final String messageFormat, final Object... args) {
        super(String.format(messageFormat, args));
    }

    private static String createMessage(final Throwable cause, final String message) {
        return String.format("%s%n%s%n%s", message,
                ExceptionUtils.getMessage(cause), ExceptionUtils.getStackTrace(cause));
    }
}
