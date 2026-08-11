-- ============================================================================
-- V5__notifications_and_invoice_disputes.sql — Lesieur Cristal B2B Portal
-- Ajout des tables de notifications contextuelles, des contestations de
-- factures, et adaptation de la table sample_requests pour le nouveau
-- catalogue produits (PRD §4.1).
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. app.sample_requests — ajout des colonnes produit + liaison commande
-- ----------------------------------------------------------------------------
ALTER TABLE app.sample_requests
    ADD COLUMN IF NOT EXISTS product_code VARCHAR(30)
        REFERENCES app.products(code);

ALTER TABLE app.sample_requests
    ADD COLUMN IF NOT EXISTS linked_order_number VARCHAR(20)
        REFERENCES erp_mock.orders(order_number);

-- product_type devient nullable (l'ancien champ texte est remplacé par
-- la vraie FK vers app.products). On conserve la colonne pour rétro-compat.
ALTER TABLE app.sample_requests
    ALTER COLUMN product_type DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sample_requests_product_code
    ON app.sample_requests(product_code);

CREATE INDEX IF NOT EXISTS idx_sample_requests_linked_order_number
    ON app.sample_requests(linked_order_number);


-- ----------------------------------------------------------------------------
-- 2. app.products — colonnes manquantes pour le catalogue enrichi
-- ----------------------------------------------------------------------------
ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS is_sampleable BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS max_sample_quantity NUMERIC(14,3);

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(14,2);

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS sales_unit VARCHAR(10);

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);


-- ----------------------------------------------------------------------------
-- 3. app.notifications — table de notifications contextuelles
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app.notifications (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_user_id  BIGINT REFERENCES app.users(id),
    -- NULL si destiné à tous les administrateurs
    recipient_role     VARCHAR(20) NOT NULL CHECK (recipient_role IN ('CLIENT','ADMIN')),
    title              VARCHAR(150) NOT NULL,
    message            TEXT NOT NULL,
    type               VARCHAR(50) NOT NULL,
    -- ORDER_CREATED, SAMPLE_REQUESTED, STATUS_CHANGED, DISPUTE_OPENED, DISPUTE_RESOLVED, INVOICE_CREATED, etc.
    related_entity_type VARCHAR(50),
    related_entity_id   VARCHAR(50),
    target_url         VARCHAR(255),
    is_read            BOOLEAN NOT NULL DEFAULT false,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user
    ON app.notifications(recipient_user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_role
    ON app.notifications(recipient_role);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON app.notifications(recipient_user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON app.notifications(created_at DESC);


-- ----------------------------------------------------------------------------
-- 4. app.invoice_disputes — contestations de factures
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app.invoice_disputes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_number  VARCHAR(20) NOT NULL REFERENCES erp_mock.invoices(invoice_number),
    customer_number VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id         BIGINT REFERENCES app.users(id),
    reason          VARCHAR(50) NOT NULL
                    CHECK (reason IN ('QUANTITY_DISCREPANCY','PRICE_DISCREPANCY','DAMAGED_GOODS','OTHER')),
    description     TEXT NOT NULL,
    file_path       VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    resolution_note TEXT,
    resolved_by_user_id BIGINT REFERENCES app.users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_invoice_disputes_invoice_number
    ON app.invoice_disputes(invoice_number);

CREATE INDEX IF NOT EXISTS idx_invoice_disputes_customer_number
    ON app.invoice_disputes(customer_number);

CREATE INDEX IF NOT EXISTS idx_invoice_disputes_status
    ON app.invoice_disputes(status);


-- ----------------------------------------------------------------------------
-- 5. erp_mock.invoices — ajout d'un statut 'disputed' pour les factures
--    contestées (état transitoire avant résolution par l'admin).
-- ----------------------------------------------------------------------------
ALTER TABLE erp_mock.invoices
    DROP CONSTRAINT IF EXISTS invoices_invoice_status_check;

ALTER TABLE erp_mock.invoices
    ADD CONSTRAINT invoices_invoice_status_check
    CHECK (invoice_status IN ('paid','unpaid','partially_paid','disputed'));

COMMIT;
