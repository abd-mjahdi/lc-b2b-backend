-- V7: order groups (multi-line submissions) + dispute previous invoice status
-- ============================================================================
-- 1. erp_mock.orders.order_group_id — links sibling lines from one portal submission
-- 2. app.invoice_disputes.previous_invoice_status — restore payment state after arbitration

ALTER TABLE erp_mock.orders
    ADD COLUMN IF NOT EXISTS order_group_id VARCHAR(40);

-- Existing rows: each line is its own group (single-line historical data)
UPDATE erp_mock.orders
SET order_group_id = order_number
WHERE order_group_id IS NULL;

ALTER TABLE erp_mock.orders
    ALTER COLUMN order_group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_order_group_id
    ON erp_mock.orders(order_group_id);

CREATE INDEX IF NOT EXISTS idx_orders_customer_group
    ON erp_mock.orders(customer_number, order_group_id);

-- Note: customer_order_reference is shared by all lines of a group — uniqueness
-- across submissions is enforced in OrderService (not a UNIQUE INDEX).

ALTER TABLE app.invoice_disputes
    ADD COLUMN IF NOT EXISTS previous_invoice_status VARCHAR(20);

COMMENT ON COLUMN erp_mock.orders.order_group_id IS
    'Identifiant de regroupement des lignes d''une même soumission portail (plusieurs order_number, un seul groupe).';

COMMENT ON COLUMN app.invoice_disputes.previous_invoice_status IS
    'Statut erp_mock.invoices.invoice_status avant passage à disputed — restauré à la résolution.';
