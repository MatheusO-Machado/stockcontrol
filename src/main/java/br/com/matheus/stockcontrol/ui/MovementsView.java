package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.StockMovementDao;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.model.StockMovement;
import br.com.matheus.stockcontrol.model.StockMovementItem;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MovementsView extends BorderPane {

    private final StockMovementDao dao = new StockMovementDao();
    private final TableView<StockMovement> table = new TableView<>();
    private final Runnable onStockChanged;

    private final ComboBox<MovementType> cbType = new ComboBox<>();
    private final DatePicker dpStart = new DatePicker();
    private final DatePicker dpEnd = new DatePicker();
    private final Button btnFilter = new Button("Filtrar");
    private final Button btnClear = new Button("Limpar");

    public MovementsView(Runnable onStockChanged) {
        this.onStockChanged = onStockChanged;

        setPadding(new Insets(10));

        Button btnNew = new Button("Nova movimentação");
        btnNew.setOnAction(e -> openMovementAndRefresh());

        cbType.getItems().setAll(MovementType.values());
        cbType.setPromptText("Tipo");

        btnFilter.setOnAction(e -> loadData());
        btnClear.setOnAction(e -> {
            cbType.getSelectionModel().clearSelection();
            dpStart.setValue(null);
            dpEnd.setValue(null);
            loadData();
        });

        HBox filters = new HBox(10,
                new Label("Tipo:"), cbType,
                new Label("Início:"), dpStart,
                new Label("Fim:"), dpEnd,
                btnFilter, btnClear
        );
        filters.setPadding(new Insets(10, 0, 10, 0));

        setTop(new javafx.scene.layout.VBox(10, new ToolBar(btnNew), filters));
        setCenter(buildTable());

        loadData();
    }

    private void openMovementAndRefresh() {
        boolean saved = openMovementForm();
        if (saved) {
            loadData();
            if (onStockChanged != null) onStockChanged.run();
        }
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StockMovement, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(80);

        TableColumn<StockMovement, LocalDateTime> colDate = new TableColumn<>("Data/Hora");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        colDate.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            @Override protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(fmt));
            }
        });

        TableColumn<StockMovement, String> colType = new TableColumn<>("Tipo");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setMaxWidth(120);

        TableColumn<StockMovement, String> colParty = new TableColumn<>("Pessoa");
        colParty.setCellValueFactory(new PropertyValueFactory<>("partyName"));

        TableColumn<StockMovement, String> colDoc = new TableColumn<>("Documento");
        colDoc.setCellValueFactory(new PropertyValueFactory<>("partyDocument"));
        colDoc.setMaxWidth(180);

        TableColumn<StockMovement, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(tc -> moneyCell());
        colTotal.setMaxWidth(140);

        TableColumn<StockMovement, String> colObs = new TableColumn<>("Obs.");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observation"));

        table.getColumns().setAll(colId, colDate, colType, colParty, colDoc, colTotal, colObs);

        table.setRowFactory(tv -> {
            TableRow<StockMovement> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showDetails(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    private TableCell<StockMovement, BigDecimal> moneyCell() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : nf.format(value));
            }
        };
    }

    private void loadData() {
        MovementType type = cbType.getSelectionModel().getSelectedItem();
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        table.setItems(FXCollections.observableArrayList(dao.listMovements(type, start, end)));
    }

    private void showDetails(StockMovement m) {
        var items = dao.listItemsByMovementId(m.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Movimentação #").append(m.getId()).append(" - ").append(m.getType()).append("\n");
        sb.append("Pessoa: ").append(m.getPartyName()).append(" - ").append(m.getPartyDocument()).append("\n\n");

        for (StockMovementItem it : items) {
            sb.append("- ")
              .append(it.getProductName()).append(" (").append(it.getProductSku()).append(")")
              .append(" | Qtd: ").append(it.getQuantity())
              .append(" | Unit: ").append(it.getUnitPrice())
              .append(" | Sub: ").append(it.getSubtotal())
              .append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalhes da movimentação");
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private boolean openMovementForm() {
        try {
            var url = getClass().getResource("/br/com/matheus/stockcontrol/ui/movement_form.fxml");
            if (url == null) {
                throw new IllegalStateException("FXML não encontrado: /br/com/matheus/stockcontrol/ui/movement_form.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            br.com.matheus.stockcontrol.ui.controller.MovementFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Nova movimentação");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.isSaved();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao abrir formulário de movimentação", e);
        }
    }
}