ALTER TABLE accounts
    ADD COLUMN owner_user_id UUID;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_owner_user
    FOREIGN KEY (owner_user_id) REFERENCES app_users(id);

CREATE INDEX idx_accounts_owner_user_id ON accounts(owner_user_id);
