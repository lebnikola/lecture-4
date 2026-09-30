package txdemo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ДЕМО 6 (сценарий 6): batch-вставка против построчной.
 *
 * Вставляем 1000 платежей двумя способами и сравниваем время:
 * 1) по одному INSERT с autocommit — 1000 транзакций;
 * 2) executeBatch в одной транзакции — 1 round-trip логики, 1 COMMIT.
 */
public class Demo06_BatchInsert {

    private static final int ROWS = 1000;

    public static void main(String[] args) throws SQLException {
        System.out.println("== ДЕМО 6: вставка " + ROWS + " платежей в audit_log ==");

        long rowByRowMs = insertRowByRow();
        long batchMs = insertWithBatch();

        System.out.printf("%nПострочно с autocommit : %5d мс%n", rowByRowMs);
        System.out.printf("executeBatch + 1 COMMIT: %5d мс%n", batchMs);
        System.out.println("\nИТОГ: batch экономит round-trips и коммиты — массовые операции"
                + " выполняйте в одной транзакции.");
    }

    private static long insertRowByRow() throws SQLException {
        try (Connection c = Db.getConnection()) {
            long start = System.nanoTime(); // autocommit=true: каждый INSERT — своя транзакция
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log(message) VALUES (?)")) {
                for (int i = 1; i <= ROWS; i++) {
                    ps.setString(1, "payment #" + i);
                    ps.executeUpdate();
                }
            }
            return elapsedMs(start);
        }
    }

    private static long insertWithBatch() throws SQLException {
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false); // одна транзакция на весь batch
            long start = System.nanoTime();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log(message) VALUES (?)")) {
                for (int i = 1; i <= ROWS; i++) {
                    ps.setString(1, "payment #batch-" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
            return elapsedMs(start);
        }
    }

    private static long elapsedMs(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000;
    }
}
