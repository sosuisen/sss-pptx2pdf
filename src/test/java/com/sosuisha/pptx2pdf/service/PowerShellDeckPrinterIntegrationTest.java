package com.sosuisha.pptx2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end check of the print path. It needs PowerPoint and the
 * "Microsoft Print to PDF" printer, so it runs only when the environment
 * variable {@code SSS_PPTX2PDF_DECK} names a pptx file to print.
 */
@EnabledIfEnvironmentVariable(named = "SSS_PPTX2PDF_DECK", matches = ".+")
class PowerShellDeckPrinterIntegrationTest {
    private static final float A3_WIDTH = 1190.55f;
    private static final float A3_HEIGHT = 841.89f;
    private static final float DELTA = 0.5f;

    @TempDir
    Path folder;

    @Test
    @DisplayName("デッキがA3横の用紙にPDFとして印刷され、切り抜くとスライドサイズになる")
    void prints_the_deck_on_a3_landscape_and_cropping_gives_the_slide_size() throws Exception {
        var deck = Path.of(System.getenv("SSS_PPTX2PDF_DECK"));
        var printed = folder.resolve("printed.pdf");
        var cropped = folder.resolve("cropped.pdf");

        new PowerShellDeckPrinter().print(deck, printed);

        assertTrue(Files.exists(printed));
        try (var document = Loader.loadPDF(printed.toFile())) {
            var box = document.getPage(0).getMediaBox();
            assertEquals(A3_WIDTH, box.getWidth(), DELTA);
            assertEquals(A3_HEIGHT, box.getHeight(), DELTA);
        }

        var slideSize = new PptxSlideSizeReader().read(deck);
        new PdfBoxPdfCropper().crop(printed, cropped, slideSize);
        try (var document = Loader.loadPDF(cropped.toFile())) {
            var box = document.getPage(0).getMediaBox();
            assertEquals(slideSize.widthPt(), box.getWidth(), DELTA);
            assertEquals(slideSize.heightPt(), box.getHeight(), DELTA);
        }
    }
}
