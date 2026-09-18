package com.sosuisha.pptx2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;

class PdfBoxPdfCropperTest {
    private static final float A3_WIDTH = 1190.55f;
    private static final float A3_HEIGHT = 841.89f;
    private static final float DELTA = 0.01f;

    @TempDir
    Path folder;

    @Test
    @DisplayName("各ページがスライドサイズちょうどに、ページ中央を残して切り抜かれる")
    void every_page_is_cropped_to_the_slide_size_centred_on_the_page() throws Exception {
        var source = writePdf("a3.pdf", 2, A3_WIDTH, A3_HEIGHT);
        var destination = folder.resolve("cropped.pdf");

        new PdfBoxPdfCropper().crop(source, destination, SlideSize.WIDESCREEN);

        try (var document = Loader.loadPDF(destination.toFile())) {
            assertEquals(2, document.getNumberOfPages());
            for (var page : document.getPages()) {
                var box = page.getMediaBox();
                assertEquals(960, box.getWidth(), DELTA);
                assertEquals(540, box.getHeight(), DELTA);
                assertEquals((A3_WIDTH - 960) / 2, box.getLowerLeftX(), DELTA);
                assertEquals((A3_HEIGHT - 540) / 2, box.getLowerLeftY(), DELTA);
                // PDRectangle has no equals; compare the coordinates.
                assertEquals(box.getLowerLeftX(), page.getCropBox().getLowerLeftX(), DELTA);
                assertEquals(box.getLowerLeftY(), page.getCropBox().getLowerLeftY(), DELTA);
                assertEquals(box.getWidth(), page.getCropBox().getWidth(), DELTA);
                assertEquals(box.getHeight(), page.getCropBox().getHeight(), DELTA);
            }
        }
    }

    @Test
    @DisplayName("スライドと同じ大きさの用紙はそのまま通る")
    void a_page_of_exactly_the_slide_size_is_kept() throws Exception {
        var source = writePdf("exact.pdf", 1, 960, 540);
        var destination = folder.resolve("cropped.pdf");

        new PdfBoxPdfCropper().crop(source, destination, SlideSize.WIDESCREEN);

        try (var document = Loader.loadPDF(destination.toFile())) {
            assertEquals(960, document.getPage(0).getMediaBox().getWidth(), DELTA);
        }
    }

    @Test
    @DisplayName("スライドより小さい用紙はConversionExceptionになり、出力は作られない")
    void a_page_smaller_than_the_slide_throws_conversion_exception_and_writes_nothing()
        throws Exception {
        var source = writePdf("a4.pdf", 1, 841.89f, 595.28f);
        var destination = folder.resolve("cropped.pdf");

        assertThrows(
            ConversionException.class,
            () -> new PdfBoxPdfCropper().crop(source, destination, SlideSize.WIDESCREEN)
        );
        assertFalse(destination.toFile().exists());
    }

    @Test
    @DisplayName("PDFでないファイルはConversionExceptionになる")
    void a_file_that_is_not_a_pdf_throws_conversion_exception() throws Exception {
        var source = folder.resolve("not.pdf");
        java.nio.file.Files.writeString(source, "not a pdf");

        assertThrows(
            ConversionException.class,
            () -> new PdfBoxPdfCropper()
                .crop(source, folder.resolve("out.pdf"), SlideSize.WIDESCREEN)
        );
    }

    private Path writePdf(String name, int pages, float width, float height) throws IOException {
        var file = folder.resolve(name);
        try (var document = new PDDocument()) {
            for (var i = 0; i < pages; i++) {
                document.addPage(new PDPage(new PDRectangle(width, height)));
            }
            document.save(file.toFile());
        }
        return file;
    }
}
