CREATE INDEX IF NOT EXISTS idx_transaction_account_timestamp_id
    ON transactions (account_id, timestamp DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_accounts_owner_created_id
    ON accounts (owner_user_id, created_at DESC, id DESC);
