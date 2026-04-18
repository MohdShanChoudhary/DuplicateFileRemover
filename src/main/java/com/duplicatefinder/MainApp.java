package com.duplicatefinder;

import com.duplicatefinder.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow window = new MainWindow(primaryStage);
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
