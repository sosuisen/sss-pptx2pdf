package com.sosuisha.pptx2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;
import com.sosuisha.pptx2pdf.domain.model.SlideSize;
import com.sosuisha.pptx2pdf.domain.service.DeckPrinter;
import com.sosuisha.pptx2pdf.domain.service.FixedSlideSizeReader;
import com.sosuisha.pptx2pdf.domain.service.NullDeckPrinter;
import com.sosuisha.pptx2pdf.domain.service.NullPdfCropper;
import com.sosuisha.pptx2pdf.domain.service.PdfCropper;

class PrintAndCropDeckConverterTest {
    @TempDir
    Path folder;

    @Test
    @DisplayName("印刷した一時PDFを、デッキから読んだスライドサイズで出力先へ切り抜く")
    void crops_the_printed_temporary_pdf_to_the_slide_size_of_the_deck_into_the_output() {
        var slideSize = new SlideSize(720, 540);
        var printedTo = new AtomicReference<@Nullable Path>();
        var croppedFrom = new AtomicReference<@Nullable Path>();
        var croppedTo = new AtomicReference<@Nullable Path>();
        var croppedSize = new AtomicReference<@Nullable SlideSize>();
        DeckPrinter printer = (_, pdf) -> {
            printedTo.set(pdf);
            writeFile(pdf);
        };
        PdfCropper cropper = (source, destination, size) -> {
            croppedFrom.set(source);
            croppedTo.set(destination);
            croppedSize.set(size);
        };
        var converter =
            new PrintAndCropDeckConverter(new FixedSlideSizeReader(slideSize), printer, cropper);
        var output = folder.resolve("deck.pdf");

        converter.convert(folder.resolve("deck.pptx"), output, _ -> {
        });

        assertNotNull(printedTo.get());
        assertEquals(printedTo.get(), croppedFrom.get());
        assertEquals(output, croppedTo.get());
        assertEquals(slideSize, croppedSize.get());
    }

    @Test
    @DisplayName("印刷、切り抜きの順に進行が報告される")
    void reports_the_phases_printing_then_cropping() {
        var phases = new ArrayList<ConversionPhase>();
        var converter = new PrintAndCropDeckConverter(
            new FixedSlideSizeReader(SlideSize.WIDESCREEN), new NullDeckPrinter(),
            new NullPdfCropper()
        );

        converter.convert(folder.resolve("deck.pptx"), folder.resolve("deck.pdf"), phases::add);

        assertEquals(List.of(ConversionPhase.PRINTING, ConversionPhase.CROPPING), phases);
    }

    @Test
    @DisplayName("印刷した一時PDFは、切り抜きが成功しても失敗しても削除される")
    void deletes_the_temporary_pdf_whether_cropping_succeeds_or_fails() {
        var printedTo = new AtomicReference<@Nullable Path>();
        DeckPrinter printer = (_, pdf) -> {
            printedTo.set(pdf);
            writeFile(pdf);
        };
        PdfCropper failingCropper = (_, _, _) -> {
            throw new ConversionException("cannot crop", null);
        };
        var converter = new PrintAndCropDeckConverter(
            new FixedSlideSizeReader(SlideSize.WIDESCREEN), printer, failingCropper
        );

        assertThrows(
            ConversionException.class,
            () -> converter.convert(folder.resolve("deck.pptx"), folder.resolve("deck.pdf"), _ -> {
            })
        );

        var printed = printedTo.get();
        assertNotNull(printed);
        assertFalse(Files.exists(printed));
    }

    @Test
    @DisplayName("スライドサイズが読めないときは、印刷を始める前にConversionExceptionになる")
    void throws_conversion_exception_before_printing_when_the_slide_size_cannot_be_read() {
        var printed = new AtomicReference<Boolean>(false);
        DeckPrinter printer = (_, _) -> printed.set(true);
        var converter = new PrintAndCropDeckConverter(
            _ -> {
                throw new ConversionException("cannot read", null);
            }, printer,
            new NullPdfCropper()
        );

        assertThrows(
            ConversionException.class,
            () -> converter.convert(folder.resolve("deck.pptx"), folder.resolve("deck.pdf"), _ -> {
            })
        );
        assertTrue(!printed.get());
    }

    private static void writeFile(Path file) {
        try {
            Files.writeString(file, "pdf");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
