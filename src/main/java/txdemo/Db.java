package txdemo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * Общие вспомогательные методы для всех демо-сценариев лекции.
 */
public final class Db {

    static final String URL = System.getenv().getOrDefault(
            "TXDEMO_URL", "jdbc:postgresql://localhost:5432/txdemo");
    static final String USER = System.getenv().getOrDefault("TXDEMO_USER", "txdemo");
    static final String PASSWORD = System.getenv().getOrDefault("TXDEMO_PASSWORD", "txdemo");

    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Возвращает балансы к исходным значениям: alice = 1000, bob = 500. */
    public static void resetAndSeed() throws SQLException {
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {
            int updated = st.executeUpdate(
                    "UPDATE accounts SET balance = CASE owner "
                            + "WHEN 'alice' THEN 1000.00 WHEN 'bob' THEN 500.00 END");
            if (updated != 2) {
                throw new SQLException("Ожидались аккаунты alice и bob, обновлено строк: " + updated);
            }
            st.execute("DELETE FROM audit_log");
        }
        System.out.println("(балансы сброшены к исходным: alice=1000, bob=500)\n");
    }

    public static void printBalances(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT owner, balance FROM accounts ORDER BY id")) {
            System.out.println("Текущие балансы:");
            while (rs.next()) {
                System.out.printf("    %-8s %10.2f%n",
                        rs.getString("owner"), rs.getBigDecimal("balance"));
            }
        }
        System.out.println();
    }

    public static BigDecimal readBalance(Connection c, String owner) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT balance FROM accounts WHERE owner = ?")) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Аккаунт не найден: " + owner);
                }
                return rs.getBigDecimal("balance");
            }
        }
    }

    public static void updateBalance(Connection c, String owner, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE accounts SET balance = balance + ? WHERE owner = ?")) {
            ps.setBigDecimal(1, delta);
            ps.setString(2, owner);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Аккаунт не найден: " + owner);
            }
        }
    }

    /** Списание и зачисление как две отдельные операции — без слов о транзакции. */
    public static void moveMoney(Connection c, String from, String to, BigDecimal amount) throws SQLException {
        System.out.println("Списываем " + amount + " со счёта '" + from + "'...");
        updateBalance(c, from, amount.negate());
        System.out.println("Зачисляем " + amount + " на счёт '" + to + "'...");
        updateBalance(c, to, amount);
    }

    public static void insertAudit(Connection c, String message) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_log(message) VALUES (?)")) {
            ps.setString(1, message);
            ps.executeUpdate();
        }
    }

    public static int countAudit(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM audit_log")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Демонстрационный savepoint-хелпер, чтобы не засорять основной сценарий. */
    public static void releaseQuietly(Connection c, Savepoint sp) {
        try {
            if (sp != null) {
                c.releaseSavepoint(sp);
            }
        } catch (SQLException ignored) {
            // после commit/rollback savepoints уже сняты сервером
        }
    }
}
