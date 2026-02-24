package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.Product;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() {
        String sql = "SELECT id, name, sku, quantity, price_cents FROM products ORDER BY name";

        List<Product> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String sku = rs.getString("sku");
                int quantity = rs.getInt("quantity");
                int priceCents = rs.getInt("price_cents");

                BigDecimal price = BigDecimal.valueOf(priceCents).movePointLeft(2);

                list.add(new Product(id, name, sku, quantity, price));
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos", e);
        }
    }
}