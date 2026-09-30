package txdemo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;

/**
 * ДЕМО 4 (сценарий 4): savepoint — откат части работы.
 *
 * Внутри одной транзакции: перевод денег + запись в аудит.
 * Строка аудита длиннее VARCHAR(50) -> сервер возвращает ошибку 22001.
 * Откатываемся к savepoint: попытка аудита отменена, перевод сохранён.
 */
public class Demo04_SavepointAudit {

    public static void main(String[] args) throws SQLException {
        Db.resetAndSeed();

        try (Connection c = Db.getConnection()) {
            System.out.println("== ДЕМО 4: savepoint — частичный откат ==");
            c.setAutoCommit(false);
            Db.printBalances(c);

            Savepoint beforeAudit = null;
            try {
                Db.moveMoney(c, "alice", "bob", new BigDecimal("100"));

                System.out.println("Ставим SAVEPOINT перед записью в аудит");
                beforeAudit = c.setSavepoint("before_audit");

                String message = "Перевод 100.00 со счёта 'alice' на счёт 'bob', "
                        + "подтверждено модулем аудита, операция №" + System.currentTimeMillis();
                System.out.println("Пишем в аудит строку длиной " + message.length()
                        + " символов (лимит VARCHAR(50))...");
                Db.insertAudit(c, message);

                c.commit();
                System.out.println(">>> COMMIT выполнен.");

            } catch (SQLException e) {
                System.out.println(">>> Ошибка сервера: [" + e.getSQLState() + "] " + e.getMessage());
                if (beforeAudit == null) {
                    System.out.println(">>> Ошибка до savepoint — откатываем всю транзакцию.");
                    c.rollback();
                } else {
                    System.out.println(">>> ROLLBACK TO SAVEPOINT: отменяем только неудавшийся аудит.");
                    c.rollback(beforeAudit);
                    c.commit();
                    System.out.println(">>> COMMIT: перевод сохранён, записи аудита нет.\n");
                }
            }

            Db.printBalances(c);
            System.out.println("Записей в audit_log: " + Db.countAudit(c));
        }
    }
}
