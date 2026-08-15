-- V10: sequence for SAP-like order numbers (avoids TOCTOU on exists-then-insert)

CREATE SEQUENCE IF NOT EXISTS erp_mock.order_number_seq AS BIGINT INCREMENT BY 1;

-- Advance past any existing numeric order_number (seed + portal inserts)
SELECT setval(
    'erp_mock.order_number_seq',
    (
        SELECT COALESCE(MAX(order_number::bigint), 4500010000)
        FROM erp_mock.orders
        WHERE order_number ~ '^[0-9]+$'
    )
);

COMMENT ON SEQUENCE erp_mock.order_number_seq IS
    'Allocates unique erp_mock.orders.order_number values (nextval → string).';
