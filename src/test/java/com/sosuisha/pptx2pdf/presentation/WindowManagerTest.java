package com.sosuisha.pptx2pdf.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import com.sosuisha.pptx2pdf.domain.service.NullDeckConverter;
import com.sosuisha.pptx2pdf.presentation.appmodel.ConversionAppModel;
import com.sosuisha.pptx2pdf.presentation.screens.main.MainView;
import com.sosuisha.pptx2pdf.presentation.screens.main.MainViewModel;

import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class WindowManagerTest {
    private static MainView newMainView() {
        return new MainView(
            new MainViewModel(new ConversionAppModel(new NullDeckConverter()), List::of)
        );
    }

    @Test
    @DisplayName("登録したViewをクラス指定で取得できる")
    void returns_the_registered_view_by_its_class() {
        var windowManager = new WindowManager();
        var view = newMainView();
        windowManager.registerView(view);

        assertSame(view, windowManager.getView(MainView.class));
    }

    @Test
    @DisplayName("未登録のViewを要求するとIllegalArgumentExceptionになる")
    void requesting_an_unregistered_view_throws_illegal_argument_exception() {
        var windowManager = new WindowManager();

        assertThrows(IllegalArgumentException.class, () -> windowManager.getView(MainView.class));
    }

    @Test
    @DisplayName("showWindowすると、ウィンドウのアイコンにアプリのアイコン（images/icon.png）が設定される")
    void show_window_sets_the_app_icon_on_the_window(FxRobot robot) {
        var windowManager = new WindowManager();
        windowManager.registerView(newMainView());

        robot.interact(() -> windowManager.showWindow(MainView.class, new Stage()));

        var window = (Stage) robot.window("SSS pptx2pdf");
        assertEquals(1, window.getIcons().size());
        assertEquals(256, window.getIcons().getFirst().getWidth());
    }
}
