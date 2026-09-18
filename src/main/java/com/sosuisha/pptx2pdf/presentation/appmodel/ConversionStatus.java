package com.sosuisha.pptx2pdf.presentation.appmodel;

/**
 * Status of a deck in the conversion list.
 */
public enum ConversionStatus {
    /** The deck waits for the conversion to start. */
    WAITING("待機"),
    /** The deck is being printed. */
    PRINTING("印刷中"),
    /** The printed PDF is being cropped. */
    CROPPING("切り抜き中"),
    /** The PDF has been written. */
    DONE("完了"),
    /** The conversion failed; the item message says why. */
    FAILED("失敗");

    private final String label;

    ConversionStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the label shown to the user.
     *
     * @return label of this status
     */
    public String label() {
        return label;
    }

    /**
     * Returns whether the conversion of the deck has ended, with or without
     * success.
     *
     * @return true for {@link #DONE} and {@link #FAILED}
     */
    public boolean isFinished() {
        return this == DONE || this == FAILED;
    }
}
