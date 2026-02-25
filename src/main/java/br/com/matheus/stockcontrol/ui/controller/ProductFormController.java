package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Category;
import br.com.matheus.stockcontrol.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

public class ProductFormController {

    @FXML private Label lblTitle;

    @FXML private TextField txtName;
    @FXML private TextField txtSku;

    @FXML private ComboBox<Category> cbCategory;

    @FXML private TextField txtCostPrice;
    @FXML private TextField txtSalePrice;

    @FXML private Spinner<Integer> spQuantity;
    @FXML private Spinner<Integer> spMinStock;

    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();

    private Product editingProduct; // null = novo; != null = editar

    @FXML
    private void initialize() {
        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));
        spMinStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));

        loadCategories();

        lblTitle.setText("Novo Produto");
        txtSku.setDisable(false);
    }

    private void loadCategories() {
        List<Category> categories = categoryDao.findAll();
        cbCategory.getItems().setAll(categories);

        // Seleciona "Geral" por padrão
        Category geral = categories.stream()
                .filter(c -> "Geral".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElse(categories.isEmpty() ? null : categories.get(0));

        if (geral != null) cbCategory.getSelectionModel().select(geral);
    }

    public void setProductToEdit(Product p) {
        this.editingProduct = p;

        lblTitle.setText("Editar Produto");

        txtName.setText(p.getName());
        txtSku.setText(p.getSku());
        txtSku.setDisable(true); // SKU não editável (jeito mais certo)

        // seleciona categoria no combo
        if (p.getCategoryId() != null) {
            cbCategory.getItems().stream()
                    .filter(c -> c.getId().equals(p.getCategoryId()))
                    .findFirst()
                    .ifPresent(c -> cbCategory.getSelectionModel().select(c));
        }

        txtCostPrice.setText(p.getCostPrice() != null ? p.getCostPrice().toPlainString() : "0.00");
        txtSalePrice.setText(p.getSalePrice() != null ? p.getSalePrice().toPlainString() : "0.00");

        spQuantity.getValueFactory().setValue(p.getQuantity());
        spMinStock.getValueFactory().setValue(p.getMinStock());
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
                productDao.insert(p);
            } else {
                p.setId(editingProduct.getId());
                // SKU não muda (o campo está disable), mas garantimos por segurança:
                p.setSku(editingProduct.getSku());
                productDao.update(p);
            }

            closeWindow();
        } catch (Exception e) {
            showError("Não foi possível salvar", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private Product readAndValidate() {
        String name = safeTrim(txtName.getText());
        String sku = safeTrim(txtSku.getText());

        Category category = cbCategory.getSelectionModel().getSelectedItem();
        Integer qty = spQuantity.getValue();
        Integer minStock = spMinStock.getValue();

        BigDecimal cost = parseMoney(txtCostPrice, "Preço custo inválido. Ex: 10,50");
        BigDecimal sale = parseMoney(txtSalePrice, "Preço venda inválido. Ex: 19,90");

        if (name.isEmpty()) {
            txtName.requestFocus();
            throw new IllegalArgumentException("Nome é obrigatório.");
        }

        // Em modo editar, sku vem do produto original
        if (editingProduct == null && sku.isEmpty()) {
            txtSku.requestFocus();
            throw new IllegalArgumentException("SKU é obrigatório.");
        }

        if (category == null) {
            throw new IllegalArgumentException("Categoria é obrigatória.");
        }

        if (cost.signum() < 0 || sale.signum() < 0) {
            throw new IllegalArgumentException("Preços não podem ser negativos.");
        }

        Product p = new Product();
        p.setName(name);
        p.setSku(editingProduct == null ? sku : editingProduct.getSku());
        p.setCategoryId(category.getId());
        p.setCategoryName(category.getName());
        p.setCostPrice(cost);
        p.setSalePrice(sale);
        p.setQuantity(qty != null ? qty : 0);
        p.setMinStock(minStock != null ? minStock : 0);

        return p;
    }

    private BigDecimal parseMoney(TextField field, String errorMessage) {
        String text = safeTrim(field.getText());
        if (text.isEmpty()) return BigDecimal.ZERO;

        text = text.replace(",", ".");
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            field.requestFocus();
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
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