package com.sosuisha.pptx2pdf.presentation.screens.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.pptx2pdf.domain.service.NullDeckConverter;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionAppModel;

class MainViewModelTest {
    @Test
    @DisplayName("ファイルを選択すると、選択したpptxが一覧に追加される")
    void choosing_decks_adds_the_chosen_files_to_the_list() {
        var viewModel = new MainViewModel(
            new ConversionAppModel(new NullDeckConverter()),
            () -> List.of(Path.of("a.pptx"), Path.of("b.pptx"))
        );

        viewModel.chooseDecks();

        assertEquals(2, viewModel.getItems().size());
    }

    @Test
    @DisplayName("ファイルの選択をキャンセルすると、一覧は変わらない")
    void cancelling_the_file_selection_keeps_the_list_unchanged() {
        var viewModel =
            new MainViewModel(new ConversionAppModel(new NullDeckConverter()), List::of);

        viewModel.chooseDecks();

        assertTrue(viewModel.getItems().isEmpty());
    }

    @Test
    @DisplayName("一覧が空のときは変換できず、待機中の項目が入ると変換できる")
    void cannot_convert_with_an_empty_list_and_can_convert_once_a_waiting_item_is_added() {
        var viewModel =
            new MainViewModel(new ConversionAppModel(new NullDeckConverter()), List::of);

        assertFalse(viewModel.canConvertProperty().get());

        viewModel.addDecks(List.of(Path.of("a.pptx")));

        assertTrue(viewModel.canConvertProperty().get());
    }

    @Test
    @DisplayName("ドロップされたファイルのうち、pptxだけが一覧に入る")
    void only_pptx_files_among_the_dropped_files_enter_the_list() {
        var viewModel =
            new MainViewModel(new ConversionAppModel(new NullDeckConverter()), List::of);

        viewModel.addDecks(List.of(Path.of("a.pptx"), Path.of("notes.txt")));

        assertEquals(1, viewModel.getItems().size());
    }
}
