package de.dk.bininja.client.ui.view;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import de.dk.bininja.client.model.DownloadMetadata;
import de.dk.bininja.client.ui.UIController;
import de.dk.bininja.net.DownloadListener;
import de.dk.bininja.net.DownloadState;
import de.dk.util.StringUtils;
import de.dk.util.timing.PulseController;
import de.dk.util.unit.memory.MemoryUnit;
import de.dk.util.unit.memory.MemoryValue;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class DownloadView extends Pane implements DownloadListener {
   public static final int HEIGHT = 166;

   private static final String FILECHOOSER_TITLE = "Downloadziel wählen";
   private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
   private static final float PROGRESS_UPDATES_PER_SECOND = 2;

   private final UIController controller;

   private final Label lblUrl;
   private final TextField txtUrl;
   private final Button btnDownload;

   private final Label lblTarget;
   private final TextField txtTarget;
   private final Button btnSelectTarget;

   private final FileChooser targetChooser;

   private ProgressBar progressBar;
   private final Label lblLoadProgress;
   private Label lblWriteProgress;


   private int downloadId;
   private final PulseController progressUpdatePulse;
   private MemoryValue loadProgress = new MemoryValue(0);
   private MemoryValue writeProgress = new MemoryValue(0);
   private MemoryValue length;

   public DownloadView(UIController controller) {
      this.controller = controller;

      this.lblUrl = new Label("URL");
      this.txtUrl = new TextField();
      txtUrl.textProperty()
            .addListener(this::urlChanged);

      this.btnDownload = new Button("Download");
      btnDownload.setOnAction(this::btnDownloadAction);

      this.lblTarget = new Label("Speicherort");
      this.txtTarget = new TextField();
      this.btnSelectTarget = new Button("...");
      btnSelectTarget.setOnAction(this::btnSelectTargetAction);

      this.targetChooser = new FileChooser();
      this.lblLoadProgress = new Label();

      this.progressBar = new ProgressBar();
      progressBar.setVisible(false);
      this.lblWriteProgress = new Label();
      lblWriteProgress.setFont(new Font(12));

      this.progressUpdatePulse = new PulseController(p -> updateProgress(), PROGRESS_UPDATES_PER_SECOND);

      getChildren().addAll(lblUrl,
                           txtUrl,
                           btnDownload,
                           lblTarget,
                           txtTarget,
                           btnSelectTarget,
                           progressBar,
                           lblLoadProgress,
                           lblWriteProgress);

      setPrefHeight(HEIGHT);
      setPadding(new Insets(4));
      setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
   }

   @Override
   protected void layoutChildren() {
      lblUrl.setLayoutX(ClientView.MARGIN);
      lblUrl.setLayoutY(ClientView.MARGIN);
      txtUrl.setLayoutX(ClientView.MARGIN);
      txtUrl.setLayoutY(ClientView.MARGIN + lblUrl.getHeight());
      txtUrl.setPrefWidth(getWidth() - ClientView.MARGIN * 2 - btnDownload.getWidth());
      btnDownload.setLayoutX(ClientView.MARGIN + txtUrl.getPrefWidth());
      btnDownload.setLayoutY(txtUrl.getLayoutY());

      lblTarget.setLayoutX(ClientView.MARGIN);
      lblTarget.setLayoutY(txtUrl.getLayoutY() + txtUrl.getHeight() + ClientView.MARGIN);
      txtTarget.setLayoutX(ClientView.MARGIN);
      txtTarget.setLayoutY(lblTarget.getLayoutY() + lblTarget.getHeight());
      txtTarget.setPrefWidth(getWidth() - ClientView.MARGIN * 2 - btnSelectTarget.getWidth());
      btnSelectTarget.setLayoutX(txtTarget.getLayoutX() + txtTarget.getPrefWidth());
      btnSelectTarget.setLayoutY(txtTarget.getLayoutY());

      progressBar.setLayoutY(txtTarget.getLayoutY() + txtTarget.getHeight() + ClientView.MARGIN);
      progressBar.setPrefWidth(getWidth() - ClientView.MARGIN * 2);
      lblLoadProgress.setLayoutX(ClientView.MARGIN);
      lblLoadProgress.setLayoutY(progressBar.getLayoutY() + progressBar.getHeight());
      lblWriteProgress.setLayoutX(ClientView.MARGIN);
      lblWriteProgress.setLayoutY(lblLoadProgress.getLayoutY() + lblLoadProgress.getHeight());
      progressBar.setLayoutX(ClientView.MARGIN);

      super.layoutChildren();
   }

   public void prepare(DownloadMetadata metadata) {
      progressUpdatePulse.reset();
      this.length = null;
      loadProgress.setValue(0);
      writeProgress.setValue(0);
      progressBar.setProgress(0);
      progressBar.setVisible(true);
      this.lblLoadProgress.setText("Initialisiere...");
      lblLoadProgress.setFont(new Font(12));
      lblLoadProgress.setBackground(null);

      setLength(metadata.getLength());
   }

   @Override
   public void stateChanged(DownloadState state) {
      switch (state) {
      case INITIALIZING:
         setLoadText("Initialisiere...", null);
         break;

      case CANCELLED:
         setLoadText("abgebrochen", Color.YELLOW);
         Platform.runLater(this::downloadTerminated);
         break;

      case ERROR:
         setLoadText("Fehler!", Color.RED);
         Platform.runLater(this::downloadTerminated);
         break;

      case COMPLETE:
         setLoadText("Fertig", Color.color(0.5, 1, 0.5));
         Platform.runLater(this::downloadTerminated);
         break;

      default:
         break;
      }
   }

   private void downloadTerminated() {
      getChildren().removeAll(lblWriteProgress, progressBar);
      btnDownload.setDisable(false);
   }

   @Override
   public void loadProgress(double progress, long receivedBytes, long total) {
      this.loadProgress.setValue(MemoryUnit.BYTE.convertTo(receivedBytes, loadProgress.getUnit()));
      progressUpdatePulse.update();
   }

   @Override
   public void writeProgress(double progress, long writtenBytes, long total) {
      this.writeProgress.setValue(MemoryUnit.BYTE.convertTo(writtenBytes, writeProgress.getUnit()));
      progressUpdatePulse.update();
   }

   private void updateProgress() {
      double progress;
      String writeProgress;
      String loadProgress;
      if (length == null) {
         progress = -1;
         loadProgress = "Ladefortschritt: " + this.loadProgress.toString(FORMAT) + " loaded";
         writeProgress = "Schreibfortschritt: " + this.writeProgress.toString(FORMAT) + " written";
      } else {
         progress = this.writeProgress.getValue() / length.getValue();
         loadProgress = String.format("Ladefortschritt: %s/%s",
                                      this.loadProgress.toString(FORMAT),
                                      length.toString(FORMAT));

         writeProgress = String.format("Schreibfortschritt: %s/%s",
                                       this.writeProgress.toString(FORMAT),
                                       length.toString(FORMAT));
      }

      Platform.runLater(() -> {
         progressBar.setProgress(progress);
         lblWriteProgress.setText(writeProgress);
         lblLoadProgress.setText(loadProgress);
      });
   }

   public void setLength(long length) {
      try {
         this.length = new MemoryValue(length);
      } catch (IllegalArgumentException e) {
         // Nothing to do here
      }
      if (this.length != null) {
         this.loadProgress.setUnit(this.length.getUnit());
         this.writeProgress.setUnit(this.length.getUnit());
      }
      updateProgress();
   }

   private void urlChanged(ObservableValue<?> val, String old, String newVal) {
      txtTarget.setText("");
   }

   private void setLoadText(String text, Color color) {
      Platform.runLater(() -> {
         lblLoadProgress.setText(text);
         if (color != null) {
            lblLoadProgress.setBackground(new Background(new BackgroundFill(color,
                                                                            CornerRadii.EMPTY,
                                                                            new Insets(0))));
         }
      });
   }

   public File getDownloadTarget() {
      return StringUtils.isBlank(txtTarget.getText()) ? null : new File(txtTarget.getText());
   }

   public File requestDownloadTarget(String fileName, long length) {
      if (fileName != null)
         targetChooser.setInitialFileName(fileName);

      targetChooser.setTitle(FILECHOOSER_TITLE + (length < 0 ? "" : new MemoryValue(length)));
      File target = targetChooser.showSaveDialog(getScene().getWindow());
      if (target != null) {
         txtTarget.setText(target.getAbsolutePath());
         targetChooser.setInitialDirectory(target.getParentFile());
      }

      return target;
   }

   private void btnDownloadAction(ActionEvent e) {
      btnDownload.setDisable(true);
      lblLoadProgress.setBackground(null);
      lblLoadProgress.setText("");
      URL url;
      try {
         url = new URL(txtUrl.getText());
      } catch (MalformedURLException ex) {
         show("Ungültige URL \"" + txtUrl.getText() + "\"", true);
         btnDownload.setDisable(false);
         return;
      }
      Platform.runLater(() -> {
         DownloadMetadata metadata = new DownloadMetadata(url);
         this.downloadId = metadata.getId();
         boolean downloadStarted = controller.requestDownloadFrom(metadata, this);
         btnDownload.setDisable(downloadStarted);
      });
   }

   public void show(String format, Object... args) {
      show(String.format(format, args), false);
   }

   public void showError(String errorMsg, Object... args) {
      show(String.format(errorMsg, args), true);
   }

   public void show(String msg, boolean error) {
      lblLoadProgress.setTextFill(error ? Color.RED : Color.BLACK);
      lblLoadProgress.setText(msg);
   }

   private void btnSelectTargetAction(ActionEvent e) {
      requestDownloadTarget(null, -1);
   }

   public int getDownloadId() {
      return downloadId;
   }
}
