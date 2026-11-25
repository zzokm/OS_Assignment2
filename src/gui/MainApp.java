package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application launcher loading the Scene Builder MainView.fxml layout.
 */
public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1150, 720);
        primaryStage.setTitle("CS241 Assignment 2 - Cloud-Cluster CPU Scheduling Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
