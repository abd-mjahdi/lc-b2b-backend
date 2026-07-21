-- ============================================================================
-- V2__seed_data.sql — Lesieur Cristal B2B Portal
-- Logical, referentially-consistent seed data for erp_mock + app schemas.
-- Safe to re-run in dev: wipes existing rows first.
-- ============================================================================

BEGIN;

TRUNCATE TABLE
    app.certificates,
    app.static_documents,
    app.documents,
    app.appointment_requests,
    app.contact_messages,
    app.purchase_order_submissions,
    app.sample_requests,
    app.quotation_requests,
    app.reclamations,
    app.client_change_log,
    app.users,
    app.products,
    erp_mock.order_status,
    erp_mock.invoices,
    erp_mock.orders,
    erp_mock.customers
RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------------------
-- 1. erp_mock.customers  (simulated SAP master data — bulk-synced once)
-- ----------------------------------------------------------------------------
INSERT INTO erp_mock.customers
    (customer_number, company_name, postal_address, city, country, phone, email, vat_id, created_at, updated_at)
VALUES
    ('CUST0001', 'Épicerie Al Amal SARL',      '12 Rue Ibn Batouta, Quartier Maarif',              'Casablanca', 'Maroc', '+212522334455', 'contact@alamal-epicerie.ma',      'IF10234567', '2026-01-15 08:00:00+01', '2026-04-15 09:30:00+01'),
    ('CUST0002', 'Superdiscount Maroc SA',      '45 Avenue Hassan II',                               'Rabat',      'Maroc', '+212537445566', 'achats@superdiscount.ma',         'IF20345678', '2026-01-15 08:00:00+01', '2026-03-10 11:00:00+01'),
    ('CUST0003', 'Groupe Marjane Holding',      'Zone Industrielle Sidi Ghanem, Route de Safi',     'Marrakech',  'Maroc', '+212524556677', 'approvisionnement@marjaneholding.ma', 'IF30456789', '2026-01-15 08:00:00+01', '2026-01-15 08:00:00+01'),
    ('CUST0004', 'Boulangerie Ahlan SARL',      'Route de Tétouan, Km 3',                           'Tanger',     'Maroc', '+212539667788', 'commandes@ahlan-boulangerie.ma',  'IF40567890', '2026-01-15 08:00:00+01', '2026-04-02 14:00:00+01'),
    ('CUST0005', 'Cash Food Distribution SARL', 'Route d''Immouzer, Zone Commerciale',              'Fès',        'Maroc', '+212535778899', 'contact@cashfood.ma',             'IF50678901', '2026-01-15 08:00:00+01', '2026-01-15 08:00:00+01');

-- ----------------------------------------------------------------------------
-- 2. app.products  (shared catalog)
-- ----------------------------------------------------------------------------
INSERT INTO app.products
    (code, name, category, description, created_at, updated_at)
VALUES
    ('HTO-001', 'Huile de tournesol Lesieur 1L',              'Huiles de table', 'Carton de 12 bouteilles de 1L.',            '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01'),
    ('HTO-005', 'Huile de tournesol Lesieur 5L',               'Huiles de table', 'Carton de 4 bidons de 5L.',                 '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01'),
    ('HOL-001', 'Huile d''olive Lesieur Extra Vierge 1L',      'Huiles d''olive', 'Carton de 12 bouteilles de 1L.',            '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01'),
    ('HME-002', 'Huile de mélange Lesieur 2L',                 'Huiles de table', 'Carton de 6 bidons de 2L.',                 '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01'),
    ('MAR-250', 'Margarine Lesieur 250g',                      'Margarines',      'Carton de 24 unités de 250g.',              '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01'),
    ('SAV-500', 'Savon de Marseille Lesieur 500g',             'Savons',          'Carton de 20 unités de 500g.',              '2026-01-10 08:00:00+01', '2026-01-10 08:00:00+01');

-- ----------------------------------------------------------------------------
-- 3. app.users  (2 admins provisioned pre-launch, 5 client accounts)
--    password_hash values are placeholders, NOT real bcrypt hashes.
-- ----------------------------------------------------------------------------
INSERT INTO app.users
    (customer_number, last_name, first_name, email, phone, login, password_hash, role, language, is_active, created_at, updated_at)
VALUES
    (NULL,       'Bakkali',   'Nadia',   'nadia.bakkali@lesieurcristal-portal.ma', '+212666667788', 'admin.portal',   '$2a$10$placeholderHashAdmin0000000000000000', 'ADMIN',  'fr', true, '2025-12-01 09:00:00+01', '2025-12-01 09:00:00+01'),
    (NULL,       'Fassi',     'Omar',    'omar.fassi@lesieurcristal-portal.ma',    '+212667778899', 'support.lesieur','$2a$10$placeholderHashAdmin0000000000000001', 'ADMIN',  'fr', true, '2025-12-01 09:15:00+01', '2025-12-01 09:15:00+01'),
    ('CUST0001', 'Alami',     'Amine',   'amine.alami@alamal-epicerie.ma',         '+212661112233', 'aalami',         '$2a$10$placeholderHashClient000000000000000', 'CLIENT', 'fr', true, '2026-01-20 10:00:00+01', '2026-01-20 10:00:00+01'),
    ('CUST0002', 'Bennani',   'Khadija', 'khadija.bennani@superdiscount.ma',       '+212662223344', 'kbennani',       '$2a$10$placeholderHashClient000000000000001', 'CLIENT', 'fr', true, '2026-01-22 10:00:00+01', '2026-01-22 10:00:00+01'),
    ('CUST0003', 'Idrissi',   'Youssef', 'youssef.idrissi@marjaneholding.ma',      '+212663334455', 'yidrissi',       '$2a$10$placeholderHashClient000000000000002', 'CLIENT', 'fr', true, '2026-01-25 10:00:00+01', '2026-01-25 10:00:00+01'),
    ('CUST0004', 'Chraibi',   'Hicham',  'hicham.chraibi@ahlan-boulangerie.ma',    '+212664445566', 'hchraibi',       '$2a$10$placeholderHashClient000000000000003', 'CLIENT', 'fr', true, '2026-02-02 10:00:00+01', '2026-02-02 10:00:00+01'),
    ('CUST0005', 'Moussaoui', 'Salma',   'salma.moussaoui@cashfood.ma',            '+212665556677', 'smoussaoui',     '$2a$10$placeholderHashClient000000000000004', 'CLIENT', 'fr', true, '2026-02-05 10:00:00+01', '2026-02-05 10:00:00+01');

-- ----------------------------------------------------------------------------
-- 4. erp_mock.orders  (invoice_number left NULL — backfilled after invoices)
-- ----------------------------------------------------------------------------
INSERT INTO erp_mock.orders
    (order_number, order_date, customer_number, customer_order_reference, product_code, product_label,
     quantity_ordered, quantity_shipped, sales_unit, net_amount, currency,
     ship_to_city, ship_to_country, requested_delivery_date, planned_delivery_date, goods_issue_date,
     created_at, updated_at)
VALUES
    ('4500010001', '2026-05-05', 'CUST0001', 'BC-AA-0512', 'HTO-001', 'Huile de tournesol Lesieur 1L', 500, 500, 'CAR', 72000.00,  'MAD', 'Casablanca', 'Maroc', '2026-05-12', '2026-05-11', '2026-05-11', '2026-05-05 09:00:00+01', '2026-05-11 17:00:00+01'),
    ('4500010002', '2026-05-12', 'CUST0001', 'BC-AA-0519', 'HOL-001', 'Huile d''olive Lesieur Extra Vierge 1L', 200, 200, 'CAR', 156000.00, 'MAD', 'Casablanca', 'Maroc', '2026-05-19', '2026-05-18', '2026-05-18', '2026-05-12 09:15:00+01', '2026-05-18 16:30:00+01'),
    ('4500010003', '2026-06-02', 'CUST0002', 'PO-SD-2233', 'HTO-005', 'Huile de tournesol Lesieur 5L', 300, 150, 'CAR', 66000.00,  'MAD', 'Rabat', 'Maroc', '2026-06-16', '2026-06-15', '2026-06-20', '2026-06-02 10:00:00+01', '2026-06-20 12:00:00+01'),
    ('4500010004', '2026-06-01', 'CUST0003', 'MJ-2026-091', 'MAR-250', 'Margarine Lesieur 250g', 1000, 1000, 'CAR', 192000.00, 'MAD', 'Marrakech', 'Maroc', '2026-06-08', '2026-06-07', '2026-06-07', '2026-06-01 08:45:00+01', '2026-06-07 15:00:00+01'),
    ('4500010005', '2026-06-03', 'CUST0003', 'MJ-2026-094', 'HME-002', 'Huile de mélange Lesieur 2L', 400, 0,   'CAR', 96000.00,  'MAD', 'Marrakech', 'Maroc', '2026-07-01', '2026-06-30', NULL,         '2026-06-03 09:30:00+01', '2026-06-03 09:30:00+01'),
    ('4500010006', '2026-06-10', 'CUST0004', 'AH-0610-B',  'HTO-001', 'Huile de tournesol Lesieur 1L', 600, 600, 'CAR', 86400.00,  'MAD', 'Tanger', 'Maroc', '2026-06-17', '2026-06-16', '2026-06-16', '2026-06-10 11:00:00+01', '2026-06-16 14:00:00+01'),
    ('4500010007', '2026-06-15', 'CUST0005', 'CF-2026-451', 'SAV-500', 'Savon de Marseille Lesieur 500g', 250, 250, 'CAR', 75000.00,  'MAD', 'Fès', 'Maroc', '2026-06-22', '2026-06-21', '2026-06-21', '2026-06-15 10:20:00+01', '2026-06-21 13:00:00+01'),
    ('4500010008', '2026-07-01', 'CUST0002', 'PO-SD-2299', 'HOL-001', 'Huile d''olive Lesieur Extra Vierge 1L', 150, 0, 'CAR', 117000.00, 'MAD', 'Rabat', 'Maroc', '2026-07-10', NULL, NULL, '2026-07-01 09:00:00+01', '2026-07-03 10:00:00+01');

-- ----------------------------------------------------------------------------
-- 5. erp_mock.invoices  (orders 4500010005 and 4500010008 not invoiced yet)
-- ----------------------------------------------------------------------------
INSERT INTO erp_mock.invoices
    (invoice_number, invoice_date, order_number, customer_number, net_amount, vat_amount, total_amount,
     currency, due_date, invoice_status, payment_date, created_at, updated_at)
VALUES
    ('900010001', '2026-05-13', '4500010001', 'CUST0001', 72000.00, 7200.00,  79200.00,  'MAD', '2026-06-12', 'paid',            '2026-06-05', '2026-05-13 09:00:00+01', '2026-06-05 10:00:00+01'),
    ('900010002', '2026-05-20', '4500010002', 'CUST0001', 156000.00, 15600.00, 171600.00, 'MAD', '2026-06-19', 'unpaid',          NULL,         '2026-05-20 09:00:00+01', '2026-05-20 09:00:00+01'),
    ('900010003', '2026-06-21', '4500010003', 'CUST0002', 33000.00,  3300.00,  36300.00,  'MAD', '2026-07-21', 'partially_paid',  NULL,         '2026-06-21 09:00:00+01', '2026-07-01 12:00:00+01'),
    ('900010004', '2026-06-08', '4500010004', 'CUST0003', 192000.00, 19200.00, 211200.00, 'MAD', '2026-07-08', 'paid',            '2026-06-30', '2026-06-08 09:00:00+01', '2026-06-30 09:00:00+01'),
    ('900010005', '2026-06-17', '4500010006', 'CUST0004', 86400.00,  8640.00,  95040.00,  'MAD', '2026-07-17', 'unpaid',          NULL,         '2026-06-17 09:00:00+01', '2026-06-17 09:00:00+01'),
    ('900010006', '2026-06-22', '4500010007', 'CUST0005', 75000.00,  7500.00,  82500.00,  'MAD', '2026-07-22', 'unpaid',          NULL,         '2026-06-22 09:00:00+01', '2026-06-22 09:00:00+01');

-- Backfill the deferred FK on orders now that invoices exist.
UPDATE erp_mock.orders SET invoice_number = '900010001' WHERE order_number = '4500010001';
UPDATE erp_mock.orders SET invoice_number = '900010002' WHERE order_number = '4500010002';
UPDATE erp_mock.orders SET invoice_number = '900010003' WHERE order_number = '4500010003';
UPDATE erp_mock.orders SET invoice_number = '900010004' WHERE order_number = '4500010004';
UPDATE erp_mock.orders SET invoice_number = '900010005' WHERE order_number = '4500010006';
UPDATE erp_mock.orders SET invoice_number = '900010006' WHERE order_number = '4500010007';

-- ----------------------------------------------------------------------------
-- 6. erp_mock.order_status  (one row per order, full status spread)
-- ----------------------------------------------------------------------------
INSERT INTO erp_mock.order_status
    (order_number, current_status, status_updated_at, expected_delivery_date, carrier_name, carrier_reference)
VALUES
    ('4500010001', 'delivered',     '2026-05-11 18:00:00+01', '2026-05-12', 'CTM Fret Maroc',  'CTM-2026-88213'),
    ('4500010002', 'shipped',       '2026-05-18 17:00:00+01', '2026-05-19', 'CTM Fret Maroc',  'CTM-2026-88240'),
    ('4500010003', 'in_preparation','2026-06-20 12:30:00+01', '2026-06-16', NULL,               NULL),
    ('4500010004', 'delivered',     '2026-06-07 16:00:00+01', '2026-06-08', 'Timar Logistique','TML-2026-04471'),
    ('4500010005', 'confirmed',     '2026-06-03 09:30:00+01', '2026-07-01', NULL,               NULL),
    ('4500010006', 'delivered',     '2026-06-16 15:00:00+01', '2026-06-17', 'CTM Fret Maroc',  'CTM-2026-88301'),
    ('4500010007', 'delivered',     '2026-06-21 14:00:00+01', '2026-06-22', 'Timar Logistique','TML-2026-04502'),
    ('4500010008', 'cancelled',     '2026-07-03 10:00:00+01', NULL,        NULL,               NULL);

-- ----------------------------------------------------------------------------
-- 7. app.client_change_log
-- ----------------------------------------------------------------------------
INSERT INTO app.client_change_log
    (customer_number, changed_by_user_id, field_name, old_value, new_value, changed_at)
VALUES
    ('CUST0002', (SELECT id FROM app.users WHERE login = 'admin.portal'),   'company_name', 'Superdiscount Maroc',   'Superdiscount Maroc SA', '2026-03-10 11:00:00+01'),
    ('CUST0004', (SELECT id FROM app.users WHERE login = 'support.lesieur'),'phone',        '+212539660000',         '+212539667788',          '2026-04-02 14:00:00+01'),
    ('CUST0001', (SELECT id FROM app.users WHERE login = 'admin.portal'),   'email',        'ancien-contact@alamal-epicerie.ma', 'contact@alamal-epicerie.ma', '2026-04-15 09:30:00+01');

-- ----------------------------------------------------------------------------
-- 8. app.reclamations
-- ----------------------------------------------------------------------------
INSERT INTO app.reclamations
    (customer_number, user_id, lot_number, description, attachment_path, status, received_at)
VALUES
    ('CUST0001', (SELECT id FROM app.users WHERE login = 'aalami'),     'L2026050501', 'Emballage carton endommagé constaté à la réception du lot L2026050501.', '/uploads/reclamations/cust0001_photo1.jpg', 'resolved',    '2026-05-14 11:00:00+01'),
    ('CUST0003', NULL,                                                   'L2026060701', 'Odeur inhabituelle détectée sur le lot de margarine reçu.',              NULL,                                          'in_progress', '2026-07-08 09:30:00+01'),
    ('CUST0005', (SELECT id FROM app.users WHERE login = 'smoussaoui'), 'L2026062001', 'Quantité livrée inférieure de 10 cartons par rapport au bon de commande.', NULL,                                          'new',         '2026-07-15 14:20:00+01');

-- ----------------------------------------------------------------------------
-- 9. app.quotation_requests
-- ----------------------------------------------------------------------------
INSERT INTO app.quotation_requests
    (customer_number, user_id, product_code, quantity, transport_method, details, status, requested_at)
VALUES
    ('CUST0002', (SELECT id FROM app.users WHERE login = 'kbennani'), 'HTO-005', 1000, 'Transport routier - franco de port', 'Demande de tarif pour approvisionnement trimestriel.', 'answered',   '2026-06-05 09:00:00+01'),
    ('CUST0004', (SELECT id FROM app.users WHERE login = 'hchraibi'), 'MAR-250', 2000, NULL, NULL,                                                                                'new',        '2026-07-10 16:45:00+01'),
    ('CUST0005', NULL,                                                 'SAV-500', 500,  NULL, 'Ouverture éventuelle d''une nouvelle ligne de produit.',                            'processing', '2026-07-05 10:15:00+01');

-- ----------------------------------------------------------------------------
-- 10. app.sample_requests
-- ----------------------------------------------------------------------------
INSERT INTO app.sample_requests
    (customer_number, user_id, product_type, quantity, contact_name, contact_address, status, resulting_order_number, requested_at)
VALUES
    ('CUST0003', (SELECT id FROM app.users WHERE login = 'yidrissi'), 'Huile de mélange conditionnée 2L', 5, 'Youssef Idrissi', 'Zone Industrielle Sidi Ghanem, Marrakech', 'fulfilled', '4500010005', '2026-05-20 08:30:00+01'),
    ('CUST0004', (SELECT id FROM app.users WHERE login = 'hchraibi'), 'Savon de Marseille 500g',          3, 'Hicham Chraibi',  'Route de Tétouan, Tanger',                 'new',       NULL,         '2026-07-18 12:00:00+01');

-- ----------------------------------------------------------------------------
-- 11. app.purchase_order_submissions
-- ----------------------------------------------------------------------------
INSERT INTO app.purchase_order_submissions
    (customer_number, user_id, file_path, note, status, submitted_at)
VALUES
    ('CUST0001', (SELECT id FROM app.users WHERE login = 'aalami'),   '/uploads/po/cust0001_bc_20260505.pdf', 'Bon de commande signé pour la commande de mai.', 'processed', '2026-05-04 09:10:00+01'),
    ('CUST0002', (SELECT id FROM app.users WHERE login = 'kbennani'), '/uploads/po/cust0002_bc_20260701.pdf', NULL,                                              'new',       '2026-07-01 15:30:00+01');

-- ----------------------------------------------------------------------------
-- 12. app.contact_messages
-- ----------------------------------------------------------------------------
INSERT INTO app.contact_messages
    (customer_number, full_name, email, subject, message, status, submitted_at)
VALUES
    (NULL,       'Karim Tazi', 'karim.tazi@example.com', 'Demande de documentation produit', 'Bonjour, je souhaiterais recevoir la fiche technique de vos huiles de table en vue d''un référencement. Cordialement.', 'processed', '2026-03-22 10:00:00+01'),
    ('CUST0005', NULL,         NULL,                     'Question facturation',             'Pouvez-vous vérifier le montant de la facture 900010006, il ne correspond pas à notre bon de commande ?',            'new',       '2026-07-19 09:45:00+01');

-- ----------------------------------------------------------------------------
-- 13. app.appointment_requests
-- ----------------------------------------------------------------------------
INSERT INTO app.appointment_requests
    (customer_number, user_id, subject, requested_date, requested_time_slot, status, confirmed_at, created_at)
VALUES
    ('CUST0002', (SELECT id FROM app.users WHERE login = 'kbennani'), 'Visite commerciale trimestrielle', '2026-08-05', '10:00 - 11:00', 'confirmed', '2026-07-12 14:00:00+01', '2026-07-10 08:20:00+01'),
    ('CUST0004', (SELECT id FROM app.users WHERE login = 'hchraibi'), 'Audit qualité fournisseur',         '2026-08-20', '14:00 - 16:00', 'pending',   NULL,                     '2026-07-17 11:00:00+01');

-- ----------------------------------------------------------------------------
-- 14. app.documents
-- ----------------------------------------------------------------------------
INSERT INTO app.documents
    (customer_number, doc_type, title, natural_key, status, file_path, related_reference, created_at, updated_at)
VALUES
    ('CUST0001', 'invoice',     'Facture 900010001',                    '900010001',                              'ready',      '/documents/invoices/900010001.pdf',                    '4500010001', '2026-05-13 09:00:00+01', '2026-05-13 09:00:00+01'),
    ('CUST0001', 'invoice',     'Facture 900010002',                    '900010002',                              'ready',      '/documents/invoices/900010002.pdf',                    '4500010002', '2026-05-20 09:00:00+01', '2026-05-20 09:00:00+01'),
    ('CUST0002', 'invoice',     'Facture 900010003',                    '900010003',                              'ready',      '/documents/invoices/900010003.pdf',                    '4500010003', '2026-06-21 09:00:00+01', '2026-06-21 09:00:00+01'),
    ('CUST0003', 'invoice',     'Facture 900010004',                    '900010004',                              'ready',      '/documents/invoices/900010004.pdf',                    '4500010004', '2026-06-08 09:00:00+01', '2026-06-08 09:00:00+01'),
    ('CUST0004', 'invoice',     'Facture 900010005',                    '900010005',                              'processing', NULL,                                                    '4500010006', '2026-07-20 09:00:00+01', '2026-07-20 09:00:00+01'),
    ('CUST0003', 'certificate', 'Certificat d''analyse - Lot L2026060101', 'CUST0003:L2026060101:analysis',         'ready',      '/documents/certificates/cust0003_L2026060101_analysis.pdf', 'L2026060101', '2026-06-25 09:00:00+01', '2026-06-25 09:00:00+01');

-- ----------------------------------------------------------------------------
-- 15. app.static_documents
-- ----------------------------------------------------------------------------
INSERT INTO app.static_documents
    (title, product_category, file_path, active, created_at)
VALUES
    ('Fiche technique - Huile de tournesol Lesieur', 'Huiles de table', '/static/fiches/huile-tournesol.pdf', true,  '2026-01-10 08:00:00+01'),
    ('Brochure institutionnelle Lesieur Cristal 2026', NULL,            '/static/brochures/brochure-2026.pdf', true,  '2026-01-10 08:05:00+01'),
    ('Catalogue produits 2024 (archivé)',              NULL,            '/static/catalogues/catalogue-2024.pdf', false, '2024-11-01 08:00:00+01');

-- ----------------------------------------------------------------------------
-- 16. app.certificates
-- ----------------------------------------------------------------------------
INSERT INTO app.certificates
    (customer_number, certificate_type, lot_number, product_code, issue_date, technical_data, document_id, created_at)
VALUES
    ('CUST0003', 'analysis', 'L2026060101', 'HME-002',
     '2026-06-24',
     '{"acidity_index": 0.15, "peroxide_value": 2.3, "lab": "Laboratoire Central Lesieur Cristal"}'::jsonb,
     (SELECT id FROM app.documents WHERE natural_key = 'CUST0003:L2026060101:analysis'),
     '2026-06-24 15:00:00+01'),
    ('CUST0001', 'origin', 'L2026050501', 'HTO-001',
     '2026-05-10',
     '{"country_of_origin": "Maroc", "hs_code": "1512.19"}'::jsonb,
     NULL,
     '2026-05-10 09:00:00+01');

COMMIT;