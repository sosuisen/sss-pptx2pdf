package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;

/**
 * A deck converter that does nothing and reports no phase. For tests that
 * need a {@link DeckConverter} but do not care about the conversion.
 */
public class NullDeckConverter implements DeckConverter {
    @Override
    public void convert(Path deck, Path pdf, Consumer<ConversionPhase> progress) {}
}
