/*
 * Copyright (C) 2020 Amy Bryant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.aabryant.eo.nexus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.github.darmbrust.ToolTipDefaultsFixer;
import com.github.aabryant.eo.nexus.gui.Display;
import com.github.aabryant.eo.nexus.manager.Data;
import com.github.aabryant.eo.nexus.model.Skill;

public class Main extends Application {
  private Scene scene;
  
  @Override
  public void start(Stage stage) {
    ToolTipDefaultsFixer.setTooltipTimers(0, 999999999, 200);
    Display display = new Display();
    scene = new Scene(display, 1572, 732);
    scene.setOnKeyPressed((e) -> {
      if (e.isControlDown()) {
        if (e.getCode() == KeyCode.O) {
          display.open();
        } else if (e.getCode() == KeyCode.S) {
          if (e.isShiftDown()) display.saveAs();
          else display.save();
        }
      }
    });
    scene.getStylesheets()
         .add(Main.class.getResource("/css/theme.css").toExternalForm());
    stage.setTitle("Etrian Odyssey Nexus Skill Respec");
    stage.setOnCloseRequest((WindowEvent e) -> Platform.exit());
    stage.setScene(scene);
    stage.show();
  }
  
  public static void main(String[] args) {
    if (args.length > 0) {
      Data.SAVE_PATH = args[0];
    }
    Data.load();
    launch(args);
  }
}
