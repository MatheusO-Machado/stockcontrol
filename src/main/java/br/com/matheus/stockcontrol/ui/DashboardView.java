package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.DashboardDao;
import br.com.matheus.stockcontrol.model.MovementType;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardView extends BorderPane {

    private final DashboardDao dao = new DashboardDao();

    private final DatePicker dpStart = new DatePicker();
    private final DatePicker dpEnd = new DatePicker();

    // KPI labels
    private final Label lblEntradas = new Label("R$ 0,00");
    private final Label lblSaidas = new Label("R$ 0,00");
    private final Label lblLucro = new Label("R$ 0,00");
    private final Label lblMovs = new Label("0");

    private final BarChart<String, Number> chart;
    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();

    private final TableView<DashboardDao.TopProductRow> tableTopSaidas = new TableView<>();
    private final TableView<DashboardDao.LowStockRow> tableLowStock = new TableView<>();

    private final TitledPane tpTop = new TitledPane();
    private final TitledPane tpLow = new TitledPane();

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final DateTimeFormatter brDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Mesmas pseudo-classes do ProductsView + app.css
    private static final PseudoClass PC_STOCK_ZERO = PseudoClass.getPseudoClass("stock-zero");
    private static final PseudoClass PC_STOCK_LOW = PseudoClass.getPseudoClass("stock-low");

    public DashboardView() {
        setPadding(new Insets(0));

        // Período padrão: mês atual
        YearMonth ym = YearMonth.now();
        dpStart.setValue(ym.atDay(1));
        dpEnd.setValue(ym.atEndOfMonth());

        setTop(buildTop());

        // Chart
        xAxis.setLabel("Mês");
        yAxis.setLabel("R$");

        chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setPrefHeight(280);

        // KPI cards
        GridPane kpis = new GridPane();
        kpis.setHgap(12);
        kpis.setVgap(12);
        kpis.setPadding(new Insets(10, 0, 10, 0));

        kpis.add(kpiCard("Entradas", lblEntradas), 0, 0);
        kpis.add(kpiCard("Saídas", lblSaidas), 1, 0);
        kpis.add(kpiCard("Lucro bruto (estim.)", lblLucro), 2, 0);
        kpis.add(kpiCard("Movimentações", lblMovs), 3, 0);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setFillWidth(true);
        kpis.getColumnConstraints().addAll(cc, cc, cc, cc);

        VBox centerTop = new VBox(12, kpis, chart);

        // Tables bottom
        buildTopSaidasTable();
        buildLowStockTable();

        tpTop.setText("Top 5 produtos (Saídas por quantidade)");
        tpTop.setContent(tableTopSaidas);
        tpTop.setCollapsible(false);

        tpLow.setText("Alertas de estoque (min_stock configurado)");
        tpLow.setContent(tableLowStock);
        tpLow.setCollapsible(false);

        HBox bottom = new HBox(12, tpTop, tpLow);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(tpTop, Priority.ALWAYS);
        HBox.setHgrow(tpLow, Priority.ALWAYS);
        tpTop.setMaxWidth(Double.MAX_VALUE);
        tpLow.setMaxWidth(Double.MAX_VALUE);

        setCenter(new VBox(12, centerTop, bottom));

        refresh();
    }

    private Parent buildTop() {
        // Você pediu "Aplicar" branco. Então não adiciono classes aqui.
        Button btnApply = new Button("Aplicar");
        btnApply.setOnAction(e -> refresh());

        Button btnThisMonth = new Button("Mês atual");
        btnThisMonth.setOnAction(e -> {
            YearMonth now = YearMonth.now();
            dpStart.setValue(now.atDay(1));
            dpEnd.setValue(now.atEndOfMonth());
            refresh();
        });

        Button btnLast90 = new Button("Últimos 90 dias");
        btnLast90.setOnAction(e -> {
            dpEnd.setValue(LocalDate.now());
            dpStart.setValue(LocalDate.now().minusDays(89));
            refresh();
        });

        Button btnLast6Months = new Button("Últimos 6 meses");
        btnLast6Months.setOnAction(e -> {
            YearMonth now = YearMonth.now();
            YearMonth startYm = now.minusMonths(5);
            dpStart.setValue(startYm.atDay(1));
            dpEnd.setValue(now.atEndOfMonth());
            refresh();
        });

        HBox filters = new HBox(10,
                new Label("Período:"),
                new Label("Início:"), dpStart,
                new Label("Fim:"), dpEnd,
                btnApply,
                btnThisMonth,
                btnLast90,
                btnLast6Months
        );
        filters.getStyleClass().add("filters-bar");

        var header = new VBox(10, filters);
        header.getStyleClass().add("view-header");
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        return header;
    }

    public void refresh() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();

        if (start == null || end == null) {
            showError("Período inválido", "Selecione início e fim.");
            return;
        }
        if (end.isBefore(start)) {
            showError("Período inválido", "A data final não pode ser menor que a inicial.");
            return;
        }

        DashboardDao.Kpis kpis = dao.getKpis(start, end);
        lblEntradas.setText(currency.format(kpis.entradas()));
        lblSaidas.setText(currency.format(kpis.saidas()));
        lblLucro.setText(currency.format(kpis.lucroBrutoEstimado()));
        lblMovs.setText(String.valueOf(kpis.movimentos()));

        chart.setTitle("Entradas x Saídas (" + brDate.format(start) + " a " + brDate.format(end) + ")");

        chart.getData().clear();
        var months = dao.getEntradasSaidasByMonth(start, end);

        XYChart.Series<String, Number> sEntradas = new XYChart.Series<>();
        sEntradas.setName("Entradas");

        XYChart.Series<String, Number> sSaidas = new XYChart.Series<>();
        sSaidas.setName("Saídas");

        for (var row : months) {
            String label = row.month().getMonthValue() + "/" + row.month().getYear();
            sEntradas.getData().add(new XYChart.Data<>(label, row.entradas().doubleValue()));
            sSaidas.getData().add(new XYChart.Data<>(label, row.saidas().doubleValue()));
        }

        chart.getData().addAll(sEntradas, sSaidas);

        var top = dao.getTopProductsByQuantity(MovementType.SAIDA, start, end, 5);
        tableTopSaidas.setItems(FXCollections.observableArrayList(top));
        tpTop.setText("Top 5 produtos (Saídas por quantidade) — " + top.size() + " resultado(s)");

        var low = dao.getLowStock(15);
        tableLowStock.setItems(FXCollections.observableArrayList(low));
        tpLow.setText("Alertas de estoque (min_stock configurado) — " + low.size() + " alerta(s)");
    }

    private Region kpiCard(String title, Label valueLabel) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #6B7280;");

        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900;");

        VBox box = new VBox(6, t, valueLabel);
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinHeight(78);
        box.setMaxWidth(Double.MAX_VALUE);

        box.getStyleClass().add("content-card");
        return box;
    }

    private void buildTopSaidasTable() {
        tableTopSaidas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DashboardDao.TopProductRow, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sku()));
        colSku.setMaxWidth(120);

        TableColumn<DashboardDao.TopProductRow, String> colName = new TableColumn<>("Produto");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<DashboardDao.TopProductRow, Number> colQty = new TableColumn<>("Qtd");
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().qty()));
        colQty.setMaxWidth(90);

        tableTopSaidas.getColumns().setAll(colSku, colName, colQty);
        tableTopSaidas.setPrefHeight(220);
    }

    private void buildLowStockTable() {
        tableLowStock.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Destaque por pseudo-class (igual Produtos)
        tableLowStock.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DashboardDao.LowStockRow item, boolean empty) {
                super.updateItem(item, empty);

                boolean stockZero = false;
                boolean stockLow = false;

                if (!empty && item != null) {
                    stockZero = item.quantity() <= 0;
                    stockLow = !stockZero && item.minStock() > 0 && item.quantity() <= item.minStock();
                }

                pseudoClassStateChanged(PC_STOCK_ZERO, stockZero);
                pseudoClassStateChanged(PC_STOCK_LOW, stockLow);
            }
        });

        TableColumn<DashboardDao.LowStockRow, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sku()));
        colSku.setMaxWidth(120);

        TableColumn<DashboardDao.LowStockRow, String> colName = new TableColumn<>("Produto");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        TableColumn<DashboardDao.LowStockRow, Number> colQty = new TableColumn<>("Qtd");
        colQty.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().quantity()));
        colQty.setMaxWidth(80);

        TableColumn<DashboardDao.LowStockRow, Number> colMin = new TableColumn<>("Mín");
        colMin.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().minStock()));
        colMin.setMaxWidth(80);

        tableLowStock.getColumns().setAll(colSku, colName, colQty, colMin);
        tableLowStock.setPrefHeight(220);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}