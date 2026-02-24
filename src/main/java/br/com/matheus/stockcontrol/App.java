package br.com.matheus.stockcontrol;

import br.com.matheus.stockcontrol.ui.ProductsView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        var root = new ProductsView();
        var scene = new Scene(root, 700, 400);
        stage.setTitle("StockControl");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}