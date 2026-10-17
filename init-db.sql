-- Initialize database schema for VaultStream
-- This file is executed on container startup

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(34) NOT NULL UNIQUE,
    account_holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    reference_id VARCHAR(100),
    description VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for transactions table
CREATE INDEX IF NOT EXISTS idx_transaction_account_id ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_transaction_type ON transactions(type);

-- Create idempotency keys table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(255) NOT NULL UNIQUE,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_parameters TEXT,
    response_code INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- Create indexes for idempotency table
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys(key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_keys(expires_at);