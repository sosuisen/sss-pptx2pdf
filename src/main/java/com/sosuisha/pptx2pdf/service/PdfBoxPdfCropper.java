package com.sosuisha.pptx2pdf.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;
import com.sosuisha.pptx2pdf.domain.service.PdfCropper;

/**
 * Crops PDF pages with Apache PDFBox by setting the media box and the crop box
 * of each page to the slide rectangle centred on the page. The page content is
 * not touched, so text stays text.
 */
public class PdfBoxPdfCropper implements PdfCropper {
    /** Tolerance for comparing paper and slide sizes, in points. */
    private static final double TOLERANCE = 0.5;

    @Override
    public void crop(Path source, Path destination, SlideSize slideSize)
        throws ConversionException {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(slideSize, "slideSize must not be null");
        try (var document = Loader.loadPDF(source.toFile())) {
            var pageNumber = 0;
            for (var page : document.getPages()) {
                pageNumber++;
                cropPage(page, pageNumber, slideSize);
            }
            document.save(destination.toFile());
        } catch (IOException e) {
            throw new ConversionException("Cannot crop the printed PDF: " + source, e);
        }
    }

    private static void cropPage(PDPage page, int pageNumber, SlideSize slideSize) {
        var paper = page.getMediaBox();
        var width = (float) slideSize.widthPt();
        var height = (float) slideSize.heightPt();
        if (paper.getWidth() + TOLERANCE < width || paper.getHeight() + TOLERANCE < height) {
            throw new ConversionException(
                "Page " + pageNumber + " (" + paper.getWidth() + " x " + paper.getHeight()
                    + " pt) is smaller than the slide (" + width + " x " + height
                    + " pt). Print at 100 % on a larger paper.",
                null
            );
        }
        var x = paper.getLowerLeftX() + (paper.getWidth() - width) / 2;
        var y = paper.getLowerLeftY() + (paper.getHeight() - height) / 2;
        var slide = new PDRectangle(x, y, width, height);
        page.setMediaBox(slide);
        page.setCropBox(slide);
    }
}
