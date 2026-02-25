package br.com.matheus.stockcontrol.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public final class Database {

    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "stockcontrol.db";

    // Controle de versão do schema (SQLite PRAGMA user_version)
    private static final int SCHEMA_VERSION = 3;

    private Database() {}

    public static Connection getConnection() {
        try {
            ensureDatabase();
            String url = "jdbc:sqlite:" + Path.of(DB_FOLDER, DB_FILE).toAbsolutePath();
            Connection conn = DriverManager.getConnection(url);

            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            return conn;
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

            st.execute("PRAGMA foreign_keys = ON");

            int userVersion = getUserVersion(st);

            if (userVersion < 1) {
                createSchemaV1(st);
                setUserVersion(st, 1);
                userVersion = 1;
            }

            if (userVersion < 2) {
                migrateV1ToV2(conn);
                setUserVersion(st, 2);
                userVersion = 2;
            }

            if (userVersion < 3) {
                migrateV2ToV3(conn);
                setUserVersion(st, 3);
                userVersion = 3;
            }

            if (userVersion > SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "Banco está em uma versão mais nova (" + userVersion + ") do que a aplicação (" + SCHEMA_VERSION + ")."
                );
            }
        }
    }

    private static int getUserVersion(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void setUserVersion(Statement st, int version) throws Exception {
        st.execute("PRAGMA user_version = " + version);
    }

    private static void createSchemaV1(Statement st) throws Exception {
        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                sku TEXT NOT NULL UNIQUE,
                quantity INTEGER NOT NULL DEFAULT 0,
                price_cents INTEGER NOT NULL DEFAULT 0
            )
        """);
    }

    private static void migrateV1ToV2(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = OFF");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
            """);

            st.executeUpdate("""
                INSERT INTO categories (name)
                VALUES ('Geral')
                ON CONFLICT(name) DO NOTHING
            """);

            st.executeUpdate("ALTER TABLE products RENAME TO products_old");

            st.executeUpdate("""
                CREATE TABLE products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    sku TEXT NOT NULL UNIQUE,
                    category_id INTEGER NOT NULL,
                    cost_cents INTEGER NOT NULL DEFAULT 0,
                    sale_cents INTEGER NOT NULL DEFAULT 0,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    min_stock INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (category_id) REFERENCES categories(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
            """);

            st.executeUpdate("""
                INSERT INTO products (id, name, sku, category_id, cost_cents, sale_cents, quantity, min_stock)
                SELECT
                    p.id,
                    p.name,
                    p.sku,
                    (SELECT id FROM categories WHERE name = 'Geral'),
                    0,
                    p.price_cents,
                    p.quantity,
                    0
                FROM products_old p
            """);

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id)");

            st.executeUpdate("DROP TABLE products_old");

            // v2 tinha stock_movements (vamos substituir na v3)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    type TEXT NOT NULL CHECK (type IN ('ENTRADA', 'SAIDA')),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    datetime TEXT NOT NULL,
                    observation TEXT,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
            """);

            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static void migrateV2ToV3(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = OFF");

            // Descarta modelo antigo (1 produto por movimentação)
            st.executeUpdate("DROP TABLE IF EXISTS stock_movements");

            // Cabeçalho
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL CHECK (type IN ('ENTRADA', 'SAIDA')),
                    datetime TEXT NOT NULL,
                    total_cents INTEGER NOT NULL DEFAULT 0,
                    observation TEXT
                )
            """);

            // Itens
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS stock_movement_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    movement_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    unit_cents INTEGER NOT NULL DEFAULT 0,
                    subtotal_cents INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (movement_id) REFERENCES stock_movements(id)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
            """);

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_movements_type_datetime ON stock_movements(type, datetime)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_items_movement ON stock_movement_items(movement_id)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_items_product ON stock_movement_items(product_id)");

            st.execute("PRAGMA foreign_keys = ON");
        }
    }
}