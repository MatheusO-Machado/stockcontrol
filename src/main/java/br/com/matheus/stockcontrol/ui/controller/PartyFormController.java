package br.com.matheus.stockcontrol.ui.controller;

import br.com.matheus.stockcontrol.dao.PartyDao;
import br.com.matheus.stockcontrol.model.DocumentType;
import br.com.matheus.stockcontrol.model.Party;
import br.com.matheus.stockcontrol.model.PartyType;
import br.com.matheus.stockcontrol.util.BrDocumentValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PartyFormController {

    @FXML private ComboBox<PartyType> cbType;
    @FXML private TextField txtName;
    @FXML private ComboBox<DocumentType> cbDocumentType;
    @FXML private TextField txtDocument;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    @FXML private CheckBox chkActive;

    @FXML private TextField txtZip;
    @FXML private TextField txtStreet;
    @FXML private TextField txtNumber;
    @FXML private TextField txtDistrict;
    @FXML private TextField txtCity;
    @FXML private TextField txtState;
    @FXML private TextField txtComplement;

    private final PartyDao dao = new PartyDao();

    private Party editing;               // se não for null => editando
    private PartyType forcedType = null; // usado quando abre pelo botão "Novo" em Movimentações
    private boolean saved = false;

    public boolean isSaved() {
        return saved;
    }

    /**
     * Usado quando o form é aberto pela movimentação:
     * - Pré-seleciona CLIENTE ou FORNECEDOR
     * - Trava o combo para evitar cadastro no tipo errado
     */
    public void prefillType(PartyType type) {
        this.forcedType = type;
        if (cbType != null && type != null) {
            cbType.getSelectionModel().select(type);
            cbType.setDisable(true);
        }
    }

    public void setPartyToEdit(Party party) {
        // Ao editar, NÃO forçamos tipo: usuário pode ajustar se quiser
        this.forcedType = null;

        this.editing = dao.findById(party.getId());
        if (this.editing == null) {
            throw new IllegalStateException("Pessoa não encontrada para edição (id=" + party.getId() + ")");
        }

        fillForm(this.editing);

        if (cbType != null) {
            cbType.setDisable(false);
        }
    }

    @FXML
    private void initialize() {
        cbType.getItems().setAll(PartyType.values());
        cbType.getSelectionModel().select(PartyType.CLIENTE);

        cbDocumentType.getItems().setAll(DocumentType.values());
        cbDocumentType.getSelectionModel().select(DocumentType.CPF);

        if (chkActive != null) chkActive.setSelected(true);

        // Ajuste de UX: se mudar tipo doc, limpa documento para evitar confusão
        cbDocumentType.valueProperty().addListener((obs, old, val) -> {
            if (old != null && val != null && old != val) {
                txtDocument.clear();
            }
        });

        // Se o tipo foi forçado antes do initialize (raro), reaplica
        if (forcedType != null) {
            cbType.getSelectionModel().select(forcedType);
            cbType.setDisable(true);
        }
    }

    private void fillForm(Party p) {
        cbType.getSelectionModel().select(p.getType());
        txtName.setText(p.getName());
        cbDocumentType.getSelectionModel().select(p.getDocumentType());
        txtDocument.setText(p.getDocument());
        txtPhone.setText(p.getPhone());
        txtEmail.setText(p.getEmail());

        if (chkActive != null) chkActive.setSelected(p.isActive());

        txtZip.setText(p.getZip());
        txtStreet.setText(p.getStreet());
        txtNumber.setText(p.getNumber());
        txtDistrict.setText(p.getDistrict());
        txtCity.setText(p.getCity());
        txtState.setText(p.getState());
        txtComplement.setText(p.getComplement());
    }

    @FXML
    private void onCancel() {
        saved = false;
        closeWindow();
    }

    @FXML
    private void onSave() {
        try {
            PartyType type = cbType.getValue();
            DocumentType docType = cbDocumentType.getValue();

            String name = safe(txtName.getText());
            String docRaw = safe(txtDocument.getText());
            String docDigits = BrDocumentValidator.onlyDigits(docRaw);

            if (type == null) throw new IllegalArgumentException("Selecione o tipo (Cliente/Fornecedor).");
            if (name == null) throw new IllegalArgumentException("Nome é obrigatório.");

            BrDocumentValidator.validate(docType, docDigits);

            Party p = (editing != null) ? editing : new Party();

            p.setType(type);
            p.setName(name);
            p.setDocumentType(docType);
            p.setDocument(docDigits);

            p.setPhone(safe(txtPhone.getText()));
            p.setEmail(safe(txtEmail.getText()));

            p.setZip(safe(txtZip.getText()));
            p.setStreet(safe(txtStreet.getText()));
            p.setNumber(safe(txtNumber.getText()));
            p.setDistrict(safe(txtDistrict.getText()));
            p.setCity(safe(txtCity.getText()));

            String uf = safe(txtState.getText());
            if (uf != null) uf = uf.toUpperCase();
            p.setState(uf);

            p.setComplement(safe(txtComplement.getText()));

            boolean active = chkActive == null || chkActive.isSelected();
            p.setActive(active);

            if (editing == null) {
                long id = dao.insert(p);
                p.setId(id);
            } else {
                dao.update(p);
            }

            saved = true;
            closeWindow();
        } catch (IllegalArgumentException e) {
            showError("Validação", e.getMessage());
        } catch (Exception e) {
            showError("Erro", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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