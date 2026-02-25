package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.model.StockMovement;
import br.com.matheus.stockcontrol.model.StockMovementItem;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class StockMovementDao {

    public List<StockMovement> listMovements(MovementType type, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, type, datetime, total_cents, observation
            FROM stock_movements
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ? ");
            params.add(type.name());
        }

        // período (inclusive)
        if (start != null) {
            sql.append(" AND datetime >= ? ");
            params.add(start.atStartOfDay().toString());
        }
        if (end != null) {
            sql.append(" AND datetime <= ? ");
            params.add(end.atTime(23, 59, 59).toString());
        }

        sql.append(" ORDER BY datetime DESC LIMIT 500");

        List<StockMovement> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int totalCents = rs.getInt("total_cents");
                    BigDecimal total = BigDecimal.valueOf(totalCents).movePointLeft(2);

                    list.add(new StockMovement(
                            rs.getLong("id"),
                            MovementType.valueOf(rs.getString("type")),
                            LocalDateTime.parse(rs.getString("datetime")),
                            total,
                            rs.getString("observation")
                    ));
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
                i.movement_id,
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
                    BigDecimal unit = BigDecimal.valueOf(rs.getInt("unit_cents")).movePointLeft(2);
                    BigDecimal subtotal = BigDecimal.valueOf(rs.getInt("subtotal_cents")).movePointLeft(2);

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

    public void createMovement(MovementType type, List<StockMovementItem> items, String observation) {
        if (type == null) throw new IllegalArgumentException("Tipo é obrigatório.");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um item.");

        // Soma por produto (caso a UI deixe duplicar)
        Map<Long, StockMovementItem> byProduct = new LinkedHashMap<>();
        for (StockMovementItem it : items) {
            if (it.getProductId() == null) throw new IllegalArgumentException("Item sem produto.");
            if (it.getQuantity() <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");

            BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            if (unit.signum() < 0) throw new IllegalArgumentException("Valor unitário não pode ser negativo.");

            if (byProduct.containsKey(it.getProductId())) {
                StockMovementItem acc = byProduct.get(it.getProductId());
                acc.setQuantity(acc.getQuantity() + it.getQuantity());
                // mantém unit do último (ou poderia validar igualdade)
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

        // calcula subtotal e total (sempre, inclusive ENTRADA)
        int totalCents = 0;
        for (StockMovementItem it : normalized) {
            int unitCents = toCents(it.getUnitPrice());
            int subtotalCents = Math.multiplyExact(unitCents, it.getQuantity());
            it.setSubtotal(BigDecimal.valueOf(subtotalCents).movePointLeft(2));
            totalCents = Math.addExact(totalCents, subtotalCents);
        }

        String insertMove = """
            INSERT INTO stock_movements (type, datetime, total_cents, observation)
            VALUES (?, ?, ?, ?)
        """;

        String insertItem = """
            INSERT INTO stock_movement_items (movement_id, product_id, quantity, unit_cents, subtotal_cents)
            VALUES (?, ?, ?, ?, ?)
        """;

        // Vamos atualizar estoque por produto:
        // ENTRADA: +qty
        // SAIDA: -qty (com validação)
        String selectQty = "SELECT quantity FROM products WHERE id = ?";
        String updateQty = "UPDATE products SET quantity = ? WHERE id = ?";

        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // valida estoque para SAIDA (considerando soma por produto)
            if (type == MovementType.SAIDA) {
                for (StockMovementItem it : normalized) {
                    int currentQty;

                    try (PreparedStatement ps = conn.prepareStatement(selectQty)) {
                        ps.setLong(1, it.getProductId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("Produto não encontrado (id=" + it.getProductId() + ")");
                            currentQty = rs.getInt("quantity");
                        }
                    }

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
                ps.setInt(3, totalCents);
                ps.setString(4, (observation == null || observation.isBlank()) ? null : observation.trim());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new IllegalStateException("Falha ao obter id da movimentação.");
                    movementId = keys.getLong(1);
                }
            }

            // Insere itens
            for (StockMovementItem it : normalized) {
                int unitCents = toCents(it.getUnitPrice());
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
                int currentQty;

                try (PreparedStatement ps = conn.prepareStatement(selectQty)) {
                    ps.setLong(1, it.getProductId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Produto não encontrado (id=" + it.getProductId() + ")");
                        currentQty = rs.getInt("quantity");
                    }
                }

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

    private int toCents(BigDecimal value) {
        if (value == null) return 0;
        return value.movePointRight(2).intValueExact();
    }
}