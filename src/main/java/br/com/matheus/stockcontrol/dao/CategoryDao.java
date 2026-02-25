package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> findAll() {
        ensureDefault();

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

    public Category findByName(String name) {
        String sql = "SELECT id, name FROM categories WHERE name = ?";

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

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

    public void ensureDefault() {
        String sql = """
            INSERT INTO categories (name)
            VALUES (?)
            ON CONFLICT(name) DO NOTHING
        """;

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, "Geral");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao garantir categoria padrão", e);
        }
    }
}