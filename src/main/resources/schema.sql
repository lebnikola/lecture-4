-- Схема для лекции «Java JDBC: работа с транзакциями на примере PostgreSQL».
-- Применяется автоматически при первом старте контейнера
-- (монтируется в /docker-entrypoint-initdb.d).

CREATE TABLE IF NOT EXISTS accounts (
    id      BIGSERIAL PRIMARY KEY,
    owner   TEXT NOT NULL UNIQUE,
    balance NUMERIC(12, 2) NOT NULL CHECK (balance >= 0)
);

-- message VARCHAR(50): в демо 4 специально отправляем строку длиннее 50 символов,
-- чтобы получить реальную ошибку сервера (22001) и показать rollback to savepoint.
CREATE TABLE IF NOT EXISTS audit_log (
    id      BIGSERIAL PRIMARY KEY,
    ts      TIMESTAMPTZ NOT NULL DEFAULT now(),
    message VARCHAR(50) NOT NULL
);

INSERT INTO accounts (owner, balance) VALUES
    ('alice', 1000.00),
    ('bob',   500.00)
ON CONFLICT (owner) DO NOTHING;
