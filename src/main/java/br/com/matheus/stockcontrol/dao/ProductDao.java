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
    
    public void insert(Product p) {
        String sql = """
            INSERT INTO products (name, sku, quantity, price_cents)
            VALUES (?, ?, ?, ?)
        """;

        int priceCents = p.getPrice()
                .movePointRight(2)
                .intValueExact();

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getQuantity());
            ps.setInt(4, priceCents);

            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            // mensagem melhor quando for SKU duplicado
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") && msg.contains("sku")) {
                throw new IllegalArgumentException("Já existe um produto com esse SKU.");
            }
            throw new RuntimeException("Erro ao inserir produto: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir produto", e);
        }
    }
    
    public int deleteById(long id) {
        String sql = "DELETE FROM products WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate(); // retorna 0 se não deletou nada
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir produto", e);
        }
    }
    
    public void update(Product p) {
        String sql = """
            UPDATE products
            SET name = ?, sku = ?, quantity = ?, price_cents = ?
            WHERE id = ?
        """;

        int priceCents = p.getPrice()
                .movePointRight(2)
                .intValueExact();

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setInt(3, p.getQuantity());
            ps.setInt(4, priceCents);
            ps.setLong(5, p.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Produto não encontrado para atualizar.");
            }
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") && msg.contains("sku")) {
                throw new IllegalArgumentException("Já existe um produto com esse SKU.");
            }
            throw new RuntimeException("Erro ao atualizar produto: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar produto", e);
        }
    }
}
