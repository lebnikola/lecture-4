package txdemo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ДЕМО 2 (сценарий 2): перевод денег БЕЗ управления транзакцией.
 *
 * autocommit включён по умолчанию: каждый UPDATE выполняется и коммитится
 * как отдельная самостоятельная транзакция. Если между списанием и зачислением
 * происходит сбой — деньги «исчезают».
 */
public class Demo02_AutocommitFailure {

    public static void main(String[] args) throws SQLException {
        Db.resetAndSeed();

        try (Connection c = Db.getConnection()) {
            System.out.println("== ДЕМО 2: перевод БЕЗ управления транзакцией ==");
            System.out.println("autocommit по умолчанию = " + c.getAutoCommit() + "\n");
            Db.printBalances(c);

            // Шаг 1: этот UPDATE завершится и ЗАКОММИТИТСЯ немедленно.
            System.out.println("Шаг 1: UPDATE ... balance = balance - 100 FOR alice");
            Db.updateBalance(c, "alice", new BigDecimal("-100"));
            System.out.println("  -> выполнено и УЖЕ ЗАКОММИЧЕНО (autocommit).\n");

            // Шаг 2: симулируем крах приложения между двумя операциями:
            // сеть отвалилась, OOM, kill -9 — с точки зрения БД не важно почему.
            System.out.println("Шаг 2: случается сбой приложения (исключение)...");
            throw new IllegalStateException("Симуляция сбоя: приложение упало между шагами");

            // Шаг 3 ниже никогда не выполнится:
            // Db.updateBalance(c, "bob", new BigDecimal("100"));

        } catch (IllegalStateException e) {
            System.out.println("Поймали исключение: " + e.getMessage());
            System.out.println("ИТОГ: списание закоммичено, зачисления нет — 100 у.е. исчезли!\n");
            try (Connection after = Db.getConnection()) {
                Db.printBalances(after);
            }
        }
    }
}
