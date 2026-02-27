package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.model.PartyType;
import br.com.matheus.stockcontrol.model.StockMovement;
import br.com.matheus.stockcontrol.model.StockMovementItem;
import br.com.matheus.stockcontrol.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class StockMovementDao {

    public List<StockMovement> listMovements(MovementType type, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("""
            SELECT
              m.id,
              m.type,
              m.datetime,
              m.party_id,
              p.name AS party_name,
              p.document AS party_document,
              m.total_cents,
              m.observation
            FROM stock_movements m
            JOIN parties p ON p.id = m.party_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND m.type = ? ");
            params.add(type.name());
        }

        if (start != null) {
            sql.append(" AND m.datetime >= ? ");
            params.add(start.atStartOfDay().toString());
        }
        if (end != null) {
            sql.append(" AND m.datetime <= ? ");
            params.add(end.atTime(23, 59, 59).toString());
        }

        sql.append(" ORDER BY m.datetime DESC LIMIT 500");

        List<StockMovement> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = new StockMovement(
                            rs.getLong("id"),
                            MovementType.valueOf(rs.getString("type")),
                            LocalDateTime.parse(rs.getString("datetime")),
                            MoneyUtil.fromCents(rs.getInt("total_cents")),
                            rs.getString("observation")
                    );
                    m.setPartyId(rs.getLong("party_id"));
                    m.setPartyName(rs.getString("party_name"));
                    m.setPartyDocument(rs.getString("party_document"));
                    list.add(m);
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar movimentações", e);
        }
    }

    public List<StockMovementItem> listItemsByMovementId(long movementId) {
        String sql = """
            SELECT
                i.id,
                i.product_id,
                p.name AS product_name,
                p.sku AS product_sku,
                i.quantity,
                i.unit_cents,
                i.subtotal_cents
            FROM stock_movement_items i
            JOIN products p ON p.id = i.product_id
            WHERE i.movement_id = ?
            ORDER BY p.name
        """;

        List<StockMovementItem> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, movementId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal unit = MoneyUtil.fromCents(rs.getInt("unit_cents"));
                    BigDecimal subtotal = MoneyUtil.fromCents(rs.getInt("subtotal_cents"));

                    list.add(new StockMovementItem(
                            rs.getLong("id"),
                            rs.getLong("product_id"),
                            rs.getString("product_name"),
                            rs.getString("product_sku"),
                            rs.getInt("quantity"),
                            unit,
                            subtotal
                    ));
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar itens da movimentação", e);
        }
    }

    public void createMovement(MovementType type, long partyId, List<StockMovementItem> items, String observation) {
        if (type == null) throw new IllegalArgumentException("Tipo é obrigatório.");
        if (partyId <= 0) throw new IllegalArgumentException("Selecione um cliente/fornecedor.");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um item.");

        // Normaliza itens por produto (soma qty se repetido)
        Map<Long, StockMovementItem> byProduct = new LinkedHashMap<>();
        for (StockMovementItem it : items) {
            if (it.getProductId() == null) throw new IllegalArgumentException("Item sem produto.");
            if (it.getQuantity() <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");

            BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            if (unit.signum() < 0) throw new IllegalArgumentException("Valor unitário não pode ser negativo.");

            if (byProduct.containsKey(it.getProductId())) {
                StockMovementItem acc = byProduct.get(it.getProductId());
                acc.setQuantity(acc.getQuantity() + it.getQuantity());
                acc.setUnitPrice(unit);
            } else {
                StockMovementItem copy = new StockMovementItem();
                copy.setProductId(it.getProductId());
                copy.setQuantity(it.getQuantity());
                copy.setUnitPrice(unit);
                byProduct.put(it.getProductId(), copy);
            }
        }

        List<StockMovementItem> normalized = new ArrayList<>(byProduct.values());

        // Calcula subtotal e total (ENTRADA e SAIDA têm valor financeiro)
        int totalCents = 0;
        for (StockMovementItem it : normalized) {
            int unitCents = MoneyUtil.toCents(it.getUnitPrice());
            int subtotalCents = Math.multiplyExact(unitCents, it.getQuantity());
            it.setSubtotal(MoneyUtil.fromCents(subtotalCents));
            totalCents = Math.addExact(totalCents, subtotalCents);
        }

        String insertMove = """
            INSERT INTO stock_movements (type, datetime, party_id, total_cents, observation)
            VALUES (?, ?, ?, ?, ?)
        """;

        String insertItem = """
            INSERT INTO stock_movement_items (movement_id, product_id, quantity, unit_cents, subtotal_cents)
            VALUES (?, ?, ?, ?, ?)
        """;

        String selectQty = "SELECT quantity FROM products WHERE id = ?";
        String updateQty = "UPDATE products SET quantity = ? WHERE id = ?";

        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // valida party conforme tipo
            PartyType requiredPartyType = (type == MovementType.SAIDA) ? PartyType.CLIENTE : PartyType.FORNECEDOR;
            validateParty(conn, partyId, requiredPartyType);

            // valida estoque para SAIDA
            if (type == MovementType.SAIDA) {
                for (StockMovementItem it : normalized) {
                    int currentQty = getCurrentQty(conn, selectQty, it.getProductId());
                    int newQty = currentQty - it.getQuantity();
                    if (newQty < 0) {
                        throw new IllegalArgumentException("Estoque insuficiente para o produto (id=" + it.getProductId() + "). Atual: " + currentQty);
                    }
                }
            }

            long movementId;

            // Insere cabeçalho
            try (PreparedStatement ps = conn.prepareStatement(insertMove, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, type.name());
                ps.setString(2, LocalDateTime.now().toString());
                ps.setLong(3, partyId);
                ps.setInt(4, totalCents);
                ps.setString(5, (observation == null || observation.isBlank()) ? null : observation.trim());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new IllegalStateException("Falha ao obter id da movimentação.");
                    movementId = keys.getLong(1);
                }
            }

            // Insere itens
            for (StockMovementItem it : normalized) {
                int unitCents = MoneyUtil.toCents(it.getUnitPrice());
                int subtotalCents = Math.multiplyExact(unitCents, it.getQuantity());

                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setLong(1, movementId);
                    ps.setLong(2, it.getProductId());
                    ps.setInt(3, it.getQuantity());
                    ps.setInt(4, unitCents);
                    ps.setInt(5, subtotalCents);
                    ps.executeUpdate();
                }
            }

            // Atualiza estoque
            for (StockMovementItem it : normalized) {
                int currentQty = getCurrentQty(conn, selectQty, it.getProductId());

                int newQty = (type == MovementType.ENTRADA)
                        ? Math.addExact(currentQty, it.getQuantity())
                        : Math.subtractExact(currentQty, it.getQuantity());

                try (PreparedStatement ps = conn.prepareStatement(updateQty)) {
                    ps.setInt(1, newQty);
                    ps.setLong(2, it.getProductId());
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar movimentação", e);
        }
    }

    private void validateParty(java.sql.Connection conn, long partyId, PartyType requiredType) throws Exception {
        String sql = "SELECT type FROM parties WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, partyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Cliente/Fornecedor não encontrado.");
                PartyType actual = PartyType.valueOf(rs.getString("type"));
                if (actual != requiredType) {
                    throw new IllegalArgumentException("Tipo de pessoa inválido para esta movimentação. Esperado: " + requiredType);
                }
            }
        }
    }

    private int getCurrentQty(java.sql.Connection conn, String selectQtySql, long productId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(selectQtySql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Produto não encontrado (id=" + productId + ")");
                return rs.getInt("quantity");
            }
        }
    }
}