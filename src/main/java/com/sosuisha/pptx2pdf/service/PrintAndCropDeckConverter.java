package com.sosuisha.pptx2pdf.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;
import com.sosuisha.pptx2pdf.domain.service.DeckConverter;
import com.sosuisha.pptx2pdf.domain.service.DeckPrinter;
import com.sosuisha.pptx2pdf.domain.service.PdfCropper;
import com.sosuisha.pptx2pdf.domain.service.SlideSizeReader;

/**
 * Converts a deck in two steps: prints it to a temporary oversized PDF, then
 * crops that PDF to the slide size read from the deck.
 */
public class PrintAndCropDeckConverter implements DeckConverter {
    private final SlideSizeReader slideSizeReader;
    private final DeckPrinter printer;
    private final PdfCropper cropper;

    /**
     * Creates the converter.
     *
     * @param slideSizeReader reader of the slide size of a deck
     * @param printer         printer that writes the oversized PDF
     * @param cropper         cropper that cuts the pages to the slide size
     * @throws NullPointerException if slideSizeReader, printer, or cropper is
     *                              null
     */
    public PrintAndCropDeckConverter(SlideSizeReader slideSizeReader, DeckPrinter printer,
        PdfCropper cropper) {
        this.slideSizeReader =
            Objects.requireNonNull(slideSizeReader, "slideSizeReader must not be null");
        this.printer = Objects.requireNonNull(printer, "printer must not be null");
        this.cropper = Objects.requireNonNull(cropper, "cropper must not be null");
    }

    @Override
    public void convert(Path deck, Path pdf, Consumer<ConversionPhase> progress)
        throws ConversionException {
        Objects.requireNonNull(deck, "deck must not be null");
        Objects.requireNonNull(pdf, "pdf must not be null");
        Objects.requireNonNull(progress, "progress must not be null");
        var slideSize = slideSizeReader.read(deck);
        var printed = createTempFile(deck);
        try {
            progress.accept(ConversionPhase.PRINTING);
            printer.print(deck, printed);
            progress.accept(ConversionPhase.CROPPING);
            cropper.crop(printed, pdf, slideSize);
        } finally {
            deleteQuietly(printed);
        }
    }

    private static Path createTempFile(Path deck) {
        try {
            var file = Files.createTempFile("sss-pptx2pdf-", ".pdf");
            // The printer creates the file itself; an existing empty file
            // would make the completion check of the print script ambiguous.
            Files.delete(file);
            return file;
        } catch (IOException e) {
            throw new ConversionException(
                "Cannot create a temporary file for printing " + deck, e
            );
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException _) {
            // The temporary file is left behind; nothing else to do.
        }
    }
}
