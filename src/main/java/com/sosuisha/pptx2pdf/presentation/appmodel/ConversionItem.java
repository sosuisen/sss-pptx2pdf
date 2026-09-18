package com.sosuisha.pptx2pdf.presentation.appmodel;

import java.nio.file.Path;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * A deck in the conversion list with its status. The status and the message
 * are changed only by the app model, on the JavaFX application thread.
 */
public class ConversionItem {
    private final Path deck;
    private final Path pdf;
    private final ObjectProperty<ConversionStatus> status =
        new SimpleObjectProperty<>(ConversionStatus.WAITING);
    private final StringProperty message = new SimpleStringProperty("");
    private final ObservableValue<String> displayText;

    /**
     * Creates the item in the {@link ConversionStatus#WAITING} status.
     *
     * @param deck path of the pptx file
     * @param pdf  path of the PDF file to write
     * @throws NullPointerException if deck or pdf is null
     */
    public ConversionItem(Path deck, Path pdf) {
        this.deck = Objects.requireNonNull(deck, "deck must not be null");
        this.pdf = Objects.requireNonNull(pdf, "pdf must not be null");
        this.displayText = Bindings.createStringBinding(this::formatDisplayText, status, message);
    }

    private String formatDisplayText() {
        var text = "[" + status.get().label() + "] " + deck.getFileName();
        return message.get().isEmpty() ? text : text + " - " + message.get();
    }

    /**
     * Returns the path of the pptx file.
     *
     * @return path of the deck
     */
    public Path getDeck() {
        return deck;
    }

    /**
     * Returns the path of the PDF file that the conversion writes.
     *
     * @return path of the PDF
     */
    public Path getPdf() {
        return pdf;
    }

    /**
     * Returns the status of the conversion.
     *
     * @return read-only property of the status
     */
    public ReadOnlyObjectProperty<ConversionStatus> statusProperty() {
        return status;
    }

    /**
     * Returns the message that explains the status, such as the reason of a
     * failure. Empty when there is nothing to say.
     *
     * @return read-only property of the message
     */
    public ReadOnlyStringProperty messageProperty() {
        return message;
    }

    /**
     * Returns the one-line text that represents this item in the list: the
     * status label, the file name, and the message when there is one.
     *
     * @return observable display text
     */
    public ObservableValue<String> displayTextProperty() {
        return displayText;
    }

    void setStatus(ConversionStatus newStatus, String newMessage) {
        status.set(newStatus);
        message.set(newMessage);
    }
}
