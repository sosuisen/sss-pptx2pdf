package com.sosuisha.pptx2pdf.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;
import com.sosuisha.pptx2pdf.domain.service.SlideSizeReader;

/**
 * Reads the slide size from the presentation part of a pptx file. A pptx file
 * is a zip archive; {@code ppt/presentation.xml} holds the size in EMUs
 * (English Metric Units, 12700 per point) in its {@code sldSz} element.
 */
public class PptxSlideSizeReader implements SlideSizeReader {
    private static final String PRESENTATION_PART = "ppt/presentation.xml";
    private static final double EMU_PER_POINT = 12700;
    private static final Pattern SLIDE_SIZE =
        Pattern.compile("<p:sldSz[^>]*\\bcx=\"(\\d+)\"[^>]*\\bcy=\"(\\d+)\"");

    @Override
    public SlideSize read(Path deck) throws ConversionException {
        Objects.requireNonNull(deck, "deck must not be null");
        String presentation;
        try (var zip = new ZipFile(deck.toFile())) {
            var entry = zip.getEntry(PRESENTATION_PART);
            if (entry == null) {
                throw new ConversionException(
                    "Not a PowerPoint file (no " + PRESENTATION_PART + "): " + deck, null
                );
            }
            try (var in = zip.getInputStream(entry)) {
                presentation = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ConversionException("Cannot read the PowerPoint file: " + deck, e);
        }
        var matcher = SLIDE_SIZE.matcher(presentation);
        if (!matcher.find()) {
            throw new ConversionException("The PowerPoint file has no slide size: " + deck, null);
        }
        return new SlideSize(
            Long.parseLong(matcher.group(1)) / EMU_PER_POINT,
            Long.parseLong(matcher.group(2)) / EMU_PER_POINT
        );
    }
}
