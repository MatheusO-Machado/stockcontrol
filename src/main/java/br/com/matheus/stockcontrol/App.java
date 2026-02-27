package br.com.matheus.stockcontrol;

import br.com.matheus.stockcontrol.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        MainView root = new MainView();
        Scene scene = new Scene(root, 900, 600);

        scene.getStylesheets().add(
                getClass().getResource("/br/com/matheus/stockcontrol/ui/app.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("StockControl");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}