package com.sosuisha.pptx2pdf.domain.service;

import java.nio.file.Path;

import com.sosuisha.pptx2pdf.domain.model.SlideSize;

/**
 * A PDF cropper that crops nothing. For tests that need a {@link PdfCropper}
 * but do not care about cropping.
 */
public class NullPdfCropper implements PdfCropper {
    @Override
    public void crop(Path source, Path destination, SlideSize slideSize) {}
}
