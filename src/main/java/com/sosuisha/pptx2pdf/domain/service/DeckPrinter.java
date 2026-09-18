package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;

/**
 * Prints a PowerPoint deck to a PDF file at actual size on a paper that is
 * larger than the slides, so that every page can be cropped to the slide size
 * afterwards.
 */
public interface DeckPrinter {
    /**
     * Prints the deck to the given PDF file. An existing file is overwritten.
     *
     * @param deck path of the pptx file
     * @param pdf  path of the PDF file to write
     * @throws NullPointerException if deck or pdf is null
     * @throws ConversionException  if the deck cannot be printed
     */
    void print(Path deck, Path pdf) throws ConversionException;
}
