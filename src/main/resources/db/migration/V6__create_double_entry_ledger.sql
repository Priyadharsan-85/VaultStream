INSERT INTO accounts (id, account_number, account_holder_name, balance, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM_CLEARING', 'VaultStream Clearing Account', 0, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

CREATE TABLE ledger_journals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(100),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_id UUID NOT NULL REFERENCES ledger_journals(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ledger_entries_journal_id ON ledger_entries(journal_id);
CREATE INDEX idx_ledger_entries_account_id_created_at ON ledger_entries(account_id, created_at);

WITH opening AS MATERIALIZED (
    SELECT uuid_generate_v4() AS journal_id, a.id AS account_id, a.balance
    FROM accounts a
    WHERE a.id <> '00000000-0000-0000-0000-000000000001' AND a.balance <> 0
), inserted_journals AS (
    INSERT INTO ledger_journals (id, transaction_type, description, created_at)
    SELECT journal_id, 'OPENING_BALANCE', 'Balance carried forward during ledger migration', now()
    FROM opening
    RETURNING id
)
INSERT INTO ledger_entries (id, journal_id, account_id, direction, amount, created_at)
SELECT uuid_generate_v4(), opening.journal_id, opening.account_id,
       CASE WHEN opening.balance > 0 THEN 'CREDIT' ELSE 'DEBIT' END,
       abs(opening.balance), now()
FROM opening
JOIN inserted_journals ON inserted_journals.id = opening.journal_id
UNION ALL
SELECT uuid_generate_v4(), opening.journal_id, '00000000-0000-0000-0000-000000000001',
       CASE WHEN opening.balance > 0 THEN 'DEBIT' ELSE 'CREDIT' END,
       abs(opening.balance), now()
FROM opening
JOIN inserted_journals ON inserted_journals.id = opening.journal_id;
