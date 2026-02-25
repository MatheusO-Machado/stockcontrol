package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Category;
import br.com.matheus.stockcontrol.model.Product;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class ProductsView extends BorderPane {

    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();

    private final TableView<Product> table = new TableView<>();

    // filtros (B: dentro da tela)
    private final TextField txtSearch = new TextField();
    private final ComboBox<Category> cbCategoryFilter = new ComboBox<>();
    private final Button btnClear = new Button("Limpar");

    public ProductsView() {
        setPadding(new Insets(10));

        setTop(buildTop());
        setCenter(buildTable());

        loadCategoryFilter();
        loadData(); // inicial
    }

    private Parent buildTop() {
        // Barra de ações
        var btnNew = new Button("Novo");
        var btnEdit = new Button("Editar");
        var btnDelete = new Button("Excluir");

        btnNew.setOnAction(e -> {
            openProductForm();
            loadData();
        });

        btnEdit.setOnAction(e -> onEdit());
        btnDelete.setOnAction(e -> onDelete());

        // Barra de filtros
        txtSearch.setPromptText("Buscar por Nome ou SKU...");
        txtSearch.setPrefWidth(280);

        cbCategoryFilter.setPromptText("Categoria");
        cbCategoryFilter.setPrefWidth(200);

        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbCategoryFilter.getSelectionModel().clearSelection();
            loadData();
        });

        ChangeListener<Object> refilter = (obs, oldVal, newVal) -> loadData();
        txtSearch.textProperty().addListener(refilter);
        cbCategoryFilter.valueProperty().addListener(refilter);

        var filters = new HBox(10, new Label("Busca:"), txtSearch, new Label("Categoria:"), cbCategoryFilter, btnClear);
        filters.setPadding(new Insets(0, 0, 10, 0));

        var actions = new ToolBar(btnNew, btnEdit, btnDelete);

        var box = new javafx.scene.layout.VBox(10, actions, filters);
        return box;
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Product, String> colName = new TableColumn<>("Nome");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<Product, String> colCategory = new TableColumn<>("Categoria");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        TableColumn<Product, BigDecimal> colCost = new TableColumn<>("Custo");
        colCost.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colCost.setCellFactory(tc -> moneyCell());

        TableColumn<Product, BigDecimal> colSale = new TableColumn<>("Venda");
        colSale.setCellValueFactory(new PropertyValueFactory<>("salePrice"));
        colSale.setCellFactory(tc -> moneyCell());

        TableColumn<Product, Integer> colQty = new TableColumn<>("Qtd");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, Integer> colMin = new TableColumn<>("Mín.");
        colMin.setCellValueFactory(new PropertyValueFactory<>("minStock"));

        table.getColumns().setAll(colName, colSku, colCategory, colCost, colSale, colQty, colMin);

        return table;
    }

    private TableCell<Product, BigDecimal> moneyCell() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(nf.format(value));
                }
            }
        };
    }

    private void loadCategoryFilter() {
        var list = FXCollections.observableArrayList(categoryDao.findAll());
        cbCategoryFilter.setItems(list);
    }

    private void loadData() {
        String text = txtSearch.getText();
        Category cat = cbCategoryFilter.getSelectionModel().getSelectedItem();
        Long categoryId = (cat != null) ? cat.getId() : null;

        table.setItems(FXCollections.observableArrayList(productDao.search(text, categoryId)));
    }

    private void onEdit() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecione um produto para editar.");
            return;
        }

        openProductFormForEdit(selected);
        loadData();
    }

    private void onDelete() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecione um produto para excluir.");
            return;
        }

        boolean confirmed = confirm(
                "Confirmar exclusão",
                "Excluir o produto: " + selected.getName() + " (SKU: " + selected.getSku() + ")?"
        );

        if (!confirmed) return;

        int rows = productDao.deleteById(selected.getId());
        if (rows == 0) {
            showInfo("Produto não encontrado para excluir (pode já ter sido removido).");
        }

        loadData();
    }

    // Reaproveita seu modal existente (NOVO)
    private void openProductForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/matheus/stockcontrol/ui/product_form.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Novo Produto");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir formulário de produto", e);
        }
    }

    // Reaproveita seu modal existente (EDITAR)
    private void openProductFormForEdit(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/matheus/stockcontrol/ui/product_form.fxml")
            );

            Parent root = loader.load();

            var controller = loader.getController();
            // chamamos o método público do controller v2
            br.com.matheus.stockcontrol.ui.controller.ProductFormController c =
                    (br.com.matheus.stockcontrol.ui.controller.ProductFormController) controller;

            c.setProductToEdit(product);

            Stage stage = new Stage();
            stage.setTitle("Editar Produto");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir formulário de edição", e);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    public void refresh() {
    loadData();
    }
}