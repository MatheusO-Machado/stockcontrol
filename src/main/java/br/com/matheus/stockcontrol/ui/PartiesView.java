package br.com.matheus.stockcontrol.ui;

import br.com.matheus.stockcontrol.dao.PartyDao;
import br.com.matheus.stockcontrol.model.Party;
import br.com.matheus.stockcontrol.model.PartyType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class PartiesView extends BorderPane {

    private final PartyDao dao = new PartyDao();
    private final TableView<Party> table = new TableView<>();

    private final ComboBox<PartyType> cbType = new ComboBox<>();
    private final TextField txtSearch = new TextField();
    private final CheckBox chkShowInactive = new CheckBox("Mostrar inativos");

    public PartiesView() {
        setPadding(new Insets(0));

        setTop(buildTop());
        setCenter(buildTable());

        loadData();
    }

    private Parent buildTop() {
        Button btnNew = new Button("Novo");
        Button btnEdit = new Button("Editar");

        // ações de status
        Button btnInactivate = new Button("Inativar");
        Button btnReactivate = new Button("Reativar");

        // manter excluir (só vai funcionar se não houver movimentações)
        Button btnDelete = new Button("Excluir");

        // Padrão global de cores
        btnNew.getStyleClass().add("btn-new");
        btnEdit.getStyleClass().add("btn-edit");
        btnDelete.getStyleClass().add("btn-delete");

        // Botões "status" podem ficar padrão (brancos), ou você pode criar classes depois
        btnInactivate.getStyleClass().add("ghost");
        btnReactivate.getStyleClass().add("ghost");

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

        btnInactivate.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um registro para inativar.");
                return;
            }
            if (!selected.isActive()) {
                showInfo("Status", "Este cadastro já está inativo.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar inativação");
            confirm.setHeaderText(null);
            confirm.setContentText("Inativar \"" + selected.getName() + "\" (" + selected.getDocument() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.setActive(selected.getId(), false);
                    loadData();
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        btnReactivate.setOnAction(e -> {
            Party selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Seleção", "Selecione um registro para reativar.");
                return;
            }
            if (selected.isActive()) {
                showInfo("Status", "Este cadastro já está ativo.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar reativação");
            confirm.setHeaderText(null);
            confirm.setContentText("Reativar \"" + selected.getName() + "\" (" + selected.getDocument() + ")?");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    dao.setActive(selected.getId(), true);
                    loadData();
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
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
                } catch (IllegalArgumentException ex) {
                    // Ex: FK -> tem movimentações
                    // Oferece inativar na hora:
                    Alert suggest = new Alert(Alert.AlertType.CONFIRMATION);
                    suggest.setTitle("Não é possível excluir");
                    suggest.setHeaderText(null);
                    suggest.setContentText(
                            (ex.getMessage() != null ? ex.getMessage() : ex.toString()) +
                            "\n\nDeseja inativar este cadastro?"
                    );

                    ButtonType btnYes = new ButtonType("Inativar", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                    suggest.getButtonTypes().setAll(btnYes, btnNo);

                    var r2 = suggest.showAndWait();
                    if (r2.isPresent() && r2.get() == btnYes) {
                        try {
                            dao.setActive(selected.getId(), false);
                            loadData();
                        } catch (Exception ex2) {
                            showError("Erro", ex2.getMessage() != null ? ex2.getMessage() : ex2.toString());
                        }
                    }
                } catch (Exception ex) {
                    showError("Erro", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }
        });

        // Habilitar/desabilitar botões conforme seleção
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean has = sel != null;
            btnEdit.setDisable(!has);
            btnDelete.setDisable(!has);
            btnInactivate.setDisable(!has || !sel.isActive());
            btnReactivate.setDisable(!has || sel.isActive());
        });

        // estado inicial
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnInactivate.setDisable(true);
        btnReactivate.setDisable(true);

        ToolBar toolbar = new ToolBar(btnNew, btnEdit, new Separator(), btnInactivate, btnReactivate, new Separator(), btnDelete);

        cbType.getItems().setAll(PartyType.values());
        cbType.setPromptText("Tipo");
        cbType.setPrefWidth(160);

        txtSearch.setPromptText("Buscar por nome ou CPF/CNPJ...");
        txtSearch.setPrefWidth(320);

        chkShowInactive.setOnAction(e -> loadData());

        Button btnFilter = new Button("Buscar");
        Button btnClear = new Button("Limpar");

        btnFilter.setOnAction(e -> loadData());
        btnClear.setOnAction(e -> {
            cbType.getSelectionModel().clearSelection();
            txtSearch.clear();
            chkShowInactive.setSelected(false);
            loadData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox filters = new HBox(10,
                new Label("Tipo:"), cbType,
                new Label("Busca:"), txtSearch,
                chkShowInactive,
                spacer,
                btnFilter, btnClear
        );
        filters.getStyleClass().add("filters-bar");

        var header = new javafx.scene.layout.VBox(10, toolbar, filters);
        header.getStyleClass().add("view-header");
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        return header;
    }

    private Parent buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Party, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(80);

        TableColumn<Party, String> colActive = new TableColumn<>("Status");
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Ativo" : "Inativo"));
        colActive.setMaxWidth(110);

        TableColumn<Party, String> colType = new TableColumn<>("Tipo");
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getType() == null ? "" : c.getValue().getType().name()
        ));
        colType.setMaxWidth(140);

        TableColumn<Party, String> colName = new TableColumn<>("Nome");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Party, String> colDocType = new TableColumn<>("Doc");
        colDocType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDocumentType() == null ? "" : c.getValue().getDocumentType().name()
        ));
        colDocType.setMaxWidth(80);

        TableColumn<Party, String> colDoc = new TableColumn<>("CPF/CNPJ");
        colDoc.setCellValueFactory(new PropertyValueFactory<>("document"));
        colDoc.setMaxWidth(180);

        TableColumn<Party, String> colPhone = new TableColumn<>("Telefone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setMaxWidth(180);

        table.getColumns().setAll(colId, colActive, colType, colName, colDocType, colDoc, colPhone);
        return table;
    }

    private void loadData() {
        PartyType type = cbType.getSelectionModel().getSelectedItem();
        String term = txtSearch.getText();
        boolean includeInactive = chkShowInactive.isSelected();
        table.setItems(FXCollections.observableArrayList(dao.search(type, term, 200, includeInactive)));
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
            e.printStackTrace();

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

    public void refresh() {
        loadData();
    }
}