package br.com.matheus.stockcontrol.model;

import java.math.BigDecimal;

public class Product {
    private Long id;
    private String name;
    private String sku;
    private int quantity;
    private BigDecimal price;

    public Product() {}

    public Product(Long id, String name, String sku, int quantity, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}