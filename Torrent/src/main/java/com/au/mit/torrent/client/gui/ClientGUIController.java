package com.au.mit.torrent.client.gui;

import com.au.mit.torrent.client.ClientImpl;
import com.au.mit.torrent.client.TorrentFile;
import com.au.mit.torrent.common.exceptions.CommunicationException;
import com.au.mit.torrent.common.protocol.FileDescription;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClientGUIController {
    private static final Logger logger = Logger.getLogger(ClientGUIController.class.getName());
    private static final Path torrentsFolder = Paths.get("torrents");
    private static final short PORT = (short)(8082+new Random().nextInt(100));
    private static final short TRACKER_PORT = 8081;

    private final String progressBarStylesheet = getClass().getClassLoader().
            getResource("progress_bar.css").toExternalForm();

    public Label textLog;
    public Button buttonDownload;
    public ListView<FileDescription> trackerList;
    public TextField textFieldTrackerIP;
    public Button buttonUpdate;
    public ListView<TorrentFile> ownList;
    private Stage stage;

    private final ClientImpl client = new ClientImpl(PORT, torrentsFolder, this::updateDownloading);

    public ClientGUIController() throws IOException {
    }

    public void publish(ActionEvent actionEvent) {
        if (!client.isConnected()) {
            showWarning("No connection established");
            return;
        }

        File file = new FileChooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }

        client.uploadFile(file.getPath());

        final List<TorrentFile> localFilesInfo = client.getLocalFiles().stream().collect(Collectors.toList());
        ObservableList<TorrentFile> localItems = FXCollections.observableArrayList(localFilesInfo);
        ownList.setItems(localItems);
        ownList.setCellFactory(ProgressBarListCell::new);
    }

    public void update(ActionEvent actionEvent) {
        buttonDownload.setDisable(true);
        final String trackerHostname = textFieldTrackerIP.getText();
        if (!client.isConnected()) {
            try {
                client.connect(trackerHostname, TRACKER_PORT);
            } catch (CommunicationException e) {
                showException(e);
                return;
            }
        } else {
            client.listRequest();
        }
        final List<FileDescription> trackerFilesInfo = client.getTrackerFiles().values().stream().collect(Collectors.toList());
        ObservableList<FileDescription> items = FXCollections.observableArrayList(trackerFilesInfo);
        trackerList.setItems(items);
        textLog.setText(String.format("Status: connected, total tracker files count: %d", items.size()));
    }

    public void download(ActionEvent actionEvent) {
        final FileDescription selectedFile = trackerList.getSelectionModel().getSelectedItem();
        textLog.setText(String.format("Status: start downloading file %s", selectedFile.getName()));
        if (!client.downloadFile(selectedFile.getId())) {
            showWarning("Local files couldn't be downloaded");
        }

        final List<TorrentFile> localFilesInfo = client.getLocalFiles().stream().collect(Collectors.toList());
        ObservableList<TorrentFile> items = FXCollections.observableArrayList(localFilesInfo);
        ownList.setItems(items);
        ownList.setCellFactory(ProgressBarListCell::new);
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }

    public void selectTrackerFile(MouseEvent mouseEvent) {
        buttonDownload.setDisable(false);
    }

    private void updateDownloading(TorrentFile torrentFile) {
        listViewUpdate(ownList);
        Platform.runLater(() -> {
            if (torrentFile.getRatio() == 1.0) {
                textLog.setText(String.format("Status: file %s successfully downloaded", torrentFile.getFileDescription().getName()));
            }
        });
    }

    /**
     * Informs the ListView that one of its items has been modified.
     *  @param listView The ListView to trigger.
     *
     */
    private static <T> void listViewUpdate(ListView<T> listView) {
        Platform.runLater(() -> {
            final ObservableList<T> items = listView.getItems();
            listView.setItems(null);
            listView.setItems(items);
        });
    }

    private void showException(Exception e) {
        logger.log(Level.WARNING, "Something gone wrong during the benchmarking", e);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning!");
        alert.setHeaderText(null);
        String exceptionMessage = e.getMessage();
        Exception curException = (Exception) e.getCause();
        while (curException != null) {
            exceptionMessage += ": " + curException.getMessage();
            curException = (Exception) curException.getCause();
        }
        alert.setContentText(exceptionMessage);
        alert.showAndWait();
        textLog.setText("Status: not started");
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning!");
        alert.setContentText(message);
        alert.showAndWait();
    }

    class ProgressBarListCell extends ListCell<TorrentFile> {
        private ProgressBar bar = new ProgressBar();
        private Label label = new Label();
        private Pane pane = new Pane();

        ProgressBarListCell(ListView<TorrentFile> listView) {
            pane.getStylesheets().add(progressBarStylesheet);
            bar.prefWidthProperty().bind(listView.widthProperty().subtract(15));
            bar.setMaxWidth(Control.USE_PREF_SIZE);
            bar.getStyleClass().add("green-bar");
            pane.getChildren().add(bar);
            label.setLayoutX(5);
            pane.getChildren().add(label);
        }

        @Override
        public void updateItem(TorrentFile item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                label.setText(item.toString());
                bar.setProgress(item.getRatio());
                setGraphic(pane);
            }
        }
    }
}