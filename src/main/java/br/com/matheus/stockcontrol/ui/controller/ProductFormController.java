package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class ProductFormController {

    @FXML private TextField txtName;
    @FXML private TextField txtSku;
    @FXML private Spinner<Integer> spQuantity;
    @FXML private TextField txtPrice;

    private final ProductDao dao = new ProductDao();

    @FXML
    private void initialize() {
        // Spinner precisa de um "modelo" de valores; aqui: 0 até 1 milhão.
        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    @FXML
    private void onSave() {
        // 1) Ler e validar (validação simples por enquanto)
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

        BigDecimal price;
        try {
            // Aceita "199.90". (Depois melhoramos para aceitar vírgula também)
            price = new BigDecimal(priceText);
        } catch (Exception e) {
            txtPrice.requestFocus();
            throw new IllegalArgumentException("Preço inválido. Use formato 199.90");
        }

        // 2) Montar objeto
        Product p = new Product(null, name, sku, qty != null ? qty : 0, price);

        // 3) Salvar no banco
        dao.insert(p);

        // 4) Fechar janela
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}