package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;

/**
 * A deck printer that prints nothing. For tests that need a
 * {@link DeckPrinter} but do not care about printing.
 */
public class NullDeckPrinter implements DeckPrinter {
    @Override
    public void print(Path deck, Path pdf) {}
}
