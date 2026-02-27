package br.com.matheus.stockcontrol.model;

public class Party {
    private Long id;
    private PartyType type;

    private DocumentType documentType;
    private String document; // apenas dígitos
    private String name;

    private String phone;
    private String email;

    // Endereço (estruturado)
    private String zip;
    private String street;
    private String number;
    private String district;
    private String city;
    private String state;
    private String complement;

    private boolean active = true;

    public Party() {}

    public Party(Long id, PartyType type, DocumentType documentType, String document, String name) {
        this.id = id;
        this.type = type;
        this.documentType = documentType;
        this.document = document;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PartyType getType() { return type; }
    public void setType(PartyType type) { this.type = type; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getComplement() { return complement; }
    public void setComplement(String complement) { this.complement = complement; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        String doc = document == null ? "" : document;
        String n = name == null ? "" : name;
        return n + " - " + doc;
    }
}