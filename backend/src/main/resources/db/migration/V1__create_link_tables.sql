CREATE TABLE guests (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_guests_token_hash UNIQUE (token_hash)
);

CREATE TABLE short_urls (
    id UUID PRIMARY KEY,
    guest_id UUID NOT NULL REFERENCES guests (id),
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(32) NOT NULL,
    custom_alias VARCHAR(32),
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_short_urls_code UNIQUE (short_code),
    CONSTRAINT ck_short_urls_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_short_urls_code_lower ON short_urls (lower(short_code));
CREATE INDEX idx_short_urls_guest_created ON short_urls (guest_id, created_at DESC);

CREATE TABLE click_events (
    id UUID PRIMARY KEY,
    short_url_id UUID NOT NULL REFERENCES short_urls (id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL,
    referrer VARCHAR(255),
    client_category VARCHAR(16) NOT NULL,
    CONSTRAINT ck_click_category CHECK (client_category IN ('DESKTOP', 'MOBILE', 'TABLET', 'BOT', 'UNKNOWN'))
);

CREATE INDEX idx_click_events_url_time ON click_events (short_url_id, clicked_at DESC);
