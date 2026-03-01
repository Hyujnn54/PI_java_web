package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/views/user/Login.fxml"));
        Scene scene = new Scene(root);

        // If you use a SceneManager, initialize it once
        Utils.SceneManager.init(stage, scene);

        stage.setTitle("RH Project");
        stage.setScene(scene);

        // ✅ Good default window size
        stage.setWidth(1000);
        stage.setHeight(720);

        // ✅ Prevent tiny ugly window
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        stage.centerOnScreen();
        stage.setResizable(true); // put false if you want fixed size

        stage.show();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("UNCAUGHT in thread " + t.getName());
            e.printStackTrace();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}