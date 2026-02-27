package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.DocumentType;
import br.com.matheus.stockcontrol.model.Party;
import br.com.matheus.stockcontrol.model.PartyType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static br.com.matheus.stockcontrol.util.BrDocumentValidator.onlyDigits;

public class PartyDao {

    public List<Party> search(PartyType type, String term, int limit) {
        String t = term == null ? "" : term.trim();
        String digits = onlyDigits(t);

        StringBuilder sql = new StringBuilder("""
            SELECT id, type, document_type, document, name, phone, email,
                   zip, street, number, district, city, state, complement
            FROM parties
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ? ");
            params.add(type.name());
        }

        if (!t.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR document LIKE ?) ");
            params.add("%" + t.toLowerCase() + "%");
            params.add("%" + (digits == null ? "" : digits) + "%");
        }

        sql.append(" ORDER BY name LIMIT ? ");
        params.add(limit <= 0 ? 50 : limit);

        List<Party> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar pessoas", e);
        }
    }

    public Party findById(long id) {
        String sql = """
            SELECT id, type, document_type, document, name, phone, email,
                   zip, street, number, district, city, state, complement
            FROM parties
            WHERE id = ?
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar pessoa por id", e);
        }
    }

    public long insert(Party p) {
        String sql = """
            INSERT INTO parties
            (type, document_type, document, name, phone, email,
             zip, street, number, district, city, state, complement)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            bind(ps, p);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("Falha ao obter id.");
                return keys.getLong(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir pessoa", e);
        }
    }

    public void update(Party p) {
        String sql = """
            UPDATE parties SET
              type = ?,
              document_type = ?,
              document = ?,
              name = ?,
              phone = ?,
              email = ?,
              zip = ?,
              street = ?,
              number = ?,
              district = ?,
              city = ?,
              state = ?,
              complement = ?
            WHERE id = ?
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bind(ps, p);
            ps.setLong(14, p.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar pessoa", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM parties WHERE id = ?";

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir pessoa", e);
        }
    }

    private Party map(ResultSet rs) throws Exception {
        Party p = new Party();
        p.setId(rs.getLong("id"));
        p.setType(PartyType.valueOf(rs.getString("type")));
        p.setDocumentType(DocumentType.valueOf(rs.getString("document_type")));
        p.setDocument(rs.getString("document"));
        p.setName(rs.getString("name"));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setZip(rs.getString("zip"));
        p.setStreet(rs.getString("street"));
        p.setNumber(rs.getString("number"));
        p.setDistrict(rs.getString("district"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setComplement(rs.getString("complement"));
        return p;
    }

    private void bind(PreparedStatement ps, Party p) throws Exception {
        ps.setString(1, p.getType().name());
        ps.setString(2, p.getDocumentType().name());
        ps.setString(3, onlyDigits(p.getDocument()));
        ps.setString(4, p.getName());
        ps.setString(5, emptyToNull(p.getPhone()));
        ps.setString(6, emptyToNull(p.getEmail()));
        ps.setString(7, emptyToNull(p.getZip()));
        ps.setString(8, emptyToNull(p.getStreet()));
        ps.setString(9, emptyToNull(p.getNumber()));
        ps.setString(10, emptyToNull(p.getDistrict()));
        ps.setString(11, emptyToNull(p.getCity()));
        ps.setString(12, emptyToNull(p.getState()));
        ps.setString(13, emptyToNull(p.getComplement()));
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}