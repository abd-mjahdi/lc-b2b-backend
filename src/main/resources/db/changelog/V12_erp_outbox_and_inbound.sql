-- V12: ERP integration seam (outbound outbox + inbound sync cursor).
-- Portal keeps talking to connectors; erp_mock is today's SAP stand-in.

CREATE TABLE app.erp_outbox (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    direction       VARCHAR(20)  NOT NULL
                    CHECK (direction IN ('OUTBOUND', 'INBOUND')),
    event_type      VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(50),
    payload         JSONB        NOT NULL,
    result_payload  JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    last_error      TEXT,
    processed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_erp_outbox_retry
    ON app.erp_outbox (status, created_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_erp_outbox_aggregate
    ON app.erp_outbox (aggregate_id, event_type);

CREATE TRIGGER trg_erp_outbox_updated_at
    BEFORE UPDATE ON app.erp_outbox
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

COMMENT ON TABLE app.erp_outbox IS
    'Queue of ERP messages. OUTBOUND = portal → SAP; INBOUND = SAP → portal (audit of pulled updates).';

CREATE TABLE app.erp_sync_state (
    id               VARCHAR(40) PRIMARY KEY,
    last_run_at      TIMESTAMPTZ,
    last_success_at  TIMESTAMPTZ,
    last_cursor      TIMESTAMPTZ,
    last_error       TEXT,
    records_pulled   INTEGER NOT NULL DEFAULT 0
);

INSERT INTO app.erp_sync_state (id, records_pulled)
VALUES ('inbound', 0);

COMMENT ON TABLE app.erp_sync_state IS
    'Cursor for inbound ERP pull. Mock: last order_status.status_updated_at seen.';
