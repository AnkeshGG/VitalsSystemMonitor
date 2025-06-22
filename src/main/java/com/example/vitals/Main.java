package com.example.vitals;

import com.example.vitals.controllers.DashboardController;
import com.example.vitals.utils.SystemMonitor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();
        DashboardController dashboardController = loader.getController();

        // Set application icon
        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon.png")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("App icon not found, continuing without icon");
        }

        Scene scene = new Scene(root, 1100, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
        primaryStage.setTitle("Vitals - System Monitoring Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start system monitoring in a background thread using the FXML controller instance.
        Thread monitorThread = new Thread(new SystemMonitor(dashboardController));
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
