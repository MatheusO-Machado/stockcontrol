package br.com.matheus.stockcontrol;

import br.com.matheus.stockcontrol.ui.ProductsView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setScene(new javafx.scene.Scene(new br.com.matheus.stockcontrol.ui.MainView(), 900, 600));
        stage.setTitle("StockControl");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}