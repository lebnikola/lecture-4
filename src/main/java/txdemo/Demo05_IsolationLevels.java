package txdemo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

/**
 * ДЕМО 5 (сценарий 5): уровни изоляции, два клиента, один счёт.
 *
 * Часть A (READ COMMITTED): читатель дважды читает баланс alice в ОДНОЙ
 * транзакции и получает разные значения — невоспроизводимое чтение.
 *
 * Часть B (SERIALIZABLE): та же ситуация, но попытка читателя изменить
 * конфликтующую строку завершается ошибкой 40001 (serialization failure).
 */
public class Demo05_IsolationLevels {

    public static void main(String[] args) throws Exception {
        System.out.println("== ДЕМО 5: уровни изоляции, два клиента ==");
        Db.resetAndSeed();

        System.out.println("--- Часть A: READ COMMITTED ---");
        partA_ReadCommitted();

        Db.resetAndSeed();
        System.out.println("\n--- Часть B: SERIALIZABLE ---");
        partB_Serializable();
    }

    private static void partA_ReadCommitted() throws Exception {
        CountDownLatch readerReadOnce = new CountDownLatch(1);
        CountDownLatch writerCommitted = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            try (Connection c = Db.getConnection()) {
                c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                c.setAutoCommit(false);

                BigDecimal first = Db.readBalance(c, "alice");
                System.out.println("[Читатель] 1-е чтение баланса alice = " + first);
                readerReadOnce.countDown();
                writerCommitted.await();

                BigDecimal second = Db.readBalance(c, "alice");
                System.out.println("[Читатель] 2-е чтение баланса alice = " + second);
                if (first.compareTo(second) != 0) {
                    System.out.println(">>> READ COMMITTED: два РАЗНЫХ ответа внутри одной"
                            + " транзакции — невоспроизводимое чтение.");
                }
                c.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "reader");

        Thread writer = new Thread(() -> {
            try {
                readerReadOnce.await();
                try (Connection c = Db.getConnection()) {
                    c.setAutoCommit(false);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE accounts SET balance = balance + 200 WHERE owner = 'alice'")) {
                        ps.executeUpdate();
                    }
                    c.commit();
                    System.out.println("[Писатель] +200 для alice и COMMIT");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                writerCommitted.countDown();
            }
        }, "writer");

        reader.start();
        writer.start();
        reader.join();
        writer.join();
    }

    private static void partB_Serializable() throws Exception {
        CountDownLatch readerReadOnce = new CountDownLatch(1);
        CountDownLatch writerCommitted = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            try (Connection c = Db.getConnection()) {
                c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                c.setAutoCommit(false);

                BigDecimal balance = Db.readBalance(c, "alice");
                System.out.println("[Читатель] прочитал баланс alice = " + balance);
                readerReadOnce.countDown();
                writerCommitted.await();

                System.out.println("[Читатель] пробуем обновить ту же строку...");
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE accounts SET balance = balance + 1 WHERE owner = 'alice'")) {
                    ps.executeUpdate();
                }
                c.commit();
                System.out.println("[Читатель] обновление прошло (не ожидали).");
            } catch (SQLException e) {
                System.out.println("[Читатель] SQLException [" + e.getSQLState() + "]: "
                        + e.getMessage());
                if ("40001".equals(e.getSQLState())) {
                    System.out.println(">>> SERIALIZABLE защитил корректность: ошибка 40001."
                            + " Правильная реакция приложения — повторить транзакцию.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "reader");

        Thread writer = new Thread(() -> {
            try {
                readerReadOnce.await();
                try (Connection c = Db.getConnection()) {
                    c.setAutoCommit(false);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE accounts SET balance = balance + 200 WHERE owner = 'alice'")) {
                        ps.executeUpdate();
                    }
                    c.commit();
                    System.out.println("[Писатель] +200 для alice и COMMIT");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                writerCommitted.countDown();
            }
        }, "writer");

        reader.start();
        writer.start();
        reader.join();
        writer.join();
    }
}
