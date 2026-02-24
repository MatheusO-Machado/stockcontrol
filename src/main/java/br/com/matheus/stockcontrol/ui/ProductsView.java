package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.ProductDao;
import br.com.matheus.stockcontrol.model.Product;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;

public class ProductsView extends BorderPane {

    private final TableView<Product> table = new TableView<>();
    private final ProductDao dao = new ProductDao();

    public ProductsView() {
        setPadding(new Insets(10));

        var btnNew = new Button("Novo");
        var btnEdit = new Button("Editar");
        var btnDelete = new Button("Excluir");

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

        // Eventos (por enquanto só para mostrar que está ligado)
        btnNew.setOnAction(e -> System.out.println("Novo (em breve abre formulário)"));
        btnEdit.setOnAction(e -> System.out.println("Editar (em breve)"));
        btnDelete.setOnAction(e -> System.out.println("Excluir (em breve)"));
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.findAll()));
    }
}