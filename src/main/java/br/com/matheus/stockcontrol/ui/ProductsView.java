package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.scene.Parent;


import java.io.IOException;
import java.math.BigDecimal;

public class ProductsView extends BorderPane {

    private final TableView<Product> table = new TableView<>();
    private final ProductDao dao = new ProductDao();

    public ProductsView() {
        setPadding(new Insets(10));

        var btnNew = new Button("Novo");
        var btnEdit = new Button("Editar");
        var btnDelete = new Button("Excluir");

        btnNew.setOnAction(e -> {
            openProductForm();
            loadData();
            // recarrega depois que o modal fecha
        });
        
        btnDelete.setOnAction(e -> onDelete());
        btnEdit.setOnAction(e -> onEdit());
        
        setTop(new ToolBar(btnNew, btnEdit, btnDelete));

        var colId = new TableColumn<Product, Long>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        var colName = new TableColumn<Product, String>("Nome");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(220);

        var colSku = new TableColumn<Product, String>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setPrefWidth(120);

        var colQty = new TableColumn<Product, Integer>("Qtd");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setPrefWidth(80);

        var colPrice = new TableColumn<Product, BigDecimal>("Preço");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(100);

        table.getColumns().addAll(colId, colName, colSku, colQty, colPrice);
        setCenter(table);

        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.findAll()));
    }

    private void openProductForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/matheus/stockcontrol/ui/product_form.fxml")
            );

            javafx.scene.Parent root = loader.load(); // <-- aqui resolve o cast

            Stage stage = new Stage();
            stage.setTitle("Novo Produto");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao abrir formulário de produto", e);
        }
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
    
    private void openProductFormForEdit(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/matheus/stockcontrol/ui/product_form.fxml")
            );

            Parent root = loader.load();

            // pega o controller criado pelo FXMLLoader
            br.com.matheus.stockcontrol.ui.controller.ProductFormController controller = loader.getController();
            controller.setProductToEdit(product);

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
    
    private void onDelete() {
        Product selected = table.getSelectionModel().getSelectedItem();
        System.out.println("DELETE click - selected=" + selected);

        if (selected == null) {
            showInfo("Selecione um produto para excluir.");
            return;
        }

        System.out.println("Selected id=" + selected.getId());

        boolean confirmed = confirm(
                "Confirmar exclusão",
                "Excluir o produto: " + selected.getName() + " (SKU: " + selected.getSku() + ")?"
        );

        System.out.println("Confirmed=" + confirmed);

        if (!confirmed) return;

  
        int rows = dao.deleteById(selected.getId());
        System.out.println("Rows deleted=" + rows);
        loadData();
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
    
        
}