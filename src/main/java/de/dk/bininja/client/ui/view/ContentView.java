package de.dk.bininja.client.ui.view;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.client.ui.UIController;
import de.dk.bininja.net.ConnectionRefusedException;
import de.dk.util.StringUtils;
import de.dk.fx.NumberTextField;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ContentView extends Pane {
   private static final Logger LOGGER = LoggerFactory.getLogger(ContentView.class);

   public static final double MARGIN = 8;
   private static final double TXT_PORT_WIDTH = 64;

   private final Label lblServer;
   private final TextField txtServer;

   private final Label lblPort;
   private final NumberTextField txtPort;

   private final Button btnConnect;

   private final List<DownloadView> downloads = new LinkedList<>();

   private final Line line;

   private final Label lblMsg;

   private final UIController controller;

   public ContentView(UIController listener, int defaultPort) {
      this.controller = Objects.requireNonNull(listener);

      setPrefWidth(FXAdapter.WIDTH);
      setPrefHeight(FXAdapter.HEIGHT);

      this.lblServer = new Label("Downloadserver URL");
      this.txtServer = new TextField();
      this.lblPort = new Label("Port");
      this.txtPort = new NumberTextField(defaultPort, 0, 0xffff);
      this.btnConnect = new Button("Connect");
      txtServer.setOnAction(e -> btnConnect.fire());
      txtServer.textProperty().addListener(this::txtServerChanged);
      btnConnect.setOnAction(this::btnConnectAction);

      this.line = new Line();
      line.setStrokeWidth(2);
      line.setFill(Color.BLACK);

      this.lblMsg = new Label();

      getChildren().addAll(lblServer,
                           lblPort,
                           txtServer,
                           txtPort,
                           btnConnect,
                           lblMsg);
   }

   @Override
   protected void layoutChildren() {
      this.lblServer.setLayoutX(MARGIN);
      lblServer.setLayoutY(MARGIN);
      this.txtServer.setLayoutX(lblServer.getLayoutX());
      txtServer.setLayoutY(lblServer.getLayoutY() + lblServer.getHeight());

      txtPort.setLayoutY(txtServer.getLayoutY());
      txtPort.setPrefWidth(TXT_PORT_WIDTH);
      lblPort.setLayoutY(lblServer.getLayoutY());

      txtServer.setPrefWidth(getWidth() - 2 * MARGIN - TXT_PORT_WIDTH - btnConnect.getWidth());
      this.txtPort.setLayoutX(txtServer.getLayoutX() + txtServer.getPrefWidth());
      this.lblPort.setLayoutX(txtPort.getLayoutX());

      this.btnConnect.setLayoutX(txtPort.getLayoutX() + txtPort.getPrefWidth());
      btnConnect.setLayoutY(txtPort.getLayoutY());
      btnConnect.setPrefHeight(txtPort.getHeight());

      if (!downloads.isEmpty()) {
         line.setStartX(0);
         line.setEndX(getWidth());
         line.setStartY(txtServer.getLayoutY() + txtServer.getHeight() + MARGIN);
         line.setEndY(line.getStartY());

         layoutDownloads();
      }

      this.lblMsg.setLayoutX(MARGIN);
      lblMsg.setLayoutY(getHeight() - MARGIN - lblMsg.getHeight());

      super.layoutChildren();
   }

   private void layoutDownloads() {
      double y = line.getEndY() + MARGIN;
      double width = getWidth() - MARGIN * 2;

      for (DownloadView dv : downloads) {
         dv.setLayoutX(MARGIN);
         dv.setLayoutY(y);
         dv.setPrefWidth(width);
         y += DownloadView.HEIGHT + MARGIN;
      }
   }

   private void txtServerChanged(ObservableValue<?> val, String old, String newVal) {
      btnConnect.setDisable(StringUtils.isBlank(newVal));
   }

   private void btnConnectAction(ActionEvent e) {
      if (controller.activeDownloadCount() > 0) {
         String msg = "Sie haben bereits eine bestehende Verbindung zu "
                      + controller.getConnectionAsString()
                      + ", über die noch Downloads laufen."
                      + "Wollen Sie die Verbindung schließen und alle "
                      + "noch nicht abgeschlossenen Downloads abbrechen?";

         Alert alert = new Alert(AlertType.WARNING,
                                 msg,
                                 ButtonType.YES,
                                 ButtonType.NO);

         alert.setTitle("BiNinjaClient - Wirklich?");
         Optional<ButtonType> result = alert.showAndWait();
         if (!result.isPresent() || result.get() != ButtonType.YES)
            return;
      }

      LOGGER.debug("Connect triggered.");
      String host = txtServer.getText();
      int port = txtPort.getValue();
      try {
         controller.connect(host, port);
      } catch (IOException | ConnectionRefusedException ex) {
         showError("Could not connect to " + host + ":" + port + " - " + ex.getMessage());
      }
   }

   public void show(String format, Object... args) {
      show(String.format(format, args), false);
   }

   public void showError(String errorMsg, Object... args) {
      show(String.format(errorMsg, args), true);
   }

   public void show(String msg, boolean error) {
      lblMsg.setTextFill(error ? Color.RED : Color.BLACK);
      lblMsg.setText(msg);
   }

   public void connected() {
      if (downloads.size() > 0)
         return;

      for (int i = 0; i < 3; i++) {
         DownloadView dv = new DownloadView(controller);
         downloads.add(dv);
         getChildren().add(dv);
      }
      getChildren().add(line);
      Platform.runLater(() -> {
         layoutChildren();
         for (DownloadView dv : downloads)
            dv.layoutChildren();
      });
   }

   public void disconnected() {
      Platform.runLater(() -> {
         for (DownloadView dv : downloads)
            getChildren().remove(dv);

         downloads.clear();
         getChildren().remove(line);
      });
   }

   public List<DownloadView> getDownloads() {
      return downloads;
   }

}
