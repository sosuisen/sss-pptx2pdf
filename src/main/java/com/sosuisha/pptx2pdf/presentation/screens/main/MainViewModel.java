package com.sosuisha.pptx2pdf.presentation.screens.main;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionAppModel;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionItem;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionStatus;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;

/**
 * ViewModel of the main screen. The conversion state lives in the
 * {@link ConversionAppModel}; this view model delegates to it.
 */
public class MainViewModel {
    private final ConversionAppModel appModel;
    private final Supplier<List<Path>> deckChooser;
    private final BooleanBinding canConvert;

    /**
     * Creates the view model.
     *
     * @param appModel    application-wide state of the conversion
     * @param deckChooser function that lets the user choose pptx files. It
     *                    returns the chosen files, or an empty list when the
     *                    user cancels
     * @throws NullPointerException if appModel or deckChooser is null
     */
    public MainViewModel(ConversionAppModel appModel, Supplier<List<Path>> deckChooser) {
        this.appModel = Objects.requireNonNull(appModel, "appModel must not be null");
        this.deckChooser = Objects.requireNonNull(deckChooser, "deckChooser must not be null");
        this.canConvert = Bindings.createBooleanBinding(
            () -> !appModel.convertingProperty().get() && hasWaitingItem(),
            appModel.convertingProperty(), appModel.getItems()
        );
    }

    private boolean hasWaitingItem() {
        return appModel.getItems().stream()
            .anyMatch(item -> item.statusProperty().get() == ConversionStatus.WAITING);
    }

    /**
     * Returns the list of decks shown on the screen.
     *
     * @return observable list of the items
     */
    public ObservableList<ConversionItem> getItems() {
        return appModel.getItems();
    }

    /**
     * Returns whether a conversion is running.
     *
     * @return read-only property that is true while a conversion runs
     */
    public ReadOnlyBooleanProperty convertingProperty() {
        return appModel.convertingProperty();
    }

    /**
     * Returns whether the conversion can be started: no conversion is running
     * and at least one deck is waiting.
     *
     * @return observable boolean that is true when the conversion can start
     */
    public BooleanBinding canConvertProperty() {
        return canConvert;
    }

    /**
     * Adds decks to the list, for example the files dropped on the window.
     * Paths that are not pptx files or already in the list are skipped.
     *
     * @param decks paths of the files
     * @throws NullPointerException if decks is null
     */
    public void addDecks(List<Path> decks) {
        appModel.addDecks(decks);
    }

    /**
     * Lets the user choose pptx files and adds them to the list. Does nothing
     * when the user cancels.
     */
    public void chooseDecks() {
        appModel.addDecks(deckChooser.get());
    }

    /**
     * Starts converting the waiting decks in the background.
     */
    public void convert() {
        appModel.convertAll();
    }

    /**
     * Removes the items whose conversion has ended.
     */
    public void removeFinished() {
        appModel.removeFinished();
    }
}
