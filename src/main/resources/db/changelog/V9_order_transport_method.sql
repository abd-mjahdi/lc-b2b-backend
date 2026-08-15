-- V9: persist transport_method on erp_mock.orders (required on CreateOrderRequestDto)

ALTER TABLE erp_mock.orders
    ADD COLUMN IF NOT EXISTS transport_method VARCHAR(50);

COMMENT ON COLUMN erp_mock.orders.transport_method IS
    'Mode de transport choisi à la soumission portail (camion, etc.).';
