package br.com.matheus.stockcontrol.dao;

import br.com.matheus.stockcontrol.db.Database;
import br.com.matheus.stockcontrol.model.MovementType;
import br.com.matheus.stockcontrol.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DashboardDao {

    public record Kpis(BigDecimal entradas, BigDecimal saidas, BigDecimal lucroBrutoEstimado, int movimentos) {}

    public record MonthSeriesRow(YearMonth month, BigDecimal entradas, BigDecimal saidas) {}

    public record TopProductRow(long productId, String sku, String name, int qty) {}

    public record LowStockRow(long productId, String sku, String name, int quantity, int minStock) {}


    public Kpis getKpis(LocalDate startInclusive, LocalDate endInclusive) {
        String sql = """
            SELECT
              COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.total_cents ELSE 0 END), 0) AS entradas_cents,
              COALESCE(SUM(CASE WHEN m.type = 'SAIDA' THEN m.total_cents ELSE 0 END), 0) AS saidas_cents,
              COALESCE(SUM(CASE WHEN m.type = 'SAIDA' THEN m.total_cents ELSE 0 END), 0)
                - COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.total_cents ELSE 0 END), 0) AS lucro_cents,
              COUNT(*) AS movimentos
            FROM stock_movements m
            WHERE m.datetime >= ? AND m.datetime <= ?
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, startInclusive.atStartOfDay().toString());
            ps.setString(2, endInclusive.atTime(23, 59, 59).toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new Kpis(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

                BigDecimal entradas = MoneyUtil.fromCents(rs.getInt("entradas_cents"));
                BigDecimal saidas = MoneyUtil.fromCents(rs.getInt("saidas_cents"));
                BigDecimal lucro = MoneyUtil.fromCents(rs.getInt("lucro_cents"));
                int movimentos = rs.getInt("movimentos");

                return new Kpis(entradas, saidas, lucro, movimentos);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular KPIs do dashboard", e);
        }
    }

    public List<MonthSeriesRow> getEntradasSaidasLastMonths(int months) {
        int n = Math.max(1, Math.min(months, 24));

        // SQLite: agrupamento por mês via substr(YYYY-MM, 1..7)
        String sql = """
            SELECT
              substr(m.datetime, 1, 7) AS ym,
              COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.total_cents ELSE 0 END), 0) AS entradas_cents,
              COALESCE(SUM(CASE WHEN m.type = 'SAIDA' THEN m.total_cents ELSE 0 END), 0) AS saidas_cents
            FROM stock_movements m
            WHERE m.datetime >= ?
            GROUP BY ym
            ORDER BY ym ASC
        """;

        YearMonth startYm = YearMonth.now().minusMonths(n - 1);
        LocalDate start = startYm.atDay(1);

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, start.atStartOfDay().toString());

            // Primeiro, traz o que existe no banco
            List<MonthSeriesRow> raw = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    YearMonth ym = YearMonth.parse(rs.getString("ym"));
                    BigDecimal entradas = MoneyUtil.fromCents(rs.getInt("entradas_cents"));
                    BigDecimal saidas = MoneyUtil.fromCents(rs.getInt("saidas_cents"));
                    raw.add(new MonthSeriesRow(ym, entradas, saidas));
                }
            }

            // Depois, “completa” os meses faltantes com zero (fica bonito no gráfico)
            List<MonthSeriesRow> filled = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                YearMonth ym = startYm.plusMonths(i);
                MonthSeriesRow found = raw.stream().filter(r -> r.month().equals(ym)).findFirst().orElse(null);
                if (found == null) filled.add(new MonthSeriesRow(ym, BigDecimal.ZERO, BigDecimal.ZERO));
                else filled.add(found);
            }

            return filled;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar série mensal do dashboard", e);
        }
    }

    /**
     * Série mensal de entradas/saídas respeitando um período selecionado (start/end).
     * - Agrupa por mês (YYYY-MM)
     * - Completa meses faltantes com zero para ficar consistente no gráfico
     */
    public List<MonthSeriesRow> getEntradasSaidasByMonth(LocalDate startInclusive, LocalDate endInclusive) {
        if (startInclusive == null || endInclusive == null) {
            throw new IllegalArgumentException("Período inválido.");
        }
        if (endInclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("Período inválido (fim < início).");
        }

        YearMonth startYm = YearMonth.from(startInclusive);
        YearMonth endYm = YearMonth.from(endInclusive);

        // limite de segurança para não travar a UI caso alguém selecione 20 anos
        int monthsBetween = (endYm.getYear() - startYm.getYear()) * 12 + (endYm.getMonthValue() - startYm.getMonthValue()) + 1;
        if (monthsBetween < 1) monthsBetween = 1;
        if (monthsBetween > 60) { // 5 anos
            throw new IllegalArgumentException("Período muito grande para o gráfico. Selecione no máximo 60 meses.");
        }

        String sql = """
            SELECT
              substr(m.datetime, 1, 7) AS ym,
              COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.total_cents ELSE 0 END), 0) AS entradas_cents,
              COALESCE(SUM(CASE WHEN m.type = 'SAIDA' THEN m.total_cents ELSE 0 END), 0) AS saidas_cents
            FROM stock_movements m
            WHERE m.datetime >= ? AND m.datetime <= ?
            GROUP BY ym
            ORDER BY ym ASC
        """;

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, startInclusive.atStartOfDay().toString());
            ps.setString(2, endInclusive.atTime(23, 59, 59).toString());

            List<MonthSeriesRow> raw = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    YearMonth ym = YearMonth.parse(rs.getString("ym"));
                    BigDecimal entradas = MoneyUtil.fromCents(rs.getInt("entradas_cents"));
                    BigDecimal saidas = MoneyUtil.fromCents(rs.getInt("saidas_cents"));
                    raw.add(new MonthSeriesRow(ym, entradas, saidas));
                }
            }

            // preenche meses sem movimentação com zero
            List<MonthSeriesRow> filled = new ArrayList<>();
            YearMonth cursor = startYm;
            while (!cursor.isAfter(endYm)) {
                YearMonth cur = cursor;
                MonthSeriesRow found = raw.stream().filter(r -> r.month().equals(cur)).findFirst().orElse(null);
                if (found == null) filled.add(new MonthSeriesRow(cur, BigDecimal.ZERO, BigDecimal.ZERO));
                else filled.add(found);
                cursor = cursor.plusMonths(1);
            }

            return filled;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar série mensal do dashboard (por período)", e);
        }
    }

    public List<TopProductRow> getTopProductsByQuantity(MovementType type, LocalDate startInclusive, LocalDate endInclusive, int limit) {
        int n = Math.max(1, Math.min(limit, 20));

        String sql = """
            SELECT
              p.id AS product_id,
              p.sku AS sku,
              p.name AS name,
              COALESCE(SUM(i.quantity), 0) AS qty
            FROM stock_movements m
            JOIN stock_movement_items i ON i.movement_id = m.id
            JOIN products p ON p.id = i.product_id
            WHERE m.type = ?
              AND m.datetime >= ? AND m.datetime <= ?
            GROUP BY p.id, p.sku, p.name
            ORDER BY qty DESC
            LIMIT ?
        """;

        List<TopProductRow> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.name());
            ps.setString(2, startInclusive.atStartOfDay().toString());
            ps.setString(3, endInclusive.atTime(23, 59, 59).toString());
            ps.setInt(4, n);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopProductRow(
                            rs.getLong("product_id"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getInt("qty")
                    ));
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar top produtos", e);
        }
    }

    public List<LowStockRow> getLowStock(int limit) {
        int n = Math.max(1, Math.min(limit, 50));

        String sql = """
            SELECT
              id AS product_id,
              sku,
              name,
              quantity,
              min_stock
            FROM products
            WHERE min_stock > 0
              AND quantity <= min_stock
            ORDER BY (min_stock - quantity) DESC, name ASC
            LIMIT ?
        """;

        List<LowStockRow> list = new ArrayList<>();

        try (var conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, n);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LowStockRow(
                            rs.getLong("product_id"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getInt("min_stock")
                    ));
                }
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar produtos com baixo estoque", e);
        }
    }
}