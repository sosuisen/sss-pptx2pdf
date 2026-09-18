package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;

/**
 * Reads the slide size of a PowerPoint deck.
 */
public interface SlideSizeReader {
    /**
     * Returns the slide size of the deck.
     *
     * @param deck path of the pptx file
     * @return slide size of the deck
     * @throws NullPointerException if deck is null
     * @throws ConversionException  if the deck cannot be read or has no slide
     *                              size
     */
    SlideSize read(Path deck) throws ConversionException;
}
