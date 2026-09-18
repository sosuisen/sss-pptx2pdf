package com.sosuisha.pptx2pdf.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sosuisha.pptx2pdf.presentation.screens.main.MainView;

import javafx.application.Application;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppTest {
  private Stage stage;

  @Start
  void setup(Stage stage) {
    // The injected primary stage is reused across tests and rejects
    // initStyle, so App gets a fresh stage.
    this.stage = new Stage();
    new App().start(this.stage);
  }

  @Test
  @DisplayName("アプリを起動すると、FIRST_VIEW定数で指定したViewのウィンドウが表示される")
  void app_startup_shows_the_window_of_the_view_specified_by_first_view_constant() {
    var expectedTitles = Map.of(MainView.class, "sss-pptx2pdf");

    assertTrue(stage.isShowing());
    assertEquals(expectedTitles.get(App.FIRST_VIEW), stage.getTitle());
  }

  @Test
  @DisplayName("起動時にAtlantaFXのPrimer Lightテーマが適用されている")
  void the_atlantafx_primer_light_theme_is_applied_at_startup() {
    var stylesheet = Application.getUserAgentStylesheet();

    assertNotNull(stylesheet);
    assertTrue(stylesheet.endsWith("primer-light.css"));
  }
}
