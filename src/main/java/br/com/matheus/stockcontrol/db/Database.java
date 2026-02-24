package br.com.matheus.stockcontrol.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class Database {

    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "stockcontrol.db";

    private Database() {}

    public static Connection getConnection() {
        try {
            ensureDatabase();
            String url = "jdbc:sqlite:" + Path.of(DB_FOLDER, DB_FILE).toAbsolutePath();
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar no SQLite", e);
        }
    }

    private static void ensureDatabase() throws Exception {
        Path folder = Path.of(DB_FOLDER);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        String dbPath = Path.of(DB_FOLDER, DB_FILE).toAbsolutePath().toString();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement()) {

            // 1) cria a tabela
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    sku TEXT NOT NULL UNIQUE,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    price_cents INTEGER NOT NULL DEFAULT 0
                )
            """);

            // 2) SEED: insere alguns registros se a tabela estiver vazia
            st.executeUpdate("""
                INSERT INTO products (name, sku, quantity, price_cents)
                SELECT 'Mouse', 'SKU-001', 10, 5990
                WHERE NOT EXISTS (SELECT 1 FROM products)
            """);

            st.executeUpdate("""
                INSERT INTO products (name, sku, quantity, price_cents)
                SELECT 'Teclado', 'SKU-002', 5, 12990
                WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-002')
            """);

            st.executeUpdate("""
                INSERT INTO products (name, sku, quantity, price_cents)
                SELECT 'Monitor', 'SKU-003', 2, 89900
                WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-003')
            """);
        }
    }
}