package com.sosuisha.pptx2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;

class PptxSlideSizeReaderTest {
    @TempDir
    Path folder;

    @Test
    @DisplayName("presentation.xmlのsldSzからスライドサイズをポイント単位で読む")
    void reads_the_slide_size_in_points_from_sldsz_of_presentation_xml() throws Exception {
        var deck = writeDeck(
            "deck.pptx",
            "<p:presentation xmlns:p=\"x\"><p:sldSz cx=\"12192000\" cy=\"6858000\"/></p:presentation>"
        );

        assertEquals(new SlideSize(960, 540), new PptxSlideSizeReader().read(deck));
    }

    @Test
    @DisplayName("sldSzに他の属性が混ざっていても読める")
    void reads_the_slide_size_when_sldsz_has_other_attributes() throws Exception {
        var deck = writeDeck(
            "deck.pptx",
            "<p:presentation><p:sldSz cx=\"9144000\" type=\"screen4x3\" cy=\"6858000\"/></p:presentation>"
        );

        assertEquals(new SlideSize(720, 540), new PptxSlideSizeReader().read(deck));
    }

    @Test
    @DisplayName("presentation.xmlが無いzipはConversionExceptionになる")
    void a_zip_without_presentation_xml_throws_conversion_exception() throws Exception {
        var file = folder.resolve("other.pptx");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("README.txt"));
            zip.write("x".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(ConversionException.class, () -> new PptxSlideSizeReader().read(file));
    }

    @Test
    @DisplayName("sldSzが無いpresentation.xmlはConversionExceptionになる")
    void presentation_xml_without_sldsz_throws_conversion_exception() throws Exception {
        var deck = writeDeck("deck.pptx", "<p:presentation/>");

        assertThrows(ConversionException.class, () -> new PptxSlideSizeReader().read(deck));
    }

    @Test
    @DisplayName("存在しないファイルはConversionExceptionになる")
    void a_missing_file_throws_conversion_exception() {
        var missing = folder.resolve("missing.pptx");

        assertThrows(ConversionException.class, () -> new PptxSlideSizeReader().read(missing));
    }

    private Path writeDeck(String name, String presentationXml) throws IOException {
        var file = folder.resolve(name);
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("ppt/presentation.xml"));
            zip.write(presentationXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return file;
    }
}
