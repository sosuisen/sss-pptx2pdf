package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;
import java.util.Objects;

import com.sosuisha.pptx2pdf.domain.model.SlideSize;

/**
 * A slide size reader that returns the same size for every deck without
 * reading it. For tests that need a {@link SlideSizeReader} but have no real
 * deck.
 */
public class FixedSlideSizeReader implements SlideSizeReader {
    private final SlideSize slideSize;

    /**
     * Creates the reader.
     *
     * @param slideSize size returned for every deck
     * @throws NullPointerException if slideSize is null
     */
    public FixedSlideSizeReader(SlideSize slideSize) {
        this.slideSize = Objects.requireNonNull(slideSize, "slideSize must not be null");
    }

    @Override
    public SlideSize read(Path deck) {
        return slideSize;
    }
}
