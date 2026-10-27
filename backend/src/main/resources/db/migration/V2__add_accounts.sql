CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_sessions_token UNIQUE (token_hash)
);

CREATE INDEX idx_user_sessions_user ON user_sessions (user_id);

ALTER TABLE short_urls ALTER COLUMN guest_id DROP NOT NULL;
ALTER TABLE short_urls ADD COLUMN owner_id UUID REFERENCES users (id);
ALTER TABLE short_urls ADD CONSTRAINT ck_short_urls_owner CHECK (guest_id IS NOT NULL OR owner_id IS NOT NULL);

CREATE INDEX idx_short_urls_owner_created ON short_urls (owner_id, created_at DESC);
