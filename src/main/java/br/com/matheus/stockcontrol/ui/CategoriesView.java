package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.store.CategoryStore;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.Optional;

public class CategoriesView extends BorderPane {

    private final CategoryDao categoryDao = new CategoryDao();
    private final CategoryStore categoryStore = CategoryStore.getInstance();

    private final TableView<CategoryDao.CategoryRow> table = new TableView<>();

    public CategoriesView() {
        setPadding(new Insets(0));
        setTop(buildTop());
        setCenter(buildTable());
        loadData();
    }

    private Parent buildTop() {
        Button btnNew = new Button("Nova");
        Button btnEdit = new Button("Editar");
        Button btnDelete = new Button("Excluir");
        Button btnRefresh = new Button("Atualizar");

        btnNew.getStyleClass().add("btn-new");
        btnEdit.getStyleClass().add("btn-edit");
        btnDelete.getStyleClass().add("btn-delete");
        // btnRefresh fica default (branco)

        btnNew.setOnAction(e -> onNew());
        btnEdit.setOnAction(e -> onEdit());
        btnDelete.setOnAction(e -> onDelete());
        btnRefresh.setOnAction(e -> loadData());

        ToolBar actions = new ToolBar(btnNew, btnEdit, btnDelete, new Separator(), btnRefresh);

        var header = new javafx.scene.layout.VBox(10, actions);
        header.getStyleClass().add("view-header");
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        return header;
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<CategoryDao.CategoryRow, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().id()));
        colId.setMaxWidth(80);

        TableColumn<CategoryDao.CategoryRow, String> colName = new TableColumn<>("Nome");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name()));

        TableColumn<CategoryDao.CategoryRow, Number> colCount = new TableColumn<>("Produtos");
        colCount.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().productsCount()));
        colCount.setMaxWidth(120);

        colName.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value);
                    if (CategoryDao.FALLBACK_NAME.equalsIgnoreCase(value)) {
                        setStyle("-fx-font-style: italic;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        table.getColumns().setAll(colId, colName, colCount);
        return table;
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(categoryDao.findAllWithProductsCount()));
    }

    private void onNew() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova categoria");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            categoryDao.insert(result.get());
            loadData();
            categoryStore.reload();
        } catch (Exception e) {
            showError("Erro ao criar categoria", e.getMessage());
        }
    }

    private void onEdit() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecione uma categoria para editar.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.name());
        dialog.setTitle("Editar categoria");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            br.com.matheus.stockcontrol.model.Category updated =
                    new br.com.matheus.stockcontrol.model.Category(selected.id(), result.get());

            categoryDao.update(updated);
            loadData();
            categoryStore.reload();
        } catch (Exception e) {
            showError("Erro ao editar categoria", e.getMessage());
        }
    }

    private void onDelete() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecione uma categoria para excluir.");
            return;
        }

        boolean confirmed = confirm(
                "Confirmar exclusão",
                "Excluir a categoria: " + selected.name() + "?\n\n" +
                        "Se houver produtos nessa categoria, eles serão movidos automaticamente para '" + CategoryDao.FALLBACK_NAME + "'."
        );

        if (!confirmed) return;

        try {
            categoryDao.deleteById(selected.id());
            loadData();
            categoryStore.reload();
        } catch (Exception e) {
            showError("Erro ao excluir categoria", e.getMessage());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
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