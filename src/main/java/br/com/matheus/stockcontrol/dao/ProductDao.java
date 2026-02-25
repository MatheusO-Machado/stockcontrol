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
                p.min_stock
            FROM products p
            JOIN categories c ON c.id = p.category_id
            ORDER BY p.name
        """;

        return queryList(sql, null);
    }

    public List<Product> search(String text, Long categoryId) {
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
                p.min_stock
            FROM products p
            JOIN categories c ON c.id = p.category_id
        """;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        boolean hasText = text != null && !text.isBlank();

        if (hasText) {
            where.append(" AND (p.name LIKE ? OR p.sku LIKE ?) ");
        }
        if (categoryId != null) {
            where.append(" AND p.category_id = ? ");
        }

        String sql = baseSql + where + " ORDER BY p.name";

        List<Product> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;

            if (hasText) {
                String like = "%" + text.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (categoryId != null) {
                ps.setLong(idx++, categoryId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    String sku = rs.getString("sku");

                    long catId = rs.getLong("category_id");
                    String catName = rs.getString("category_name");

                    int costCents = rs.getInt("cost_cents");
                    int saleCents = rs.getInt("sale_cents");
                    int quantity = rs.getInt("quantity");
                    int minStock = rs.getInt("min_stock");

                    BigDecimal costPrice = BigDecimal.valueOf(costCents).movePointLeft(2);
                    BigDecimal salePrice = BigDecimal.valueOf(saleCents).movePointLeft(2);

                    list.add(new Product(
                            id, name, sku,
                            catId, catName,
                            costPrice, salePrice,
                            quantity, minStock
                    ));
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos", e);
        }
    }

    private List<Product> queryList(String sql, Object params) {
        List<Product> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (params == null) {
                // nada
            } else if (params instanceof ParamsSearch p) {
                // (não usado)
            } else if (params.getClass().getSimpleName().equals("Params")) {
                // hack simples para manter compatível com Java sem expor record fora
                // vamos setar por reflexão? não. Então: vamos usar método próprio.
            }

            // Para evitar complicação, vamos detectar e setar manualmente se for search:
            if (params != null) {
                // params é record local Params(text, categoryId)
                var text = (String) params.getClass().getMethod("text").invoke(params);
                var categoryId = (Long) params.getClass().getMethod("categoryId").invoke(params);

                String like = (text == null || text.isBlank()) ? null : ("%" + text.trim() + "%");

                // ( ? IS NULL OR ? = '' OR p.name LIKE ? OR p.sku LIKE ? )
                ps.setString(1, text);
                ps.setString(2, text);
                ps.setString(3, like);
                ps.setString(4, like);

                // ( ? IS NULL OR p.category_id = ? )
                if (categoryId == null) {
                    ps.setObject(5, null);
                    ps.setObject(6, null);
                } else {
                    ps.setLong(5, categoryId);
                    ps.setLong(6, categoryId);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    String sku = rs.getString("sku");

                    long catId = rs.getLong("category_id");
                    String catName = rs.getString("category_name");

                    int costCents = rs.getInt("cost_cents");
                    int saleCents = rs.getInt("sale_cents");

                    int quantity = rs.getInt("quantity");
                    int minStock = rs.getInt("min_stock");

                    BigDecimal costPrice = BigDecimal.valueOf(costCents).movePointLeft(2);
                    BigDecimal salePrice = BigDecimal.valueOf(saleCents).movePointLeft(2);

                    list.add(new Product(
                            id, name, sku,
                            catId, catName,
                            costPrice, salePrice,
                            quantity, minStock
                    ));
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos", e);
        }
    }

    // Insert V2
    public void insert(Product p) {
        String sql = """
            INSERT INTO products (name, sku, category_id, cost_cents, sale_cents, quantity, min_stock)
            VALUES (?, ?, ?, ?, ?, ?, ?)
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
            SET name = ?, category_id = ?, cost_cents = ?, sale_cents = ?, quantity = ?, min_stock = ?
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
            ps.setLong(7, p.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Produto não encontrado para atualizar.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar produto", e);
        }
    }

    public int deleteById(long id) {
        String sql = "DELETE FROM products WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir produto", e);
        }
    }

    private int toCents(BigDecimal value) {
        if (value == null) return 0;
        return value.movePointRight(2).intValueExact();
    }

    // Apenas para evitar warning do compilador por causa do record local acima
    private interface ParamsSearch {}
}