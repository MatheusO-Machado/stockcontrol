package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.CategoryDao;
import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Category;
import br.com.matheus.stockcontrol.model.Product;
import br.com.matheus.stockcontrol.store.CategoryStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Comparator;
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

    @FXML private CheckBox chkActive;

    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao(); // mantido (mesmo que não use diretamente)

    private final CategoryStore categoryStore = CategoryStore.getInstance();
    private final CategoryStore.Listener categoryListener = this::onCategoriesChanged;

    private Product editingProduct; // null = novo; != null = editar
    private boolean saved = false;

    public boolean isSaved() { return saved; }

    @FXML
    private void initialize() {
        // Estoque:
        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));
        spQuantity.setDisable(true); // estoque só via movimentações

        spMinStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1_000_000, 0));
        spMinStock.setDisable(false);

        // Novo produto:
        lblTitle.setText("Novo Produto");

        // SKU automático no modo "novo"
        txtSku.setText(productDao.getNextSku());
        txtSku.setDisable(true);

        // Custo: não é editável (controlado por ENTRADAS / custo médio)
        txtCostPrice.setDisable(true);
        txtCostPrice.setText("0.00");

        if (chkActive != null) chkActive.setSelected(true);

        categoryStore.addListener(categoryListener);

        Platform.runLater(() -> {
            Stage stage = (Stage) txtName.getScene().getWindow();
            stage.setOnHidden(e -> categoryStore.removeListener(categoryListener));
        });

        categoryStore.reload();
    }

    private void onCategoriesChanged(List<Category> categories) {
        Platform.runLater(() -> {
            Long selectedId = null;
            Category selected = cbCategory.getSelectionModel().getSelectedItem();
            if (selected != null) selectedId = selected.getId();

            cbCategory.getItems().setAll(sortCategoriesForUi(categories));

            if (selectedId != null) {
                Long finalSelectedId = selectedId;
                cbCategory.getItems().stream()
                        .filter(c -> c.getId() != null && c.getId().equals(finalSelectedId))
                        .findFirst()
                        .ifPresentOrElse(
                                c -> cbCategory.getSelectionModel().select(c),
                                this::selectFallbackOrFirst
                        );
            } else {
                if (editingProduct != null && editingProduct.getCategoryId() != null) {
                    Long editCatId = editingProduct.getCategoryId();
                    cbCategory.getItems().stream()
                            .filter(c -> c.getId() != null && c.getId().equals(editCatId))
                            .findFirst()
                            .ifPresentOrElse(
                                    c -> cbCategory.getSelectionModel().select(c),
                                    this::selectFallbackOrFirst
                            );
                } else {
                    selectFallbackOrFirst();
                }
            }
        });
    }

    private List<Category> sortCategoriesForUi(List<Category> categories) {
        return categories.stream()
                .sorted(Comparator
                        .comparing((Category c) -> CategoryDao.FALLBACK_NAME.equalsIgnoreCase(c.getName()))
                        .thenComparing(c -> c.getName() == null ? "" : c.getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void selectFallbackOrFirst() {
        cbCategory.getItems().stream()
                .filter(c -> c.getName() != null && CategoryDao.FALLBACK_NAME.equalsIgnoreCase(c.getName()))
                .findFirst()
                .ifPresentOrElse(
                        c -> cbCategory.getSelectionModel().select(c),
                        () -> {
                            if (!cbCategory.getItems().isEmpty()) cbCategory.getSelectionModel().select(0);
                            else cbCategory.getSelectionModel().clearSelection();
                        }
                );
    }

    public void setProductToEdit(Product p) {
        this.editingProduct = p;

        lblTitle.setText("Editar Produto");

        txtName.setText(p.getName());

        txtSku.setText(p.getSku());
        txtSku.setDisable(true);

        // Mostra custo, mas não deixa editar
        txtCostPrice.setText(p.getCostPrice() != null ? p.getCostPrice().toPlainString() : "0.00");
        txtCostPrice.setDisable(true);

        // Venda pode editar
        txtSalePrice.setText(p.getSalePrice() != null ? p.getSalePrice().toPlainString() : "0.00");

        // Estoque só via movimentação (mas mostra)
        spQuantity.getValueFactory().setValue(p.getQuantity());
        spQuantity.setDisable(true);

        spMinStock.getValueFactory().setValue(p.getMinStock());
        spMinStock.setDisable(false);

        if (chkActive != null) chkActive.setSelected(p.isActive());

        Platform.runLater(() -> {
            if (p.getCategoryId() != null) {
                cbCategory.getItems().stream()
                        .filter(c -> c.getId() != null && c.getId().equals(p.getCategoryId()))
                        .findFirst()
                        .ifPresent(c -> cbCategory.getSelectionModel().select(c));
            }
        });
    }

    @FXML
    private void onCancel() {
        saved = false;
        closeWindow();
    }

    @FXML
    private void onSave() {
        try {
            Product p = readAndValidate();

            if (editingProduct == null) {
                // Estoque sempre começa 0 (movimentações controlam)
                p.setQuantity(0);

                // SKU automático
                if (p.getSku() == null || p.getSku().isBlank()) {
                    p.setSku(productDao.getNextSku());
                }

                // Custo vem das entradas; no cadastro inicia 0
                p.setCostPrice(BigDecimal.ZERO);

                productDao.insert(p);
            } else {
                p.setId(editingProduct.getId());
                p.setSku(editingProduct.getSku());

                // Mantém estoque e custo atuais (controlados por movimentação)
                p.setQuantity(editingProduct.getQuantity());
                p.setCostPrice(editingProduct.getCostPrice() != null ? editingProduct.getCostPrice() : BigDecimal.ZERO);

                productDao.update(p);
            }

            saved = true;
            closeWindow();
        } catch (IllegalArgumentException e) {
            showError("Validação", e.getMessage());
        } catch (Exception e) {
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private Product readAndValidate() {
        String name = safeTrim(txtName.getText());
        String sku = safeTrim(txtSku.getText());

        Category category = cbCategory.getSelectionModel().getSelectedItem();
        Integer minStock = spMinStock.getValue();

        BigDecimal sale = parseMoney(txtSalePrice, "Preço venda inválido. Ex: 19,90");

        if (name.isEmpty()) {
            txtName.requestFocus();
            throw new IllegalArgumentException("Nome é obrigatório.");
        }

        if (editingProduct == null && sku.isEmpty()) {
            throw new IllegalArgumentException("SKU é obrigatório.");
        }

        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("Categoria é obrigatória. Cadastre ao menos 1 categoria antes.");
        }

        if (sale.signum() < 0) {
            throw new IllegalArgumentException("Preço venda não pode ser negativo.");
        }

        Product p = new Product();
        p.setName(name);
        p.setSku(editingProduct == null ? sku : editingProduct.getSku());
        p.setCategoryId(category.getId());
        p.setCategoryName(category.getName());

        // custo/estoque controlados por movimentação (mas ao editar, serão preservados no onSave)
        p.setCostPrice(BigDecimal.ZERO);
        p.setSalePrice(sale);

        p.setQuantity(0);
        p.setMinStock(minStock != null ? minStock : 0);

        boolean active = chkActive == null || chkActive.isSelected();
        p.setActive(active);

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