package com.sosuisha.pptx2pdf.presentation.screens.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.pptx2pdf.domain.service.NullDeckConverter;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionAppModel;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionItem;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionStatus;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class MainViewTest {
    private MainViewModel viewModel;

    @Start
    void setup(Stage stage) {
        viewModel = new MainViewModel(
            new ConversionAppModel(new NullDeckConverter()),
            () -> List.of(Path.of("chosen.pptx"))
        );
        var view = new MainView(viewModel);
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }

    @Test
    @DisplayName("一覧が空のとき、ドロップを促す文言が表示され、変換ボタンは押せない")
    void with_an_empty_list_the_drop_hint_is_shown_and_the_convert_button_is_disabled(
        FxRobot robot) {
        verifyThat("#placeholder", LabeledMatchers.hasText("ここに.pptxファイルをドロップしてください"));
        assertTrue(robot.lookup("#convert").queryAs(Button.class).isDisabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("ファイルを追加ボタンを押すと、選択したファイルが一覧に状態付きで表示される")
    void clicking_the_add_button_shows_the_chosen_file_in_the_list_with_its_status(FxRobot robot) {
        robot.clickOn("#chooseDecks");

        var list = robot.lookup("#decks").queryAs(ListView.class);
        assertEquals(1, list.getItems().size());
        var item = (ConversionItem) list.getItems().getFirst();
        assertEquals("[待機] chosen.pptx", item.displayTextProperty().getValue());
        assertFalse(robot.lookup("#convert").queryAs(Button.class).isDisabled());
    }

    @Test
    @DisplayName("変換ボタンを押すと変換が始まり、終わると項目は完了になる")
    void clicking_the_convert_button_starts_the_conversion_and_the_item_ends_up_done(
        FxRobot robot) throws Exception {
        robot.clickOn("#chooseDecks");

        robot.clickOn("#convert");

        WaitForAsyncUtils.waitFor(
            5, TimeUnit.SECONDS,
            () -> viewModel.getItems().getFirst().statusProperty().get() == ConversionStatus.DONE
        );
        assertTrue(robot.lookup("#convert").queryAs(Button.class).isDisabled());
    }

    @Test
    @DisplayName("完了した項目を一覧から外すボタンを押すと、完了した項目が一覧から外れる（ファイルは残る）")
    void clicking_the_remove_finished_button_removes_the_done_items_from_the_list(FxRobot robot)
        throws Exception {
        robot.clickOn("#chooseDecks");
        robot.clickOn("#convert");
        WaitForAsyncUtils.waitFor(
            5, TimeUnit.SECONDS,
            () -> viewModel.getItems().getFirst().statusProperty().get() == ConversionStatus.DONE
        );

        robot.clickOn("#removeFinished");

        assertTrue(viewModel.getItems().isEmpty());
    }
}
