package br.com.matheus.stockcontrol.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {

    private final TextField txtSearch = new TextField();
    private final Label lblTitle = new Label("StockControl");

    private final Button btnProducts = new Button("Produtos");
    private final Button btnMovements = new Button("Movimentações");
    private final Button btnDashboard = new Button("Dashboard");

    // Mantém instâncias para poder dar refresh sem recriar tudo
    private final ProductsView productsView = new ProductsView();
    private final MovementsView movementsView = new MovementsView(productsView::refresh);

    public MainView() {
        setPadding(new Insets(10));

        // Top: título + busca (por enquanto, não conectada ao ProductsView)
        var topBar = new ToolBar();
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        txtSearch.setPromptText("Buscar por Nome ou SKU...");
        txtSearch.setPrefWidth(300);

        topBar.getItems().addAll(lblTitle, new Separator(), txtSearch);
        setTop(topBar);

        // Left: menu
        btnProducts.setMaxWidth(Double.MAX_VALUE);
        btnMovements.setMaxWidth(Double.MAX_VALUE);
        btnDashboard.setMaxWidth(Double.MAX_VALUE);

        var leftMenu = new VBox(8, btnProducts, btnMovements, btnDashboard);
        leftMenu.setPadding(new Insets(10, 10, 10, 0));
        leftMenu.setPrefWidth(160);
        setLeft(leftMenu);

        // Center: default
        showProducts();

        // Navegação
        btnProducts.setOnAction(e -> showProducts());
        btnMovements.setOnAction(e -> showMovements());
        btnDashboard.setOnAction(e -> showDashboard());
    }

    private void showProducts() {
        setCenter(productsView);
        lblTitle.setText("Produtos");
    }

    private void showMovements() {
        setCenter(movementsView);
        lblTitle.setText("Movimentações");
    }

    private void showDashboard() {
        setCenter(new Label("Dashboard (em construção)"));
        lblTitle.setText("Dashboard");
    }
}