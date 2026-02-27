package br.com.matheus.stockcontrol.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class MainView extends BorderPane {

    private final Label lblTitle = new Label("StockControl");

    // Sidebar com ícones (Unicode)
    private final Button btnDashboard = new Button("📊  Dashboard");
    private final Button btnProducts = new Button("📦  Produtos");
    private final Button btnCategories = new Button("🏷  Categorias");
    private final Button btnMovements = new Button("📈  Movimentações");
    private final Button btnParties = new Button("👥  Pessoas");

    private final List<Button> navButtons = List.of(btnDashboard, btnProducts, btnCategories, btnMovements, btnParties);

    private final ProductsView productsView = new ProductsView();
    private final DashboardView dashboardView = new DashboardView();
    private final CategoriesView categoriesView = new CategoriesView();

    private final MovementsView movementsView = new MovementsView(() -> {
        productsView.refresh();
        dashboardView.refresh();
        categoriesView.refresh();
    });

    private final PartiesView partiesView = new PartiesView();

    // “Shell” do conteúdo
    private final StackPane contentArea = new StackPane();
    private final VBox contentContainer = new VBox();
    private final BorderPane contentCard = new BorderPane();

    public MainView() {
        setPadding(Insets.EMPTY);

        // ---------- Top bar ----------
        ToolBar topBar = new ToolBar();
        lblTitle.getStyleClass().add("page-title");
        topBar.getItems().add(lblTitle);
        setTop(topBar);

        // ---------- Sidebar ----------
        for (Button b : navButtons) setupMenuButton(b);

        Label sideTitle = new Label("Menu");
        sideTitle.getStyleClass().add("side-title");

        VBox leftMenu = new VBox(10, sideTitle, btnDashboard, btnProducts, btnCategories, btnMovements, btnParties);
        leftMenu.getStyleClass().add("side-menu");
        leftMenu.setPrefWidth(240);
        leftMenu.setAlignment(Pos.TOP_LEFT);
        setLeft(leftMenu);

        // ---------- Content shell ----------
        contentArea.getStyleClass().add("content-area");

        contentContainer.getStyleClass().add("content-container");
        contentContainer.setFillWidth(true);
        contentContainer.setAlignment(Pos.TOP_CENTER);

        contentCard.getStyleClass().add("content-card");

        contentContainer.getChildren().add(contentCard);
        contentArea.getChildren().add(contentContainer);
        setCenter(contentArea);

        // handlers
        btnDashboard.setOnAction(e -> showDashboard());
        btnProducts.setOnAction(e -> showProducts());
        btnCategories.setOnAction(e -> showCategories());
        btnMovements.setOnAction(e -> showMovements());
        btnParties.setOnAction(e -> showParties());

        showDashboard();
    }

    private void setupMenuButton(Button b) {
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("menu-button");
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setWrapText(false);
    }

    private void setActive(Button active) {
        for (Button b : navButtons) b.getStyleClass().remove("active");
        active.getStyleClass().add("active");
    }

    private void setContent(Node node) {
        contentCard.setCenter(node);
        BorderPane.setMargin(node, new Insets(6));
    }

    private void showDashboard() {
        setActive(btnDashboard);
        lblTitle.setText("Dashboard");
        setContent(dashboardView);
        dashboardView.refresh();
    }

    private void showProducts() {
        setActive(btnProducts);
        lblTitle.setText("Produtos");
        setContent(productsView);
        productsView.refresh();
    }

    private void showCategories() {
        setActive(btnCategories);
        lblTitle.setText("Categorias");
        setContent(categoriesView);
        categoriesView.refresh();
    }

    private void showMovements() {
        setActive(btnMovements);
        lblTitle.setText("Movimentações");
        setContent(movementsView);
    }

    private void showParties() {
        setActive(btnParties);
        lblTitle.setText("Pessoas");
        setContent(partiesView);
    }
}