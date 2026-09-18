package com.sosuisha.pptx2pdf.presentation.appmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;
import com.sosuisha.pptx2pdf.domain.service.DeckConverter;
import com.sosuisha.pptx2pdf.domain.service.NullDeckConverter;

import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class ConversionAppModelTest {
    @TempDir
    Path folder;

    @Start
    void setup(Stage stage) {
        // Initializes the JavaFX toolkit, which the background conversion
        // needs to update the statuses on the FX thread.
    }

    @Test
    @DisplayName("pptxファイルを追加すると、待機状態の項目になり、PDFの出力先は同じ場所の同名.pdfになる")
    void adding_a_deck_creates_a_waiting_item_whose_pdf_is_next_to_the_deck() {
        var appModel = new ConversionAppModel(new NullDeckConverter());

        appModel.addDecks(List.of(folder.resolve("deck.pptx")));

        assertEquals(1, appModel.getItems().size());
        var item = appModel.getItems().getFirst();
        assertEquals(ConversionStatus.WAITING, item.statusProperty().get());
        assertEquals(folder.resolve("deck.pdf").toAbsolutePath(), item.getPdf());
    }

    @Test
    @DisplayName("拡張子はpptx以外を無視し、大文字小文字は区別しない")
    void ignores_files_other_than_pptx_and_ignores_the_case_of_the_extension() {
        var appModel = new ConversionAppModel(new NullDeckConverter());

        appModel.addDecks(
            List.of(
                folder.resolve("a.pptx"), folder.resolve("b.PPTX"), folder.resolve("c.pdf"),
                folder.resolve("d.ppt"), folder.resolve("e.txt")
            )
        );

        assertEquals(
            List.of("a.pptx", "b.PPTX"),
            appModel.getItems().stream().map(item -> item.getDeck().getFileName().toString())
                .toList()
        );
    }

    @Test
    @DisplayName("同じファイルを二度追加しても、項目は一つのまま")
    void adding_the_same_deck_twice_keeps_one_item() {
        var appModel = new ConversionAppModel(new NullDeckConverter());

        appModel.addDecks(List.of(folder.resolve("deck.pptx")));
        appModel.addDecks(List.of(folder.resolve("deck.pptx")));

        assertEquals(1, appModel.getItems().size());
    }

    @Test
    @DisplayName("変換はバックグラウンドで行われ、項目の状態が印刷中、切り抜き中、完了と進む")
    void conversion_runs_in_the_background_and_the_status_goes_printing_cropping_done(
        FxRobot robot) throws Exception {
        var statuses = new java.util.concurrent.CopyOnWriteArrayList<ConversionStatus>();
        DeckConverter converter = (_, _, progress) -> {
            progress.accept(ConversionPhase.PRINTING);
            progress.accept(ConversionPhase.CROPPING);
        };
        var appModel = new ConversionAppModel(converter);
        appModel.addDecks(List.of(folder.resolve("deck.pptx")));
        var item = appModel.getItems().getFirst();
        item.statusProperty().subscribe(status -> statuses.add(status));

        var convertingRightAfterCall = new AtomicReference<@Nullable Boolean>();
        robot.interact(() -> {
            appModel.convertAll();
            convertingRightAfterCall.set(appModel.convertingProperty().get());
        });

        assertEquals(Boolean.TRUE, convertingRightAfterCall.get());
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !appModel.convertingProperty().get());
        assertEquals(
            List.of(
                ConversionStatus.WAITING, ConversionStatus.PRINTING, ConversionStatus.CROPPING,
                ConversionStatus.DONE
            ),
            statuses
        );
    }

    @Test
    @DisplayName("変換に失敗した項目は失敗状態になり、理由がメッセージに入り、次の項目は変換される")
    void a_failed_item_becomes_failed_with_the_reason_and_the_next_item_is_still_converted(
        FxRobot robot) throws Exception {
        DeckConverter converter = (deck, _, _) -> {
            if (deck.getFileName().toString().startsWith("bad")) {
                throw new ConversionException("printer is missing", null);
            }
        };
        var appModel = new ConversionAppModel(converter);
        appModel.addDecks(List.of(folder.resolve("bad.pptx"), folder.resolve("good.pptx")));

        robot.interact(appModel::convertAll);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !appModel.convertingProperty().get());
        var bad = appModel.getItems().get(0);
        var good = appModel.getItems().get(1);
        assertEquals(ConversionStatus.FAILED, bad.statusProperty().get());
        assertEquals("printer is missing", bad.messageProperty().get());
        assertEquals(ConversionStatus.DONE, good.statusProperty().get());
    }

    @Test
    @DisplayName("完了と失敗の項目だけが消され、待機中の項目は残る")
    void removing_finished_items_removes_done_and_failed_and_keeps_waiting_items(FxRobot robot)
        throws Exception {
        DeckConverter converter = (deck, _, _) -> {
            if (deck.getFileName().toString().startsWith("bad")) {
                throw new ConversionException("failed", null);
            }
        };
        var appModel = new ConversionAppModel(converter);
        appModel.addDecks(List.of(folder.resolve("bad.pptx"), folder.resolve("good.pptx")));
        robot.interact(appModel::convertAll);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !appModel.convertingProperty().get());
        appModel.addDecks(List.of(folder.resolve("later.pptx")));

        appModel.removeFinished();

        assertEquals(1, appModel.getItems().size());
        assertEquals(
            "later.pptx", appModel.getItems().getFirst().getDeck().getFileName().toString()
        );
    }

    @Test
    @DisplayName("待機中の項目が無ければ、変換は始まらない")
    void conversion_does_not_start_when_no_item_is_waiting() {
        var appModel = new ConversionAppModel(new NullDeckConverter());

        appModel.convertAll();

        assertFalse(appModel.convertingProperty().get());
    }

    @Test
    @DisplayName("ConversionException以外の例外は変換を止め、FXスレッドの未捕捉例外ハンドラに届く")
    void an_unexpected_exception_stops_the_conversion_and_reaches_the_uncaught_exception_handler(
        FxRobot robot) throws Exception {
        var thrown = new AtomicReference<@Nullable Throwable>();
        Consumer<Thread.UncaughtExceptionHandler> install =
            handler -> Thread.currentThread().setUncaughtExceptionHandler(handler);
        robot.interact(() -> install.accept((_, e) -> thrown.set(e)));
        DeckConverter converter = (_, _, _) -> {
            throw new IllegalStateException("bug");
        };
        var appModel = new ConversionAppModel(converter);
        appModel.addDecks(List.of(folder.resolve("deck.pptx")));

        robot.interact(appModel::convertAll);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> thrown.get() != null);
        assertTrue(thrown.get() instanceof IllegalStateException);
        assertFalse(appModel.convertingProperty().get());
    }
}
