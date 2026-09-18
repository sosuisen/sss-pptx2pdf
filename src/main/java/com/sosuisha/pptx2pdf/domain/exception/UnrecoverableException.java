package com.sosuisha.pptx2pdf.domain.exception;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Base class of the errors that the caller cannot recover from. Such an error
 * is thrown as an unchecked exception, is not caught on the way, and is shown
 * to the user by the uncaught exception handler of the application. The
 * message is written for the user and is required.
 */
public abstract class UnrecoverableException extends RuntimeException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause   underlying cause of the failure, or null when there is none
     * @throws NullPointerException     if message is null
     * @throws IllegalArgumentException if message is blank
     */
    protected UnrecoverableException(String message, @Nullable Throwable cause) {
        super(requireMessage(message), cause);
    }

    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message must not be null");
        if (message.isBlank()) { throw new IllegalArgumentException("message must not be blank"); }
        return message;
    }

    /**
     * {@inheritDoc} The message is never null, because the constructor
     * requires it.
     */
    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
