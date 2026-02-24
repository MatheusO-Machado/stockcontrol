package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() {
        // temporário: depois trocamos por SQLite
        List<Product> list = new ArrayList<>();
        list.add(new Product(1L, "Mouse", "SKU-001", 10, new BigDecimal("59.90")));
        list.add(new Product(2L, "Teclado", "SKU-002", 5, new BigDecimal("129.90")));
        list.add(new Product(3L, "Monitor", "SKU-003", 2, new BigDecimal("899.00")));
        return list;
    }
}