package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;

/**
 * Converts a PowerPoint deck to a PDF whose pages have exactly the slide
 * size.
 */
public interface DeckConverter {
    /**
     * Converts the deck to the given PDF file. An existing file is
     * overwritten. The phases of the conversion are reported to the progress
     * consumer on the calling thread as they start.
     *
     * @param deck     path of the pptx file
     * @param pdf      path of the PDF file to write
     * @param progress consumer that receives each phase when it starts
     * @throws NullPointerException if deck, pdf, or progress is null
     * @throws ConversionException  if the deck cannot be converted
     */
    void convert(Path deck, Path pdf, Consumer<ConversionPhase> progress)
        throws ConversionException;
}
