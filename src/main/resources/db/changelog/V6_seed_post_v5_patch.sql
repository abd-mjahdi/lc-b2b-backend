-- ============================================================================
-- V6_seed_post_v5_patch.sql — Lesieur Cristal B2B Portal
-- Idempotent patch for databases where V3_seed_data ran BEFORE V4/V5 existed.
-- New installs get correct data from V3 directly; this only backfills gaps.
-- ============================================================================

BEGIN;

-- Catalogue enrichi (colonnes ajoutées en V5)
UPDATE app.products SET
    is_sampleable       = true,
    max_sample_quantity = v.max_qty,
    unit_price          = v.price,
    sales_unit          = 'CAR',
    image_url           = v.img,
    updated_at          = now()
FROM (VALUES
    ('HTO-001', 24.000, 144.00, '/static/products/huile-tournesol-1L.jpg'),
    ('HTO-005', 16.000, 220.00, '/static/products/huile-tournesol-5L.jpg'),
    ('HOL-001', 24.000, 780.00, '/static/products/huile-olive-1L.jpg'),
    ('HME-002', 12.000, 240.00, '/static/products/huile-melange-2L.jpg'),
    ('MAR-250', 48.000, 192.00, '/static/products/margarine-250g.jpg'),
    ('SAV-500', 40.000, 300.00, '/static/products/savon-marseille-500g.jpg'),
    ('HTC-002', 12.000, 420.00, '/static/products/al-horra-2L.jpg'),
    ('SAV-300', 60.000, 180.00, '/static/products/savon-olive-300g.jpg')
) AS v(code, max_qty, price, img)
WHERE app.products.code = v.code
  AND (app.products.is_sampleable IS NOT true OR app.products.unit_price IS NULL);

-- Échantillons : FK produit + commande groupée (V5)
UPDATE app.sample_requests
SET product_code = 'HME-002',
    linked_order_number = '4500010005'
WHERE resulting_order_number = '4500010005'
  AND product_code IS NULL;

UPDATE app.sample_requests
SET product_code = 'SAV-500'
WHERE product_type ILIKE '%Savon%'
  AND product_code IS NULL;

-- Contestation de démo si la table V5 est vide
INSERT INTO app.invoice_disputes
    (invoice_number, customer_number, user_id, reason, description, status, created_at)
SELECT
    '900010002',
    'CUST0001',
    (SELECT id FROM app.users WHERE login = 'aalami' LIMIT 1),
    'QUANTITY_DISCREPANCY',
    'Écart constaté entre quantité facturée et bon de livraison pour la commande 4500010002.',
    'PENDING',
    '2026-07-02 10:00:00+01'
WHERE NOT EXISTS (SELECT 1 FROM app.invoice_disputes)
  AND EXISTS (SELECT 1 FROM erp_mock.invoices WHERE invoice_number = '900010002');

UPDATE erp_mock.invoices
SET invoice_status = 'disputed'
WHERE invoice_number = '900010002'
  AND EXISTS (SELECT 1 FROM app.invoice_disputes WHERE invoice_number = '900010002' AND status = 'PENDING');

-- Notification admin de démo si aucune notification broadcast
INSERT INTO app.notifications
    (recipient_user_id, recipient_role, title, message, type, related_entity_type, related_entity_id, target_url, is_read, created_at)
SELECT
    NULL,
    'ADMIN',
    'Contestation de facture reçue',
    'Facture #900010002 contestée par Épicerie Al Amal SARL (Client #CUST0001). Motif : Écart de quantité.',
    'DISPUTE_OPENED',
    'INVOICE_DISPUTE',
    (SELECT id::text FROM app.invoice_disputes WHERE invoice_number = '900010002' LIMIT 1),
    '/admin/disputes',
    false,
    '2026-07-02 10:00:00+01'
WHERE NOT EXISTS (
    SELECT 1 FROM app.notifications
    WHERE recipient_role = 'ADMIN' AND recipient_user_id IS NULL
);

COMMIT;
