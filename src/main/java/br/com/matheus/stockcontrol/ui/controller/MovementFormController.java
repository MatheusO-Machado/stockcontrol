package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.dao.StockMovementDao;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.model.Product;
import br.com.matheus.stockcontrol.model.StockMovementItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MovementFormController {

    @FXML private ComboBox<MovementType> cbType;
    @FXML private TextField txtObservation;

    @FXML private ComboBox<Product> cbProduct;
    @FXML private Spinner<Integer> spQuantity;
    @FXML private TextField txtUnitPrice;
    @FXML private Button btnAddItem;

    @FXML private TableView<StockMovementItem> tableItems;
    @FXML private Label lblTotal;
    @FXML private Button btnSave;

    private final ProductDao productDao = new ProductDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    private final ObservableList<StockMovementItem> items = FXCollections.observableArrayList();

    private boolean saved = false;

    public boolean isSaved() { return saved; }

    @FXML
    private void initialize() {
        cbType.getItems().setAll(MovementType.values());
        cbType.getSelectionModel().select(MovementType.ENTRADA);

        cbProduct.getItems().setAll(productDao.findAll());

        cbProduct.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getSku() + ")");
            }
        });
        cbProduct.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getSku() + ")");
            }
        });

        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1_000_000, 1));

        buildItemsTable();
        tableItems.setItems(items);

        // comportamento do valor unitário conforme tipo
        cbType.valueProperty().addListener((obs, old, type) -> {
            onTypeChanged(type);
            updateTotal();
        });
        cbProduct.valueProperty().addListener((obs, old, p) -> fillSuggestedUnitPrice());
        onTypeChanged(cbType.getValue());
        fillSuggestedUnitPrice();

        btnAddItem.setOnAction(e -> onAddItem());
        updateTotal();
        updateSaveButtonState();
    }

    private void onTypeChanged(MovementType type) {
        boolean needsUnit = (type == MovementType.ENTRADA || type == MovementType.SAIDA);
        txtUnitPrice.setDisable(!needsUnit);

        // limpa sugestão e recalcula
        fillSuggestedUnitPrice();
    }

    private void fillSuggestedUnitPrice() {
        Product p = cbProduct.getValue();
        if (p == null) return;

        MovementType type = cbType.getValue();
        BigDecimal suggested = (type == MovementType.SAIDA) ? p.getSalePrice() : p.getCostPrice();
        if (suggested == null) suggested = BigDecimal.ZERO;

        txtUnitPrice.setText(suggested.toPlainString());
    }

    private void buildItemsTable() {
        tableItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StockMovementItem, String> colProduct = new TableColumn<>("Produto");
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<StockMovementItem, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("productSku"));

        TableColumn<StockMovementItem, Integer> colQty = new TableColumn<>("Qtd");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<StockMovementItem, BigDecimal> colUnit = new TableColumn<>("Unit.");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colUnit.setCellFactory(tc -> moneyCell());

        TableColumn<StockMovementItem, BigDecimal> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colSub.setCellFactory(tc -> moneyCell());

        TableColumn<StockMovementItem, Void> colRemove = new TableColumn<>("Ações");
        colRemove.setMaxWidth(90);
        colRemove.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("Remover");
            {
                btn.setOnAction(e -> {
                    StockMovementItem it = getTableView().getItems().get(getIndex());
                    items.remove(it);
                    updateTotal();
                    updateSaveButtonState();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tableItems.getColumns().setAll(colProduct, colSku, colQty, colUnit, colSub, colRemove);
    }

    private TableCell<StockMovementItem, BigDecimal> moneyCell() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : nf.format(value));
            }
        };
    }

    private void onAddItem() {
        try {
            Product product = cbProduct.getValue();
            if (product == null) throw new IllegalArgumentException("Selecione um produto.");

            int qty = spQuantity.getValue();
            if (qty <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");

            BigDecimal unit = parseMoney(txtUnitPrice.getText());
            if (unit.signum() < 0) throw new IllegalArgumentException("Valor unitário não pode ser negativo.");

            // Se já existe item do produto, soma quantidade e atualiza unit
            StockMovementItem existing = items.stream()
                    .filter(i -> i.getProductId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                StockMovementItem it = new StockMovementItem();
                it.setProductId(product.getId());
                it.setProductName(product.getName());
                it.setProductSku(product.getSku());
                it.setQuantity(qty);
                it.setUnitPrice(unit);
                it.setSubtotal(unit.multiply(BigDecimal.valueOf(qty)));
                items.add(it);
            } else {
                existing.setQuantity(existing.getQuantity() + qty);
                existing.setUnitPrice(unit);
                existing.setSubtotal(unit.multiply(BigDecimal.valueOf(existing.getQuantity())));
                tableItems.refresh();
            }

            updateTotal();
            updateSaveButtonState();

            // reset qty para facilitar
            spQuantity.getValueFactory().setValue(1);
            cbProduct.requestFocus();
        } catch (IllegalArgumentException e) {
            showError("Validação", e.getMessage());
        } catch (Exception e) {
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (StockMovementItem it : items) {
            BigDecimal sub = (it.getSubtotal() == null) ? BigDecimal.ZERO : it.getSubtotal();
            total = total.add(sub);
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        lblTotal.setText(nf.format(total));
    }

    private void updateSaveButtonState() {
        btnSave.setDisable(items.isEmpty());
    }

    @FXML
    private void onCancel() {
        saved = false;
        closeWindow();
    }

    @FXML
    private void onSave() {
        try {
            MovementType type = cbType.getValue();
            if (type == null) throw new IllegalArgumentException("Selecione o tipo.");

            // garante subtotal consistente
            for (StockMovementItem it : items) {
                BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                it.setSubtotal(unit.multiply(BigDecimal.valueOf(it.getQuantity())));
            }

            movementDao.createMovement(type, items, txtObservation.getText());

            saved = true;
            closeWindow();
        } catch (IllegalArgumentException e) {
            showError("Validação", e.getMessage());
        } catch (Exception e) {
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private BigDecimal parseMoney(String text) {
        if (text == null) return BigDecimal.ZERO;
        String s = text.trim();
        if (s.isEmpty()) return BigDecimal.ZERO;

        s = s.replace(",", ".");
        return new BigDecimal(s);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) cbType.getScene().getWindow();
        stage.close();
    }
}