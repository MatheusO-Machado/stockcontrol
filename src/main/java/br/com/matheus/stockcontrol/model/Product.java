package br.com.matheus.stockcontrol.model;

import java.math.BigDecimal;

public class Product {
    private Long id;
    private String name;
    private String sku;

    private Long categoryId;
    private String categoryName;

    private BigDecimal costPrice;   // precoCusto
    private BigDecimal salePrice;   // precoVenda

    private int quantity;
    private int minStock;           // estoqueMinimo

    private boolean active = true;

    public Product() {}

    public Product(
            Long id,
            String name,
            String sku,
            Long categoryId,
            String categoryName,
            BigDecimal costPrice,
            BigDecimal salePrice,
            int quantity,
            int minStock
    ) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.costPrice = costPrice;
        this.salePrice = salePrice;
        this.quantity = quantity;
        this.minStock = minStock;
        this.active = true;
    }

    // Construtor com active (usado pelo DAO)
    public Product(
            Long id,
            String name,
            String sku,
            Long categoryId,
            String categoryName,
            BigDecimal costPrice,
            BigDecimal salePrice,
            int quantity,
            int minStock,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.costPrice = costPrice;
        this.salePrice = salePrice;
        this.quantity = quantity;
        this.minStock = minStock;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        String n = name == null ? "" : name;
        String s = sku == null ? "" : sku;
        return n + " (" + s + ")";
    }
}