package com.sosuisha.pptx2pdf.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a deck cannot be converted to a PDF: the deck cannot be read,
 * the print through the PDF printer fails, or the printed PDF cannot be
 * cropped. The error is not recoverable by the caller, so this is an
 * unchecked exception. The message is written for the user and is required.
 */
public class ConversionException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause   underlying cause of the failure, or null when there is none
     * @throws NullPointerException     if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public ConversionException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
