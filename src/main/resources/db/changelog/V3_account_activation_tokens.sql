-- One-time activation links for admin-provisioned client accounts.

CREATE TABLE app.account_activation_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_activation_tokens_user_id ON app.account_activation_tokens(user_id);
CREATE INDEX idx_account_activation_tokens_expires_at ON app.account_activation_tokens(expires_at)
    WHERE used_at IS NULL;

COMMENT ON TABLE app.account_activation_tokens IS
    'Hashed one-time tokens for client self-service password setup after admin provisioning.';
