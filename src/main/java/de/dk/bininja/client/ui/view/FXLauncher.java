package de.dk.bininja.client.ui.view;

import javafx.application.Application;
import javafx.stage.Stage;

public class FXLauncher extends Application {
   private static GUI gui;

   public FXLauncher() {

   }

   public static void start(GUI gui) {
      if (FXLauncher.gui != null)
         throw new IllegalStateException("FX application already launched.");

      FXLauncher.gui = gui;
      launch();
   }

   @Override
   public void start(Stage window) {
      if (gui == null)
         throw new IllegalStateException("No gui to set.");

      gui.start(window);
   }

   @Override
   public void stop() {
      gui.stop();
   }

}
