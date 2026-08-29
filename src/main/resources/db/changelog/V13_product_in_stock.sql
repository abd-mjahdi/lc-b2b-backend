-- V13: catalog availability (out of stock) distinct from withdraw/hide (is_active).

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS in_stock BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN app.products.in_stock IS
    'When false, the SKU stays visible in the catalog but cannot be ordered or sampled.';
