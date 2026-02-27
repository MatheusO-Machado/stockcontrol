package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.PartyDao;
import br.com.matheus.stockcontrol.model.Party;
import br.com.matheus.stockcontrol.model.PartyType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class PartiesView extends BorderPane {

    private final PartyDao dao = new PartyDao();
    private final TableView<Party> table = new TableView<>();

    private final ComboBox<PartyType> cbType = new ComboBox<>();
    private final TextField txtSearch = new TextField();

    public PartiesView() {
        setPadding(new Insets(10));

        Button btnNew = new Button("Novo");
        Button btnEdit = new Button("Editar");
        Button btnDelete = new Button("Excluir");

        btnNew.setOnAction(e -> {
            boolean changed = openPartyForm(null);
            if (changed) loadData();
        });

        btnEdit.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um registro para editar.");
                return;
            }
            boolean changed = openPartyForm(selected);
            if (changed) loadData();
        });

        btnDelete.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um registro para excluir.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar exclusão");
            confirm.setHeaderText(null);
            confirm.setContentText("Excluir \"" + selected.getName() + "\" (" + selected.getDocument() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.delete(selected.getId());
                    loadData();
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        ToolBar toolbar = new ToolBar(btnNew, btnEdit, btnDelete);

        cbType.getItems().setAll(PartyType.values());
        cbType.setPromptText("Tipo");

        txtSearch.setPromptText("Buscar por nome ou CPF/CNPJ...");
        txtSearch.setPrefWidth(280);

        Button btnFilter = new Button("Buscar");
        Button btnClear = new Button("Limpar");

        btnFilter.setOnAction(e -> loadData());
        btnClear.setOnAction(e -> {
            cbType.getSelectionModel().clearSelection();
            txtSearch.clear();
            loadData();
        });

        HBox filters = new HBox(10,
                new Label("Tipo:"), cbType,
                txtSearch,
                btnFilter, btnClear
        );
        filters.setPadding(new Insets(10, 0, 10, 0));

        setTop(new javafx.scene.layout.VBox(10, toolbar, filters));
        setCenter(buildTable());

        loadData();
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Party, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(80);

        TableColumn<Party, String> colType = new TableColumn<>("Tipo");
        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getType() == null ? "" : c.getValue().getType().name()
        ));
        colType.setMaxWidth(140);

        TableColumn<Party, String> colName = new TableColumn<>("Nome");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Party, String> colDocType = new TableColumn<>("Doc");
        colDocType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDocumentType() == null ? "" : c.getValue().getDocumentType().name()
        ));
        colDocType.setMaxWidth(80);

        TableColumn<Party, String> colDoc = new TableColumn<>("CPF/CNPJ");
        colDoc.setCellValueFactory(new PropertyValueFactory<>("document"));
        colDoc.setMaxWidth(180);

        TableColumn<Party, String> colPhone = new TableColumn<>("Telefone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setMaxWidth(180);

        table.getColumns().setAll(colId, colType, colName, colDocType, colDoc, colPhone);
        return table;
    }

    private void loadData() {
        PartyType type = cbType.getSelectionModel().getSelectedItem();
        String term = txtSearch.getText();
        table.setItems(FXCollections.observableArrayList(dao.search(type, term, 200)));
    }

    private boolean openPartyForm(Party partyToEdit) {
        try {
            var url = getClass().getResource("/br/com/matheus/stockcontrol/ui/party_form.fxml");
            if (url == null) {
                throw new IllegalStateException("FXML não encontrado: /br/com/matheus/stockcontrol/ui/party_form.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            br.com.matheus.stockcontrol.ui.controller.PartyFormController controller = loader.getController();
            if (partyToEdit != null) controller.setPartyToEdit(partyToEdit);

            Stage stage = new Stage();
            stage.setTitle(partyToEdit == null ? "Nova Pessoa" : "Editar Pessoa");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.isSaved();
            } catch (Exception e) {
                e.printStackTrace(); // <-- importante para ver no console do NetBeans

                String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
                if (e.getCause() != null) {
                    msg += "\nCausa: " + (e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getCause().toString());
                }

                showError("Erro ao abrir formulário", msg);
                return false;
            }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}