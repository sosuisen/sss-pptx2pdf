package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;

/**
 * Crops every page of a printed PDF to the slide rectangle centred on the
 * page.
 */
public interface PdfCropper {
    /**
     * Crops the pages of the source PDF to the given slide size and writes the
     * result to the destination. An existing destination is overwritten.
     *
     * @param source      path of the printed PDF, whose pages are larger than
     *                    the slide
     * @param destination path of the PDF file to write
     * @param slideSize   size of the rectangle to keep, centred on each page
     * @throws NullPointerException if source, destination, or slideSize is null
     * @throws ConversionException  if the source cannot be read, a page is
     *                              smaller than the slide, or the destination
     *                              cannot be written
     */
    void crop(Path source, Path destination, SlideSize slideSize) throws ConversionException;
}
