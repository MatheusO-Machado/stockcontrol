package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.Product;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public String getNextSku() {
        String sql = """
            SELECT MAX(CAST(substr(sku, 5) AS INTEGER)) AS max_num
            FROM products
            WHERE sku LIKE 'SKU-%'
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int next = 1;
            if (rs.next()) {
                int max = rs.getInt("max_num");
                if (!rs.wasNull()) next = max + 1;
            }

            return String.format("SKU-%03d", next);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar próximo SKU", e);
        }
    }

    public List<Product> findAll() {
        return findAll(false);
    }

    public List<Product> findAll(boolean includeInactive) {
        String sql = """
            SELECT
                p.id,
                p.name,
                p.sku,
                p.category_id,
                c.name AS category_name,
                p.cost_cents,
                p.sale_cents,
                p.quantity,
                p.min_stock,
                p.active
            FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE (? = 1 OR p.active = 1)
            ORDER BY p.name
        """;

        List<Product> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, includeInactive ? 1 : 0);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar produtos", e);
        }
    }

    public List<Product> search(String text, Long categoryId) {
        return search(text, categoryId, false);
    }

    public List<Product> search(String text, Long categoryId, boolean includeInactive) {
        String baseSql = """
            SELECT
                p.id,
                p.name,
                p.sku,
                p.category_id,
                c.name AS category_name,
                p.cost_cents,
                p.sale_cents,
                p.quantity,
                p.min_stock,
                p.active
            FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE 1=1
        """;

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (!includeInactive) {
            where.append(" AND p.active = 1 ");
        }

        boolean hasText = text != null && !text.isBlank();
        if (hasText) {
            where.append(" AND (p.name LIKE ? OR p.sku LIKE ?) ");
            String like = "%" + text.trim() + "%";
            params.add(like);
            params.add(like);
        }

        if (categoryId != null) {
            where.append(" AND p.category_id = ? ");
            params.add(categoryId);
        }

        String sql = baseSql + where + " ORDER BY p.name";

        List<Product> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos", e);
        }
    }

    public void insert(Product p) {
        String sql = """
            INSERT INTO products (name, sku, category_id, cost_cents, sale_cents, quantity, min_stock, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        int costCents = toCents(p.getCostPrice());
        int saleCents = toCents(p.getSalePrice());

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getSku());
            ps.setLong(3, p.getCategoryId());
            ps.setInt(4, costCents);
            ps.setInt(5, saleCents);
            ps.setInt(6, p.getQuantity());
            ps.setInt(7, p.getMinStock());
            ps.setInt(8, p.isActive() ? 1 : 0);

            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") && msg.contains("sku")) {
                throw new IllegalArgumentException("Já existe um produto com esse SKU.");
            }
            throw new RuntimeException("Erro ao inserir produto: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir produto", e);
        }
    }

    public void update(Product p) {
        String sql = """
            UPDATE products
            SET name = ?, category_id = ?, cost_cents = ?, sale_cents = ?, quantity = ?, min_stock = ?, active = ?
            WHERE id = ?
        """;

        int costCents = toCents(p.getCostPrice());
        int saleCents = toCents(p.getSalePrice());

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setLong(2, p.getCategoryId());
            ps.setInt(3, costCents);
            ps.setInt(4, saleCents);
            ps.setInt(5, p.getQuantity());
            ps.setInt(6, p.getMinStock());
            ps.setInt(7, p.isActive() ? 1 : 0);
            ps.setLong(8, p.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Produto não encontrado para atualizar.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar produto", e);
        }
    }

    public void setActive(long id, boolean active) {
        String sql = "UPDATE products SET active = ? WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao alterar status (ativo/inativo) do produto", e);
        }
    }

    public int deleteById(long id) {
        String sql = "DELETE FROM products WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("foreign key")) {
                throw new IllegalArgumentException(
                        "Não é possível excluir este produto porque ele possui movimentações. Use a opção Inativar."
                );
            }
            throw new RuntimeException("Erro ao excluir produto: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir produto", e);
        }
    }

    private Product map(ResultSet rs) throws Exception {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String sku = rs.getString("sku");

        long catId = rs.getLong("category_id");
        String catName = rs.getString("category_name");

        int costCents = rs.getInt("cost_cents");
        int saleCents = rs.getInt("sale_cents");

        int quantity = rs.getInt("quantity");
        int minStock = rs.getInt("min_stock");

        boolean active = rs.getInt("active") == 1;

        BigDecimal costPrice = BigDecimal.valueOf(costCents).movePointLeft(2);
        BigDecimal salePrice = BigDecimal.valueOf(saleCents).movePointLeft(2);

        return new Product(
                id, name, sku,
                catId, catName,
                costPrice, salePrice,
                quantity, minStock,
                active
        );
    }

    private int toCents(BigDecimal value) {
        if (value == null) return 0;
        return value.movePointRight(2).intValueExact();
    }
}