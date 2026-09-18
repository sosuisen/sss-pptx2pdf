package com.sosuisha.pptx2pdf.main;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.sosuisha.pptx2pdf.domain.exception.UnrecoverableException;
import com.sosuisha.pptx2pdf.presentation.View;
import com.sosuisha.pptx2pdf.presentation.WindowManager;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionAppModel;
import com.sosuisha.pptx2pdf.presentation.screens.alert.AlertDialog;
import com.sosuisha.pptx2pdf.presentation.screens.main.MainView;
import com.sosuisha.pptx2pdf.presentation.screens.main.MainViewModel;
import com.sosuisha.pptx2pdf.service.PdfBoxPdfCropper;
import com.sosuisha.pptx2pdf.service.PowerShellDeckPrinter;
import com.sosuisha.pptx2pdf.service.PptxSlideSizeReader;
import com.sosuisha.pptx2pdf.service.PrintAndCropDeckConverter;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * JavaFX application of SSS pptx2pdf.
 */
public class App extends Application {
    /** Change this constant during development to open another window first. */
    static final Class<? extends View> FIRST_VIEW = MainView.class;

    /**
     * Composition root that wires the dependencies and shows the first window.
     *
     * @param stage the primary stage for this application
     * @throws NullPointerException if stage is null
     */
    @Override
    public void start(Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        Thread.currentThread().setUncaughtExceptionHandler((_, e) -> {
            if (e instanceof UnrecoverableException unrecoverable) {
                AlertDialog.showError(unrecoverable.getMessage());
            } else {
                e.printStackTrace();
                AlertDialog.showUnexpectedError(e);
            }
        });
        setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var windowManager = new WindowManager();
        var appModel = new ConversionAppModel(
            new PrintAndCropDeckConverter(
                new PptxSlideSizeReader(), new PowerShellDeckPrinter(), new PdfBoxPdfCropper()
            )
        );
        // The main window owns the file chooser dialog. It is looked up when
        // the dialog opens, because the view is registered later.
        var mainViewModel = new MainViewModel(
            appModel,
            () -> chooseDecks(windowManager.getView(MainView.class).getScene().getWindow())
        );
        windowManager.registerView(new MainView(mainViewModel));
        windowManager.showWindow(FIRST_VIEW, stage);
    }

    private static List<Path> chooseDecks(Window ownerWindow) {
        var chooser = new FileChooser();
        chooser.setTitle("PowerPointファイルを選択");
        chooser.getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("PowerPoint (*.pptx)", "*.pptx"));
        var files = chooser.showOpenMultipleDialog(ownerWindow);
        return files == null ? List.of() : files.stream().map(File::toPath).toList();
    }
}
