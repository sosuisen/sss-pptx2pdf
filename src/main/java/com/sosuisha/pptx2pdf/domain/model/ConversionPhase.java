package com.sosuisha.pptx2pdf.domain.model;

/**
 * Phase of the conversion of one deck, reported as progress.
 */
public enum ConversionPhase {
    /** The deck is being printed to an oversized PDF through the PDF printer. */
    PRINTING,
    /** The printed PDF is being cropped to the slide size. */
    CROPPING
}
