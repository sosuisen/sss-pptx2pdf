package com.sosuisha.pptx2pdf.presentation.appmodel;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.sosuisha.pptx2pdf.domain.exception.ConversionException;
import com.sosuisha.pptx2pdf.domain.model.ConversionPhase;
import com.sosuisha.pptx2pdf.domain.service.DeckConverter;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * Application-wide state of the conversion: the list of decks and whether a
 * conversion is running.
 */
public class ConversionAppModel {
    private static final String DECK_EXTENSION = ".pptx";
    private static final String PDF_EXTENSION = ".pdf";

    // The extractor makes the list fire an update when the status of an item
    // changes, so bindings on the list see status changes.
    private final ObservableList<ConversionItem> items = FXCollections
        .observableArrayList(item -> new Observable[] {item.statusProperty()});
    private final BooleanProperty converting = new SimpleBooleanProperty(false);
    private final DeckConverter converter;

    /**
     * Creates the app model.
     *
     * @param converter converter that turns a deck into a PDF
     * @throws NullPointerException if converter is null
     */
    public ConversionAppModel(DeckConverter converter) {
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
    }

    /**
     * Adds decks to the list. A path that is not a pptx file (by its
     * extension, ignoring case) or that is already in the list is skipped.
     * The PDF is written next to the deck with the same name and the pdf
     * extension.
     *
     * @param decks paths of pptx files
     * @throws NullPointerException if decks is null
     */
    public void addDecks(List<Path> decks) {
        Objects.requireNonNull(decks, "decks must not be null");
        for (var deck : decks) {
            var absolute = deck.toAbsolutePath().normalize();
            if (isDeck(absolute) && !contains(absolute)) {
                items.add(new ConversionItem(absolute, pdfPathOf(absolute)));
            }
        }
    }

    private static boolean isDeck(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(DECK_EXTENSION);
    }

    private boolean contains(Path deck) {
        return items.stream().anyMatch(item -> item.getDeck().equals(deck));
    }

    private static Path pdfPathOf(Path deck) {
        var name = deck.getFileName().toString();
        var base = name.substring(0, name.length() - DECK_EXTENSION.length());
        return deck.resolveSibling(base + PDF_EXTENSION);
    }

    /**
     * Removes the items whose conversion has ended, with or without success.
     */
    public void removeFinished() {
        items.removeIf(item -> item.statusProperty().get().isFinished());
    }

    /**
     * Converts every waiting deck, one after another, in the background.
     * Returns immediately; the statuses are updated on the JavaFX application
     * thread. A deck whose conversion fails gets the {@link
     * ConversionStatus#FAILED} status with the reason, and the next deck is
     * converted. Does nothing while a conversion is running. An unexpected
     * error (not a {@link ConversionException}) stops the conversion and is
     * rethrown on the JavaFX application thread, so it reaches the uncaught
     * exception handler of that thread instead of the caller.
     */
    public void convertAll() {
        if (converting.get()) { return; }
        var waiting = items.stream()
            .filter(item -> item.statusProperty().get() == ConversionStatus.WAITING)
            .toList();
        if (waiting.isEmpty()) { return; }
        converting.set(true);
        var task = new Task<Void>() {
            @Override
            protected Void call() {
                for (var item : waiting) {
                    convertOne(item);
                }
                return null;
            }
        };
        task.setOnSucceeded(_ -> converting.set(false));
        task.setOnFailed(_ -> {
            converting.set(false);
            throw rethrow(task.getException());
        });
        Thread.ofVirtual().start(task);
    }

    private void convertOne(ConversionItem item) {
        try {
            converter.convert(
                item.getDeck(), item.getPdf(),
                phase -> update(item, statusOf(phase), "")
            );
            update(item, ConversionStatus.DONE, "");
        } catch (ConversionException e) {
            update(item, ConversionStatus.FAILED, e.getMessage());
        }
    }

    private static ConversionStatus statusOf(ConversionPhase phase) {
        return switch (phase) {
            case PRINTING -> ConversionStatus.PRINTING;
            case CROPPING -> ConversionStatus.CROPPING;
        };
    }

    private static void update(ConversionItem item, ConversionStatus status, String message) {
        Platform.runLater(() -> item.setStatus(status, message));
    }

    private static RuntimeException rethrow(Throwable exception) {
        if (exception instanceof RuntimeException runtimeException) { return runtimeException; }
        if (exception instanceof Error error) { throw error; }
        // call() declares no checked exception, so this cannot happen.
        return new IllegalStateException(exception);
    }

    /**
     * Returns the list of decks. The list fires an update when the status of
     * an item changes.
     *
     * @return observable list of the items
     */
    public ObservableList<ConversionItem> getItems() {
        return items;
    }

    /**
     * Returns whether a conversion is running.
     *
     * @return read-only property that is true while a conversion runs
     */
    public ReadOnlyBooleanProperty convertingProperty() {
        return converting;
    }
}
