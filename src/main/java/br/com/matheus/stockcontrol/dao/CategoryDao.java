package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public static final String FALLBACK_NAME = "Outros";

    public record CategoryRow(long id, String name, int productsCount) {}

    public List<Category> findAll() {
        String sql = "SELECT id, name FROM categories ORDER BY name";
        List<Category> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Category(
                        rs.getLong("id"),
                        rs.getString("name")
                ));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar categorias", e);
        }
    }

    public List<CategoryRow> findAllWithProductsCount() {
        String sql = """
            SELECT
              c.id,
              c.name,
              COUNT(p.id) AS products_count
            FROM categories c
            LEFT JOIN products p ON p.category_id = c.id
            GROUP BY c.id, c.name
            ORDER BY c.name
        """;

        List<CategoryRow> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new CategoryRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("products_count")
                ));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar categorias com contagem de produtos", e);
        }
    }

    public Category findById(long id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getLong("id"), rs.getString("name"));
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar categoria por id", e);
        }
    }

    public Category findByName(String name) {
        String sql = "SELECT id, name FROM categories WHERE lower(name) = lower(?)";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, safeTrim(name));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getLong("id"), rs.getString("name"));
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar categoria", e);
        }
    }

    public long insert(String name) {
        String n = safeTrim(name);
        if (n.isEmpty()) throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        if (FALLBACK_NAME.equalsIgnoreCase(n)) {
            throw new IllegalArgumentException("O nome '" + FALLBACK_NAME + "' é reservado.");
        }

        Category existing = findByName(n);
        if (existing != null) throw new IllegalArgumentException("Já existe uma categoria com esse nome.");

        String sql = "INSERT INTO categories (name) VALUES (?)";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, n);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new RuntimeException("Falha ao obter ID gerado da categoria.");
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") || msg.contains("constraint")) {
                throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
            }
            throw new RuntimeException("Erro ao inserir categoria: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir categoria", e);
        }
    }

    public void update(Category c) {
        if (c == null || c.getId() == null) throw new IllegalArgumentException("Categoria inválida.");

        String n = safeTrim(c.getName());
        if (n.isEmpty()) throw new IllegalArgumentException("Nome da categoria é obrigatório.");

        Category current = findById(c.getId());
        if (current == null) throw new IllegalArgumentException("Categoria não encontrada para atualizar.");

        if (FALLBACK_NAME.equalsIgnoreCase(current.getName())) {
            throw new IllegalArgumentException("A categoria '" + FALLBACK_NAME + "' não pode ser renomeada.");
        }
        if (FALLBACK_NAME.equalsIgnoreCase(n)) {
            throw new IllegalArgumentException("O nome '" + FALLBACK_NAME + "' é reservado.");
        }

        Category existing = findByName(n);
        if (existing != null && !existing.getId().equals(c.getId())) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
        }

        String sql = "UPDATE categories SET name = ? WHERE id = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, n);
            ps.setLong(2, c.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) throw new IllegalArgumentException("Categoria não encontrada para atualizar.");
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") || msg.contains("constraint")) {
                throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
            }
            throw new RuntimeException("Erro ao atualizar categoria: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar categoria", e);
        }
    }

    public void deleteById(long id) {
        Category toDelete = findById(id);
        if (toDelete == null) {
            throw new IllegalArgumentException("Categoria não encontrada para excluir.");
        }

        if (FALLBACK_NAME.equalsIgnoreCase(toDelete.getName())) {
            throw new IllegalArgumentException("A categoria '" + FALLBACK_NAME + "' não pode ser excluída.");
        }

        long outrosId = ensureOutros();

        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (var ps = conn.prepareStatement("UPDATE products SET category_id = ? WHERE category_id = ?")) {
                ps.setLong(1, outrosId);
                ps.setLong(2, id);
                ps.executeUpdate();
            }

            try (var ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                ps.setLong(1, id);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new IllegalArgumentException("Categoria não encontrada para excluir.");
            }

            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir categoria", e);
        }
    }

    private long ensureOutros() {
        String insert = """
            INSERT INTO categories (name)
            VALUES (?)
            ON CONFLICT(name) DO NOTHING
        """;

        try (var conn = Database.getConnection()) {
            try (var ps = conn.prepareStatement(insert)) {
                ps.setString(1, FALLBACK_NAME);
                ps.executeUpdate();
            }

            try (var ps = conn.prepareStatement("SELECT id FROM categories WHERE name = ?")) {
                ps.setString(1, FALLBACK_NAME);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong("id");
                }
            }

            throw new RuntimeException("Não foi possível garantir a categoria '" + FALLBACK_NAME + "'.");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao garantir categoria '" + FALLBACK_NAME + "'", e);
        }
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}