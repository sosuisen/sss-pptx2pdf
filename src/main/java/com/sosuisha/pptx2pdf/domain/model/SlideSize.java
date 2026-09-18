package com.sosuisha.pptx2pdf.domain.model;

/**
 * Size of a slide in PDF points (1/72 inch).
 *
 * @param widthPt  width in points
 * @param heightPt height in points
 */
public record SlideSize(double widthPt, double heightPt) {
    /** Default slide size of PowerPoint: 13.333 x 7.5 inches (16:9). */
    public static final SlideSize WIDESCREEN = new SlideSize(960, 540);

    /**
     * Creates the size.
     *
     * @param widthPt  width in points
     * @param heightPt height in points
     * @throws IllegalArgumentException if widthPt or heightPt is not positive
     */
    public SlideSize {
        if (widthPt <= 0) {
            throw new IllegalArgumentException("widthPt must be positive: " + widthPt);
        }
        if (heightPt <= 0) {
            throw new IllegalArgumentException("heightPt must be positive: " + heightPt);
        }
    }
}
