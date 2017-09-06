package de.dk.bininja.client.ui.view;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.client.model.DownloadMetadata;
import de.dk.bininja.client.ui.UI;
import de.dk.bininja.client.ui.UIController;
import de.dk.bininja.net.Base64Connection;
import de.dk.util.FileUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class FXAdapter implements UI {
   private static final Logger LOGGER = LoggerFactory.getLogger(FXAdapter.class);

   private static final String TITLE = "BiNinja Client";

   public static final double WIDTH = 800;
   public static final double HEIGHT = 666;

   private UIController controller;
   private ContentView view;

   private Stage window;

   private boolean connected;

   public FXAdapter(UIController controller) {
      this.controller = controller;
   }

   public void start(Stage window) {
      this.window = window;
      this.view = new ContentView(controller, Base64Connection.PORT);
      window.setScene(new Scene(view, WIDTH, HEIGHT));
      window.setTitle(TITLE);
      window.show();

      if (connected)
         view.connected();
      else
         view.disconnected();
   }

   @Override
   public void start() {
      LOGGER.debug("Launching javafx");
      FXLauncher.start(this);
   }

   @Override
   public void show(String format, Object... args) {
      if (started())
         view.show(format, args);
   }

   @Override
   public void showError(String errorMsg, Object... args) {
      if (started())
         view.show(errorMsg, args);
   }

   @Override
   public void alert(String format, Object... args) {
      Platform.runLater(() -> {
         Alert alert = new Alert(AlertType.INFORMATION,
                                 String.format(format, args),
                                 ButtonType.CLOSE);

         alert.setTitle("BiNinjaClient - Mitteilung");
         alert.showAndWait();
      });
   }

   @Override
   public void alertError(String errorMsg, Object... args) {
      Alert alert = new Alert(AlertType.ERROR,
                              errorMsg,
                              ButtonType.APPLY);

      alert.setTitle("BiNinjaClient - Fehler!");
      alert.showAndWait();
   }

   @Override
   public void setConnected(boolean connected) {
      this.connected = connected;
      if (!started())
         return;

      if (connected)
         view.connected();
      else
         view.disconnected();
   }

   @Override
   public void setDownloadTargetTo(DownloadMetadata metadata) {
      DownloadView dv = view.getDownloads().stream()
                                           .filter(v -> v.getDownloadId() == metadata.getId())
                                           .findAny()
                                           .orElseThrow(IllegalStateException::new);

      File target = dv.getDownloadTarget();
      if (FileUtils.isBlank(target)) {
         target = dv.requestDownloadTarget(metadata.getFileName(), metadata.getLength());
         if (FileUtils.isBlank(target)) {
            metadata.setTargetDirectory(null);
         } else if (target.isDirectory()) {
            metadata.setTargetDirectory(target);
         } else {
            metadata.setTargetDirectory(target.getParentFile());
            metadata.setFileName(target.getName());
         }
      } else {
         if (target.isDirectory()) {
            metadata.setTargetDirectory(target);
         } else {
            metadata.setTargetDirectory(target.getParentFile());
            metadata.setFileName(target.getName());
         }
      }
   }

   public boolean started() {
      return view != null;
   }

   @Override
   public void close() {
      if (window != null)
         window.close();

      view = null;
   }

   public void stop() {
      controller.exit();
   }

}
