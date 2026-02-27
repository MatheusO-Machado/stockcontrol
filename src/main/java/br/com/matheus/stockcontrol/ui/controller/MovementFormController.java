package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.PartyDao;
import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.dao.StockMovementDao;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.model.Party;
import br.com.matheus.stockcontrol.model.PartyType;
import br.com.matheus.stockcontrol.model.Product;
import br.com.matheus.stockcontrol.model.StockMovementItem;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MovementFormController {

    @FXML private ComboBox<MovementType> cbType;
    @FXML private Label lblParty;
    @FXML private ComboBox<Party> cbParty;
    @FXML private Button btnNewParty;
    @FXML private TextField txtObservation;

    @FXML private TextField txtProductSearch;

    @FXML private ComboBox<Product> cbProduct;
    @FXML private Spinner<Integer> spQuantity;
    @FXML private TextField txtUnitPrice;
    @FXML private Button btnAddItem;

    @FXML private TableView<StockMovementItem> tableItems;
    @FXML private Label lblTotal;
    @FXML private Button btnSave;

    private final ProductDao productDao = new ProductDao();
    private final PartyDao partyDao = new PartyDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    private final ObservableList<StockMovementItem> items = FXCollections.observableArrayList();
    private List<Product> allProducts = List.of();

    private boolean saved = false;
    public boolean isSaved() { return saved; }

    private final PauseTransition partySearchDebounce = new PauseTransition(Duration.millis(250));
    private final PauseTransition productSearchDebounce = new PauseTransition(Duration.millis(150));

    @FXML
    private void initialize() {
        cbType.getItems().setAll(MovementType.values());
        cbType.getSelectionModel().select(MovementType.ENTRADA);

        cbParty.setEditable(true);
        cbParty.setPromptText("Digite nome ou CPF/CNPJ...");
        configurePartyAutocomplete();

        // Produtos: carrega tudo e prepara filtro por busca
        allProducts = productDao.findAll();
        configureProductCombo();
        configureProductSearch();

        // Quantidade: digitável (inteiro) e commita ao pressionar Enter
        spQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1_000_000, 1));
        spQuantity.setEditable(true);
        configureQuantitySpinnerCommit();

        // Enter na quantidade adiciona item
        spQuantity.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                commitQuantityEditor();
                onAddItem();
                e.consume();
            }
        });

        // Seleciona tudo quando focar (pra digitar por cima)
        Platform.runLater(() -> spQuantity.getEditor().focusedProperty().addListener((obs, was, is) -> {
            if (is) spQuantity.getEditor().selectAll();
        }));

        buildItemsTable();
        tableItems.setItems(items);

        cbType.valueProperty().addListener((obs, old, type) -> {
            onTypeChanged(type);
            fillSuggestedUnitPrice();
            updateTotal();
            refreshPartyForType();
        });

        cbProduct.valueProperty().addListener((obs, old, p) -> fillSuggestedUnitPrice());

        onTypeChanged(cbType.getValue());
        fillSuggestedUnitPrice();
        refreshPartyForType();

        btnAddItem.setOnAction(e -> {
            commitQuantityEditor();
            onAddItem();
        });
        btnNewParty.setOnAction(e -> onNewParty());

        updateTotal();
        updateSaveButtonState();
    }

    private void configureQuantitySpinnerCommit() {
        SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spQuantity.getValueFactory();

        vf.setConverter(new StringConverter<>() {
            @Override public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override public Integer fromString(String s) {
                if (s == null) return 1;
                String t = s.trim();
                if (t.isEmpty()) return 1;
                return Integer.parseInt(t); // inteiro
            }
        });

        spQuantity.getEditor().setOnAction(e -> commitQuantityEditor());
    }

    private void commitQuantityEditor() {
        try {
            SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) spQuantity.getValueFactory();

            Integer parsed = vf.getConverter().fromString(spQuantity.getEditor().getText());
            if (parsed == null) parsed = 1;

            // respeita limites do spinner
            int min = 1;
            int max = 1_000_000;
            int v = Math.max(min, Math.min(max, parsed));

            vf.setValue(v);
            spQuantity.getEditor().setText(String.valueOf(v));
        } catch (Exception ex) {
            // volta para o último valor válido
            SpinnerValueFactory<Integer> vf = spQuantity.getValueFactory();
            Integer current = vf == null ? 1 : vf.getValue();
            if (current == null) current = 1;
            spQuantity.getEditor().setText(String.valueOf(current));
        }
    }

    private void configureProductCombo() {
        cbProduct.getItems().setAll(allProducts);

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
    }

    private void configureProductSearch() {
        // Enter na busca: se tiver exatamente 1 resultado, seleciona e vai pra quantidade
        txtProductSearch.setOnAction(e -> {
            if (cbProduct.getValue() == null && cbProduct.getItems().size() == 1) {
                cbProduct.getSelectionModel().select(0);
            }

            if (cbProduct.getValue() != null) {
                spQuantity.requestFocus();
            } else {
                cbProduct.show();
            }
        });

        txtProductSearch.textProperty().addListener((obs, old, val) -> {
            productSearchDebounce.stop();
            productSearchDebounce.setOnFinished(e -> filterProducts(val));
            productSearchDebounce.playFromStart();
        });
    }

    private void filterProducts(String term) {
        String t = term == null ? "" : term.trim().toLowerCase();
        if (t.isEmpty()) {
            cbProduct.getItems().setAll(allProducts);
            cbProduct.setValue(null);
            return;
        }

        var filtered = allProducts.stream().filter(p -> {
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            String sku = p.getSku() == null ? "" : p.getSku().toLowerCase();
            return name.contains(t) || sku.contains(t);
        }).toList();

        cbProduct.getItems().setAll(filtered);

        if (filtered.size() == 1) {
            cbProduct.getSelectionModel().select(filtered.getFirst());
        } else {
            cbProduct.setValue(null);
            cbProduct.show();
        }
    }

    private void configurePartyAutocomplete() {
        cbParty.setConverter(new StringConverter<>() {
            @Override
            public String toString(Party p) {
                if (p == null) return "";
                return p.getName() + " - " + p.getDocument();
            }

            @Override
            public Party fromString(String s) {
                return null;
            }
        });

        cbParty.getEditor().textProperty().addListener((obs, old, val) -> {
            partySearchDebounce.stop();
            partySearchDebounce.setOnFinished(e -> searchParties(val));
            partySearchDebounce.playFromStart();
        });

        cbParty.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Party item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getDocument());
            }
        });

        cbParty.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Party item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getDocument());
            }
        });
    }

    private void refreshPartyForType() {
        cbParty.getItems().clear();
        cbParty.setValue(null);
        cbParty.getEditor().clear();

        PartyType partyType = requiredPartyType();
        cbParty.getItems().setAll(partyDao.search(partyType, "", 30));
    }

    private void searchParties(String term) {
        PartyType partyType = requiredPartyType();
        var results = partyDao.search(partyType, term, 30);
        cbParty.getItems().setAll(results);
        if (!results.isEmpty()) cbParty.show();
    }

    private PartyType requiredPartyType() {
        return cbType.getValue() == MovementType.SAIDA ? PartyType.CLIENTE : PartyType.FORNECEDOR;
    }

    private void onTypeChanged(MovementType type) {
        lblParty.setText(type == MovementType.SAIDA ? "Cliente:" : "Fornecedor:");
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

            // garante que digitado foi commitado
            commitQuantityEditor();

            int qty = spQuantity.getValue();
            if (qty <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");

            BigDecimal unit = parseMoney(txtUnitPrice.getText());
            if (unit.signum() < 0) throw new IllegalArgumentException("Valor unitário não pode ser negativo.");

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

            spQuantity.getValueFactory().setValue(1);

            // fluxo rápido
            txtProductSearch.clear();
            cbProduct.setValue(null);
            txtProductSearch.requestFocus();
        } catch (IllegalArgumentException e) {
            showError("Validação", e.getMessage());
        } catch (Exception e) {
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void onNewParty() {
        try {
            PartyType partyType = requiredPartyType();

            var url = getClass().getResource("/br/com/matheus/stockcontrol/ui/party_form.fxml");
            if (url == null) throw new IllegalStateException("FXML não encontrado: /br/com/matheus/stockcontrol/ui/party_form.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            PartyFormController controller = loader.getController();
            controller.prefillType(partyType);

            Stage stage = new Stage();
            stage.setTitle(partyType == PartyType.CLIENTE ? "Novo Cliente" : "Novo Fornecedor");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isSaved()) {
                refreshPartyForType();
                cbParty.requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
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

            Party party = cbParty.getValue();
            if (party == null || party.getId() == null) {
                throw new IllegalArgumentException("Selecione um " + (type == MovementType.SAIDA ? "cliente" : "fornecedor") + " na lista.");
            }

            for (StockMovementItem it : items) {
                BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                it.setSubtotal(unit.multiply(BigDecimal.valueOf(it.getQuantity())));
            }

            movementDao.createMovement(type, party.getId(), items, txtObservation.getText());

            saved = true;
            closeWindow();
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