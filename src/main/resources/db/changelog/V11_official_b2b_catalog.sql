-- V11: official B2B catalog (Lesieur Cristal public product pages).
-- Demo SKUs stay in app.products for historical FKs but are hidden from the catalog.

ALTER TABLE app.products
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN app.products.is_active IS
    'When false, the SKU remains for historical orders/samples but is hidden from the sellable catalog.';

UPDATE app.products
SET is_active = false
WHERE code IN (
    'HTO-001', 'HTO-005', 'HOL-001', 'HME-002',
    'MAR-250', 'SAV-500', 'HTC-002', 'SAV-300'
);

-- Prices are catalog estimates in MAD (the public site does not publish tariffs):
-- huiles = carton of 4 x 5 L bidons; tourteaux = 25 kg bag.
INSERT INTO app.products (
    code, name, category, description,
    is_sampleable, max_sample_quantity, unit_price, sales_unit, image_url, is_active
) VALUES
    (
        'HSO-001',
        'Huile de soja',
        'Huiles',
        'L''huile de soja est la 2ème huile la plus consommée au monde derrière celle de palme. Elle se distingue par son excellent apport en acides gras essentiels Oméga 3 et Oméga 6 (plus de 50%). L''huile de soja raffinée est utilisée en conserverie de poisson, margarinerie, charcuterie, biscuiterie et production de condiments. Usage polyvalent, limité à hautes températures. Carton de 4 bidons de 5 L.',
        true, 2.000, 290.00, 'CAR',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Huile-de-Soja-768x812.png',
        true
    ),
    (
        'HTN-001',
        'Huile de tournesol',
        'Huiles',
        'L''huile de tournesol, 4ème huile la plus consommée au monde, est une excellente source de vitamine E et d''acides gras essentiels Oméga 6. Usage polyvalent, elle résiste mieux aux températures élevées et présente un goût et une odeur relativement neutres. Intrant de choix en conserverie de poisson, margarinerie, biscuiterie et industries cosmétique et pharmaceutique. Carton de 4 bidons de 5 L.',
        true, 2.000, 316.00, 'CAR',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Huile-de-tournesol-768x813.png',
        true
    ),
    (
        'HCL-001',
        'Huile de colza',
        'Huiles',
        'L''huile de colza est la 3ème huile la plus consommée au monde. Riche en Oméga 3 et en vitamine E, elle offre un rapport Oméga 6/Oméga 3 conforme aux recommandations alimentaires. Débouchés : margarinerie, mayonnaises et sauces émulsionnées, biscuiterie et conserverie (notamment de poisson). Moins adaptée aux hautes températures. Carton de 4 bidons de 5 L.',
        true, 2.000, 324.00, 'CAR',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Huile-de-colga-768x814.png',
        true
    ),
    (
        'HOL-010',
        'Huile d''olive',
        'Huiles',
        'Vierge extra ou raffinée, l''huile d''olive est reconnue pour ses apports nutritionnels. Sa richesse en acides gras monoinsaturés et en antioxydants naturels lui confère de multiples atouts. Utilisée en conserverie de poisson, margarinerie et autres industries agroalimentaires. À conserver au frais, à l''abri de la lumière et des fortes odeurs. Carton de 4 bidons de 5 L.',
        true, 1.000, 840.00, 'CAR',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Huile-dolive-768x811.png',
        true
    ),
    (
        'TTS-001',
        'Tourteau de tournesol',
        'Tourteaux',
        'Coproduit d''huilerie obtenu par pression et extraction au solvant de graines de tournesol, avec décorticage partiel ou complet. Selon le degré de décorticage : 26–28% de protéines (non décortiqué) jusqu''à plus de 34% (décortiqué). Source appréciable de vitamine E et de vitamines du groupe B, destinée à l''alimentation animale (ruminants, volailles). Sac de 25 kg.',
        true, 2.000, 96.00, 'SAC',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Couv_tournesol_18x18cm-768x768.png',
        true
    ),
    (
        'TCL-001',
        'Tourteau de colza',
        'Tourteaux',
        'Coproduit d''huilerie obtenu après pression et extraction par solvant des graines de colza. Source de protéines végétales pour l''alimentation animale, notamment volailles, bovins à l''engraissement et vaches laitières. Équilibré en acides aminés digestibles et en énergie. Sac de 25 kg.',
        true, 2.000, 105.00, 'SAC',
        'https://lesieur-cristal.com/wp-content/uploads/2019/11/Couv_COLZA_18x18cm-768x768.png',
        true
    )
ON CONFLICT (code) DO UPDATE SET
    name                = EXCLUDED.name,
    category            = EXCLUDED.category,
    description         = EXCLUDED.description,
    is_sampleable       = EXCLUDED.is_sampleable,
    max_sample_quantity = EXCLUDED.max_sample_quantity,
    unit_price          = EXCLUDED.unit_price,
    sales_unit          = EXCLUDED.sales_unit,
    image_url           = EXCLUDED.image_url,
    is_active           = true;
