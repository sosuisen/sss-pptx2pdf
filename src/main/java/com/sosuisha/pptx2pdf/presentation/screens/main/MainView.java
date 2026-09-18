package com.sosuisha.pptx2pdf.presentation.screens.main;

import java.io.File;
import java.util.Objects;

import com.sosuisha.pptx2pdf.presentation.View;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionItem;

import atlantafx.base.theme.Styles;
import io.github.sosuisen.jfxbuilder.controls.ButtonBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.ListViewBuilder;
import io.github.sosuisen.jfxbuilder.graphics.HBoxBuilder;
import io.github.sosuisen.jfxbuilder.graphics.SceneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.VBoxBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.jspecify.annotations.Nullable;

/**
 * View of the main screen: the list of decks with a drop area and the
 * buttons that add, convert, and clear decks.
 */
public class MainView implements View {
  private static final String TITLE = "sss-pptx2pdf";
  private static final double WIDTH = 640;
  private static final double HEIGHT = 420;
  private static final double SPACING = 10;

  private final MainViewModel viewModel;
  private final Scene scene;

  /**
   * Creates the view.
   *
   * @param viewModel view model of the main screen
   * @throws NullPointerException if viewModel is null
   */
  public MainView(MainViewModel viewModel) {
    this.viewModel = Objects.requireNonNull(viewModel, "viewModel must not be null");
    this.scene = buildSceneGraph();
  }

  @Override
  public Scene getScene() {
    return scene;
  }

  @Override
  public String getTitle() {
    return TITLE;
  }

  private Scene buildSceneGraph() {
    return SceneBuilder
      .create(
        VBoxBuilder
          .withChildren(
            buildToolbar(),
            ListViewBuilder.<ConversionItem>create()
              .id("decks")
              .items(viewModel.getItems())
              .cellFactory(_ -> new ConversionItemCell())
              .placeholder(
                LabelBuilder.create()
                  .id("placeholder")
                  .text("ここに.pptxファイルをドロップしてください")
                  .build()
              )
              .vGrowInVBox(Priority.ALWAYS)
              .build(),
            LabelBuilder.create()
              .id("status")
              .textPropertyApply(
                prop -> prop.bind(
                  viewModel.convertingProperty()
                    .map(converting -> converting ? "変換中..." : "")
                )
              )
              .build()
          )
          .spacing(SPACING)
          .padding(new Insets(SPACING))
          .onDragOver(this::acceptFiles)
          .onDragDropped(this::addDroppedFiles)
          .build(),
        WIDTH,
        HEIGHT
      )
      .build();
  }

  private HBox buildToolbar() {
    return HBoxBuilder
      .withChildren(
        ButtonBuilder.create()
          .id("chooseDecks")
          .text("ファイルを追加...")
          .onAction(_ -> viewModel.chooseDecks())
          .build(),
        ButtonBuilder.create()
          .id("convert")
          .text("PDFに変換")
          .addStyleClass(Styles.ACCENT)
          .disablePropertyApply(prop -> prop.bind(viewModel.canConvertProperty().not()))
          .onAction(_ -> viewModel.convert())
          .build(),
        ButtonBuilder.create()
          .id("removeFinished")
          .text("完了した項目を一覧から外す")
          .onAction(_ -> viewModel.removeFinished())
          .build()
      )
      .spacing(SPACING)
      .alignment(Pos.CENTER_LEFT)
      .build();
  }

  private void acceptFiles(DragEvent event) {
    if (event.getDragboard().hasFiles()) {
      event.acceptTransferModes(TransferMode.COPY);
    }
    event.consume();
  }

  private void addDroppedFiles(DragEvent event) {
    var dragboard = event.getDragboard();
    if (dragboard.hasFiles()) {
      viewModel.addDecks(dragboard.getFiles().stream().map(File::toPath).toList());
    }
    event.setDropCompleted(dragboard.hasFiles());
    event.consume();
  }

  /**
   * Cell that shows the display text of an item and follows its changes.
   */
  private static class ConversionItemCell extends ListCell<ConversionItem> {
    @Override
    protected void updateItem(@Nullable ConversionItem item, boolean empty) {
      super.updateItem(item, empty);
      textProperty().unbind();
      if (empty || item == null) {
        setText(null);
      } else {
        textProperty().bind(item.displayTextProperty());
      }
    }
  }
}
