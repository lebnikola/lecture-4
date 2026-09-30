# Демо-проект к лекции «Java JDBC: работа с транзакциями на примере PostgreSQL»

Java 17 + Maven + PostgreSQL 16 (Docker). Каждый сценарий лекции — отдельный
класс с подробными комментариями.

## Требования

- JDK 17+
- Maven 3.8+ (`brew install maven`)
- Docker (для PostgreSQL)

## Запуск PostgreSQL

```bash
docker compose up -d          # старт контейнера, схема применяется автоматически
docker compose ps             # дождаться статуса healthy
```

Подключение: `jdbc:postgresql://localhost:5432/txdemo`, пользователь `txdemo`,
пароль `txdemo` (переопределяются переменными `TXDEMO_URL`, `TXDEMO_USER`,
`TXDEMO_PASSWORD`).

## Сценарии

| Класс | Сценарий лекции | Что показываем |
|-------|-----------------|----------------|
| `Demo02_AutocommitFailure` | 2 (5 мин) | Перевод без транзакции: сбой между UPDATE — деньги исчезли |
| `Demo03_CommitRollback` | 3 (4 мин) | Тот же перевод с `setAutoCommit(false)` + commit/rollback |
| `Demo04_SavepointAudit` | 4 (3 мин) | Savepoint: ошибка аудита, откат только её части |
| `Demo05_IsolationLevels` | 5 (4 мин) | READ COMMITTED: невоспроизводимое чтение; SERIALIZABLE: ошибка 40001 |
| `Demo06_BatchInsert` | 6 (2 мин) | Batch-вставка против построчной: сравнение времени |

Сценарий 1 (обзор проекта) — это сам этот README и структура каталогов.

```bash
# из каталога demo/
mvn exec:java -Ddemo.mainClass=txdemo.Demo02_AutocommitFailure
mvn exec:java -Ddemo.mainClass=txdemo.Demo03_CommitRollback              # со сбоем
mvn exec:java -Ddemo.mainClass=txdemo.Demo03_CommitRollback -Dexec.args=ok   # успешный перевод
mvn exec:java -Ddemo.mainClass=txdemo.Demo04_SavepointAudit
mvn exec:java -Ddemo.mainClass=txdemo.Demo05_IsolationLevels
mvn exec:java -Ddemo.mainClass=txdemo.Demo06_BatchInsert
```

Каждый сценарий начинается со сброса данных (`alice=1000`, `bob=500`),
поэтому классы можно запускать в любом порядке и повторно.

Ручная проверка через psql:

```bash
docker exec -it txdemo-db psql -U txdemo -d txdemo
  SELECT * FROM accounts;
  SELECT * FROM audit_log;
```

## Остановка

```bash
docker compose down          # остановить (данные сохраняются в volume)
docker compose down -v       # остановить и удалить данные (схема пересоздастся при следующем старте)
```

## Если порт 5432 занят

Либо освободите порт, либо измените публикацию порта в `docker-compose.yml`
(например, `"15432:5432"`) и задайте `TXDEMO_URL=jdbc:postgresql://localhost:15432/txdemo`.
