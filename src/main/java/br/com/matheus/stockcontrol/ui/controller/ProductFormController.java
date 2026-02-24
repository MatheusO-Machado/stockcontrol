package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class ProductFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtSku;
    @FXML private Spinner<Integer> spQuantity;
    @FXML private TextField txtPrice;

    private final ProductDao dao = new ProductDao();

    // se null => modo "novo"; se não-null => modo "editar"
    private Product editingProduct;

    @FXML
    private void initialize() {
        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));
        setTitleNew();
    }

    // Chamado pela tela que abre o modal (ProductsView)
    public void setProductToEdit(Product p) {
        this.editingProduct = p;

        lblTitle.setText("Editar Produto");

        txtName.setText(p.getName());
        txtSku.setText(p.getSku());
        spQuantity.getValueFactory().setValue(p.getQuantity());
        txtPrice.setText(p.getPrice() != null ? p.getPrice().toPlainString() : "0.00");
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    @FXML
    private void onSave() {
        try {
            Product p = readAndValidate();

            if (editingProduct == null) {
                dao.insert(p);
            } else {
                p.setId(editingProduct.getId()); // mantém o ID original
                dao.update(p);
            }

            closeWindow();
        } catch (Exception e) {
            showError("Não foi possível salvar", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private Product readAndValidate() {
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String sku = txtSku.getText() != null ? txtSku.getText().trim() : "";
        Integer qty = spQuantity.getValue();
        String priceText = txtPrice.getText() != null ? txtPrice.getText().trim() : "";

        if (name.isEmpty()) {
            txtName.requestFocus();
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (sku.isEmpty()) {
            txtSku.requestFocus();
            throw new IllegalArgumentException("SKU é obrigatório.");
        }
        if (priceText.isEmpty()) {
            txtPrice.requestFocus();
            throw new IllegalArgumentException("Preço é obrigatório.");
        }

        priceText = priceText.replace(",", ".");
        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
        } catch (Exception e) {
            txtPrice.requestFocus();
            throw new IllegalArgumentException("Preço inválido. Exemplo: 199,90");
        }

        if (price.signum() < 0) {
            txtPrice.requestFocus();
            throw new IllegalArgumentException("Preço não pode ser negativo.");
        }

        return new Product(null, name, sku, qty != null ? qty : 0, price);
    }

    private void setTitleNew() {
        if (lblTitle != null) lblTitle.setText("Novo Produto");
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}