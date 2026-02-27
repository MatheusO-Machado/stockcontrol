package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Category;
import br.com.matheus.stockcontrol.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductsView extends BorderPane {

    private final ProductDao dao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();

    private final TableView<Product> table = new TableView<>();

    private final TextField txtSearch = new TextField();
    private final ComboBox<Category> cbCategory = new ComboBox<>();
    private final CheckBox chkShowInactive = new CheckBox("Mostrar inativos");

    public ProductsView() {
        setPadding(new Insets(0));
        setTop(buildTop());
        setCenter(buildTable());

        loadCategories();
        loadData();
    }

    private Parent buildTop() {
        Button btnNew = new Button("Novo");
        Button btnEdit = new Button("Editar");

        Button btnInactivate = new Button("Inativar");
        Button btnReactivate = new Button("Reativar");

        Button btnDelete = new Button("Excluir");

        btnNew.getStyleClass().add("btn-new");
        btnEdit.getStyleClass().add("btn-edit");
        btnDelete.getStyleClass().add("btn-delete");

        btnNew.setOnAction(e -> {
            boolean changed = openProductForm(null);
            if (changed) loadData();
        });

        btnEdit.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um produto para editar.");
                return;
            }
            boolean changed = openProductForm(selected);
            if (changed) loadData();
        });

        btnInactivate.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um produto para inativar.");
                return;
            }
            if (!selected.isActive()) {
                showInfo("Status", "Este produto já está inativo.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar inativação");
            confirm.setHeaderText(null);
            confirm.setContentText("Inativar \"" + selected.getName() + "\" (" + selected.getSku() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.setActive(selected.getId(), false);
                    loadData();
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        btnReactivate.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um produto para reativar.");
                return;
            }
            if (selected.isActive()) {
                showInfo("Status", "Este produto já está ativo.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar reativação");
            confirm.setHeaderText(null);
            confirm.setContentText("Reativar \"" + selected.getName() + "\" (" + selected.getSku() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.setActive(selected.getId(), true);
                    loadData();
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        btnDelete.setOnAction(e -> {
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um produto para excluir.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar exclusão");
            confirm.setHeaderText(null);
            confirm.setContentText("Excluir \"" + selected.getName() + "\" (" + selected.getSku() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.deleteById(selected.getId());
                    loadData();
                } catch (IllegalArgumentException ex) {
                    // FK / possui movimentações -> oferece inativar
                    Alert suggest = new Alert(Alert.AlertType.CONFIRMATION);
                    suggest.setTitle("Não é possível excluir");
                    suggest.setHeaderText(null);
                    suggest.setContentText(
                            (ex.getMessage() != null ? ex.getMessage() : ex.toString()) +
                            "\n\nDeseja inativar este produto?"
                    );

                    ButtonType btnYes = new ButtonType("Inativar", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                    suggest.getButtonTypes().setAll(btnYes, btnNo);

                    var r2 = suggest.showAndWait();
                    if (r2.isPresent() && r2.get() == btnYes) {
                        try {
                            dao.setActive(selected.getId(), false);
                            loadData();
                        } catch (Exception ex2) {
                            showError("Erro", ex2.getMessage() != null ? ex2.getMessage() : ex2.toString());
                        }
                    }
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        // habilitação dinâmica
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean has = sel != null;
            btnEdit.setDisable(!has);
            btnDelete.setDisable(!has);
            btnInactivate.setDisable(!has || !sel.isActive());
            btnReactivate.setDisable(!has || sel.isActive());
        });

        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnInactivate.setDisable(true);
        btnReactivate.setDisable(true);

        ToolBar toolbar = new ToolBar(
                btnNew, btnEdit,
                new Separator(),
                btnInactivate, btnReactivate,
                new Separator(),
                btnDelete
        );

        txtSearch.setPromptText("Buscar por nome ou SKU...");
        txtSearch.setPrefWidth(320);

        cbCategory.setPromptText("Categoria");
        cbCategory.setPrefWidth(220);

        chkShowInactive.setOnAction(e -> loadData());

        Button btnFilter = new Button("Buscar");
        Button btnClear = new Button("Limpar");

        btnFilter.setOnAction(e -> loadData());
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbCategory.getSelectionModel().clearSelection();
            chkShowInactive.setSelected(false);
            loadData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox filters = new HBox(10,
                new Label("Busca:"), txtSearch,
                new Label("Categoria:"), cbCategory,
                chkShowInactive,
                spacer,
                btnFilter, btnClear
        );
        filters.getStyleClass().add("filters-bar");

        var header = new javafx.scene.layout.VBox(10, toolbar, filters);
        header.getStyleClass().add("view-header");
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        return header;
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // ===== Destaques de estoque (CSS pseudo-classes: stock-zero / stock-low) =====
        PseudoClass stockZero = PseudoClass.getPseudoClass("stock-zero");
        PseudoClass stockLow = PseudoClass.getPseudoClass("stock-low");

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);

                boolean isZero = false;
                boolean isLow = false;

                // Mantém neutro para inativos (se quiser pintar inativo também, remova item.isActive())
                if (!empty && item != null && item.isActive()) {
                    int qty = item.getQuantity();
                    int min = item.getMinStock();

                    isZero = (qty == 0);
                    isLow = (!isZero && min > 0 && qty <= min);
                }

                pseudoClassStateChanged(stockZero, isZero);
                pseudoClassStateChanged(stockLow, isLow);
            }
        });
        // ===== fim =====

        TableColumn<Product, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(80);

        TableColumn<Product, String> colActive = new TableColumn<>("Status");
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Ativo" : "Inativo"));
        colActive.setMaxWidth(110);

        TableColumn<Product, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setMaxWidth(120);

        TableColumn<Product, String> colName = new TableColumn<>("Produto");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCat.setMaxWidth(180);

        TableColumn<Product, Integer> colQty = new TableColumn<>("Qtd");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setMaxWidth(90);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        TableColumn<Product, String> colSale = new TableColumn<>("Venda");
        colSale.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSalePrice() == null ? "" : nf.format(c.getValue().getSalePrice())
        ));
        colSale.setMaxWidth(120);

        table.getColumns().setAll(colId, colActive, colSku, colName, colCat, colQty, colSale);
        return table;
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            cbCategory.getItems().setAll(categories);
        } catch (Exception e) {
            cbCategory.getItems().clear();
        }
    }

    private void loadData() {
        String term = txtSearch.getText();
        Category cat = cbCategory.getSelectionModel().getSelectedItem();
        Long categoryId = cat == null ? null : cat.getId();
        boolean includeInactive = chkShowInactive.isSelected();

        table.setItems(FXCollections.observableArrayList(
                dao.search(term, categoryId, includeInactive)
        ));
    }

    private boolean openProductForm(Product productToEdit) {
        try {
            var url = getClass().getResource("/br/com/matheus/stockcontrol/ui/product_form.fxml");
            if (url == null) {
                throw new IllegalStateException("FXML não encontrado: /br/com/matheus/stockcontrol/ui/product_form.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            br.com.matheus.stockcontrol.ui.controller.ProductFormController controller = loader.getController();
            if (productToEdit != null) controller.setProductToEdit(productToEdit);

            Stage stage = new Stage();
            stage.setTitle(productToEdit == null ? "Novo Produto" : "Editar Produto");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.isSaved();
        } catch (Exception e) {
            e.printStackTrace();

            String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
            if (e.getCause() != null) {
                msg += "\nCausa: " + (e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getCause().toString());
            }

            showError("Erro ao abrir formulário", msg);
            return false;
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void refresh() {
        loadData();
    }
}