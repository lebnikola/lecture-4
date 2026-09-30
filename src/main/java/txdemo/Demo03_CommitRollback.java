package txdemo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * ДЕМО 3 (сценарий 3): тот же перевод, но с ручной транзакцией.
 *
 * Запуск:
 *   mvn exec:java -Ddemo.mainClass=txdemo.Demo03_CommitRollback            -- сбой (по умолчанию)
 *   mvn exec:java -Ddemo.mainClass=txdemo.Demo03_CommitRollback -Dexec.args=ok  -- успешный перевод
 */
public class Demo03_CommitRollback {

    public static void main(String[] args) throws SQLException {
        boolean simulateFailure = args.length == 0 || !"ok".equalsIgnoreCase(args[0]);
        Db.resetAndSeed();

        try (Connection c = Db.getConnection()) {
            System.out.println("== ДЕМО 3: перевод с ручной транзакцией (сбой=" + simulateFailure + ") ==");
            c.setAutoCommit(false);
            Db.printBalances(c);

            try {
                Db.moveMoney(c, "alice", "bob", new BigDecimal("100"));

                if (simulateFailure) {
                    System.out.println("Случается сбой приложения (исключение)...");
                    throw new IllegalStateException("Симуляция сбоя после списания");
                }

                Db.insertAudit(c, "Перевод alice->bob 100.00");
                c.commit();
                System.out.println(">>> COMMIT: все три операции зафиксированы.\n");

            } catch (Exception e) {
                System.out.println(">>> Исключение: " + e.getMessage());
                System.out.println(">>> ROLLBACK: отменяем ВСЁ, что сделано в транзакции.");
                c.rollback();
                System.out.println(">>> База данных вернулась к состоянию до перевода.\n");
            }

            Db.printBalances(c);
            System.out.println("Записей в audit_log: " + Db.countAudit(c));
        }
    }
}
