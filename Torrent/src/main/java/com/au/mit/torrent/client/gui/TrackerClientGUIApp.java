package com.au.mit.torrent.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class TrackerClientGUIApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("client-gui.fxml"));
        Parent root = loader.load();
        ClientGUIController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle("Benchmark client");
        primaryStage.setScene(new Scene(root, 800, 350));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
