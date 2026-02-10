package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the main menu view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainMenu.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        primaryStage.setTitle("Interview Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

