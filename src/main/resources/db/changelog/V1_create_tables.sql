-- =============================================================================
-- Portail B2B Lesieur Cristal — Script de création de la base de données
-- PostgreSQL 17
-- =============================================================================
--
-- Organisation : une seule instance PostgreSQL, deux schémas.
--   - erp_mock : simule les données SAP (commandes, statuts, factures,
--                clients). Table jetable : le jour où l'accès SAP réel est
--                disponible, ce schéma est supprimé et le connecteur ERP
--                pointe vers SAP à la place — rien dans `app` ne change.
--   - app      : données natives et permanentes du portail.
--
-- Concernant erp_mock.orders / order_status / invoices : l'extrait SAP fourni
-- par le client contient ~180 colonnes issues d'un rapport SAP qui aplatit
-- commande + livraison + facture + logistique (EDI, palettisation, tarifs,
-- Incoterms, etc.). Seules les colonnes réellement utiles à l'affichage
-- client (historique des commandes, statut en cours, factures réglées/à
-- régler) ont été retenues ici ; chaque colonne gardée renvoie en commentaire
-- au libellé SAP d'origine dont elle est issue. Tout le reste (paliers
-- tarifaires, EDI, blocages logistiques, GTIN, poids/volumes détaillés,
-- titristation, etc.) est un détail interne SAP sans valeur pour le client
-- final et a été volontairement écarté.
--
-- Conventions :
--   - Clés primaires : GENERATED ALWAYS AS IDENTITY (sauf clés naturelles
--     comme les numéros SAP, qui sont déjà des identifiants métier stables).
--   - Statuts : chaînes de caractères contraintes par CHECK, en anglais,
--     pour rester cohérent avec les décisions déjà prises dans le README de
--     conception (design-choice.md).
--   - Toutes les tables `app.*` qui appartiennent à un client portent une
--     colonne `customer_number`, clé logique unique reliant les deux
--     schémas (l'authentification reste entièrement dans `app`, les
--     données métier restent entièrement dans `erp_mock`).
-- =============================================================================


-- =============================================================================
-- 0. SCHÉMAS
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS erp_mock;
CREATE SCHEMA IF NOT EXISTS app;

COMMENT ON SCHEMA erp_mock IS 'Données simulant SAP. Schéma jetable, remplacé par un vrai connecteur SAP plus tard.';
COMMENT ON SCHEMA app      IS 'Données natives et permanentes du portail B2B.';

-- Fonction utilitaire réutilisée par tous les triggers "updated_at".
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =============================================================================
-- 1. SCHEMA erp_mock — simulation SAP
-- =============================================================================

-- -----------------------------------------------------------------------------
-- erp_mock.customers
-- -----------------------------------------------------------------------------
CREATE TABLE erp_mock.customers (
    customer_number   VARCHAR(20)  PRIMARY KEY,          -- SAP "Donneur d'ordre" / "Payeur"
    company_name      VARCHAR(150) NOT NULL,             -- SAP "Nom Donneur d'ordre"
    postal_address    VARCHAR(255),
    city              VARCHAR(100),                      -- SAP "Ville (livré)"
    country           VARCHAR(100),                      -- SAP "Pays (livré)"
    phone             VARCHAR(30),
    email             VARCHAR(150),
    vat_id            VARCHAR(30),                       -- SAP "ID TVA Client"
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE erp_mock.customers IS 'Fiche client simulée. En réel : SAP business partner / customer master.';

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON erp_mock.customers
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


-- -----------------------------------------------------------------------------
-- erp_mock.orders — lignes de commande
-- -----------------------------------------------------------------------------
CREATE TABLE erp_mock.orders (
    order_number               VARCHAR(20)  PRIMARY KEY,      -- SAP "Document de vente"
    order_date                 DATE         NOT NULL,         -- SAP "Date doc."
    customer_number            VARCHAR(20)  NOT NULL REFERENCES erp_mock.customers(customer_number),
    customer_order_reference   VARCHAR(50),                   -- SAP "Cde Client" (numéro de commande interne au client)
    product_code               VARCHAR(30)  NOT NULL,         -- SAP "Article"
    product_label               VARCHAR(150),                 -- SAP "Libellé"
    quantity_ordered            NUMERIC(14,3),                 -- SAP "Qté cdée de cde"
    quantity_shipped            NUMERIC(14,3),                 -- SAP "Quantité expédiée"
    sales_unit                  VARCHAR(10),                   -- SAP "Unité de vente"
    net_amount                  NUMERIC(14,2),                 -- SAP "Montant net" / "Val. Nette"
    currency                    VARCHAR(3),                    -- SAP "Devise"
    ship_to_city                VARCHAR(100),                  -- SAP "Ville (livré)"
    ship_to_country             VARCHAR(100),                  -- SAP "Pays (livré)"
    requested_delivery_date     DATE,                          -- SAP "Date liv."
    planned_delivery_date       DATE,                          -- SAP "Date de livraison prév 1"
    goods_issue_date            DATE,                          -- SAP "Date SM réelle" (date d'expédition réelle)
    invoice_number               VARCHAR(20),                  -- SAP "Facture" (lien vers erp_mock.invoices)
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE erp_mock.orders IS 'Lignes de commande simulant un extrait SAP (Historique des commandes). Colonnes non retenues de l''extrait original : tout ce qui est logistique/EDI/tarification interne SAP sans valeur d''affichage client.';

CREATE INDEX idx_orders_customer_number ON erp_mock.orders(customer_number);
CREATE INDEX idx_orders_invoice_number  ON erp_mock.orders(invoice_number);

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON erp_mock.orders
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


-- -----------------------------------------------------------------------------
-- erp_mock.order_status — suivi en direct (Suivi de commande)
-- -----------------------------------------------------------------------------
CREATE TABLE erp_mock.order_status (
    order_number          VARCHAR(20) PRIMARY KEY REFERENCES erp_mock.orders(order_number),
    current_status        VARCHAR(20) NOT NULL DEFAULT 'confirmed'
                           CHECK (current_status IN ('confirmed','in_preparation','shipped','delivered','cancelled')),
                                                                -- SAP "Statut Commande"
    status_updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expected_delivery_date DATE,                                -- SAP "Date de livraison prév 1"
    carrier_name           VARCHAR(100),                        -- SAP "Nom transporteur"
    carrier_reference      VARCHAR(50)                          -- SAP "Numéro de container" / "Nº de groupage"
);

COMMENT ON TABLE erp_mock.order_status IS 'Statut en direct d''une commande. Pas de document/PDF associé, simple valeur affichée à chaque appel (pas de cache pour le MVP).';

CREATE INDEX idx_order_status_current_status ON erp_mock.order_status(current_status);


-- -----------------------------------------------------------------------------
-- erp_mock.invoices — factures
-- -----------------------------------------------------------------------------
CREATE TABLE erp_mock.invoices (
    invoice_number   VARCHAR(20)  PRIMARY KEY,           -- SAP "Facture"
    invoice_date     DATE         NOT NULL,              -- SAP "Date création fact."
    order_number     VARCHAR(20)  REFERENCES erp_mock.orders(order_number),
    customer_number  VARCHAR(20)  NOT NULL REFERENCES erp_mock.customers(customer_number),
    net_amount       NUMERIC(14,2),                      -- SAP "Montant net facturé"
    vat_amount       NUMERIC(14,2),                       -- SAP "Mt TVA (Fact)"
    total_amount     NUMERIC(14,2),                       -- net_amount + vat_amount
    currency         VARCHAR(3),                          -- SAP "Devise"
    due_date         DATE,                                -- SAP "Date d'échéance (pièce compt.)"
    invoice_status   VARCHAR(20) NOT NULL DEFAULT 'unpaid'
                     CHECK (invoice_status IN ('paid','unpaid','partially_paid')),
                                                            -- champ propre au portail, absent de l'extrait SAP fourni
                                                            -- (vient normalement du module FI-AR, pas de la vente) ;
                                                            -- mocké ici car explicitement requis par le besoin client.
    payment_date     DATE,                                 -- champ propre au portail, nullable
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE erp_mock.invoices IS 'Factures. Le PDF n''est jamais stocké ici (voir app.documents) — cette table ne porte que la donnée, toujours lue en direct pour le statut réglé/à régler.';

CREATE INDEX idx_invoices_customer_number ON erp_mock.invoices(customer_number);
CREATE INDEX idx_invoices_order_number    ON erp_mock.invoices(order_number);
CREATE INDEX idx_invoices_status          ON erp_mock.invoices(invoice_status);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON erp_mock.invoices
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE erp_mock.orders
    ADD CONSTRAINT fk_orders_invoice_number
    FOREIGN KEY (invoice_number) REFERENCES erp_mock.invoices(invoice_number)
    DEFERRABLE INITIALLY DEFERRED;
    -- Déférée : une commande peut être créée avant que sa facture existe.


-- =============================================================================
-- 2. SCHEMA app — données natives du portail
-- =============================================================================

-- -----------------------------------------------------------------------------
-- app.users — comptes (Client et Administrateur)
-- -----------------------------------------------------------------------------
CREATE TABLE app.users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number VARCHAR(20) REFERENCES erp_mock.customers(customer_number),
                    -- NULL pour un compte Administrateur (pas rattaché à un client)
    last_name       VARCHAR(100) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(30),
    login           VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('CLIENT','ADMIN')),
    language        VARCHAR(5)   NOT NULL DEFAULT 'fr',
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_client_has_customer_number
        CHECK (role = 'ADMIN' OR customer_number IS NOT NULL)
);

COMMENT ON TABLE app.users IS 'Comptes de connexion. Pas d''auto-inscription publique pour ce MVP : comptes provisionnés manuellement / via jeu de données.';

CREATE INDEX idx_users_customer_number ON app.users(customer_number);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON app.users
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


-- -----------------------------------------------------------------------------
-- app.client_change_log — traçabilité des modifications de la fiche client
-- -----------------------------------------------------------------------------
CREATE TABLE app.client_change_log (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number    VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    changed_by_user_id BIGINT REFERENCES app.users(id),
    field_name         VARCHAR(50) NOT NULL,
    old_value          TEXT,
    new_value          TEXT,
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_client_change_log_customer_number ON app.client_change_log(customer_number);


-- -----------------------------------------------------------------------------
-- app.products — catalogue statique (utilisé par devis / échantillon)
-- -----------------------------------------------------------------------------
CREATE TABLE app.products (
    code        VARCHAR(30)  PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    category    VARCHAR(100),
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON app.products
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


-- -----------------------------------------------------------------------------
-- app.reclamations
-- -----------------------------------------------------------------------------
CREATE TABLE app.reclamations (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number  VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id          BIGINT REFERENCES app.users(id),
    lot_number       VARCHAR(50),
    description      TEXT NOT NULL,
    attachment_path  VARCHAR(500),
    status           VARCHAR(20) NOT NULL DEFAULT 'new'
                     CHECK (status IN ('new','in_progress','resolved','rejected')),
    received_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reclamations_customer_number ON app.reclamations(customer_number);
CREATE INDEX idx_reclamations_status          ON app.reclamations(status);


-- -----------------------------------------------------------------------------
-- app.quotation_requests — demande de cotation
-- -----------------------------------------------------------------------------
CREATE TABLE app.quotation_requests (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number   VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id           BIGINT REFERENCES app.users(id),
    product_code      VARCHAR(30) REFERENCES app.products(code),
    quantity          NUMERIC(14,3) NOT NULL,
    transport_method  VARCHAR(50),
    details           TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'new'
                      CHECK (status IN ('new','processing','answered','rejected')),
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_quotation_requests_customer_number ON app.quotation_requests(customer_number);
CREATE INDEX idx_quotation_requests_status          ON app.quotation_requests(status);


-- -----------------------------------------------------------------------------
-- app.sample_requests — demande d'échantillon
-- -----------------------------------------------------------------------------
CREATE TABLE app.sample_requests (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number         VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id                 BIGINT REFERENCES app.users(id),
    product_type            VARCHAR(100),
    quantity                NUMERIC(14,3),
    contact_name            VARCHAR(150),
    contact_address         VARCHAR(255),
    status                  VARCHAR(20) NOT NULL DEFAULT 'new'
                            CHECK (status IN ('new','processing','fulfilled','rejected')),
    resulting_order_number  VARCHAR(20) REFERENCES erp_mock.orders(order_number),
                            -- rempli plus tard, manuellement ou via une future intégration SAP réelle
    requested_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sample_requests_customer_number ON app.sample_requests(customer_number);
CREATE INDEX idx_sample_requests_status          ON app.sample_requests(status);


-- -----------------------------------------------------------------------------
-- app.purchase_order_submissions — bon de commande (fichier joint)
-- -----------------------------------------------------------------------------
CREATE TABLE app.purchase_order_submissions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number  VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id          BIGINT REFERENCES app.users(id),
    file_path        VARCHAR(500) NOT NULL,
    note             TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'new'
                     CHECK (status IN ('new','processed')),
    submitted_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_order_submissions_customer_number ON app.purchase_order_submissions(customer_number);


-- -----------------------------------------------------------------------------
-- app.contact_messages — formulaire de contact libre
-- -----------------------------------------------------------------------------
CREATE TABLE app.contact_messages (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number  VARCHAR(20) REFERENCES erp_mock.customers(customer_number), -- nullable : contact possible hors compte client
    full_name        VARCHAR(150),
    email            VARCHAR(150),
    subject          VARCHAR(200),
    message          TEXT NOT NULL,
    attachment_path  VARCHAR(500),
    status           VARCHAR(20) NOT NULL DEFAULT 'new'
                     CHECK (status IN ('new','processed')),
    submitted_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contact_messages_customer_number ON app.contact_messages(customer_number);


-- -----------------------------------------------------------------------------
-- app.appointment_requests — rendez-vous
-- -----------------------------------------------------------------------------
CREATE TABLE app.appointment_requests (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number       VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    user_id               BIGINT REFERENCES app.users(id),
    subject               VARCHAR(200),
    requested_date        DATE NOT NULL,
    requested_time_slot   VARCHAR(50),
    status                VARCHAR(20) NOT NULL DEFAULT 'pending'
                          CHECK (status IN ('pending','confirmed','cancelled')),
    confirmed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_appointment_requests_customer_number ON app.appointment_requests(customer_number);


-- -----------------------------------------------------------------------------
-- app.documents — table pivot "Mes documents" (générique, extensible)
-- -----------------------------------------------------------------------------
CREATE TABLE app.documents (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number    VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    doc_type           VARCHAR(20) NOT NULL CHECK (doc_type IN ('invoice','certificate')),
                       -- seul 'invoice' est peuplé pour le MVP ; 'certificate' est prévu
                       -- pour la Phase 9 (bonus hors MVP) sans migration supplémentaire.
    title              VARCHAR(200),
    natural_key        VARCHAR(150) NOT NULL,
                       -- clé de dédoublonnage : numéro de facture, ou
                       -- customer_number:lot_number:certificate_type pour un certificat
    status             VARCHAR(20) NOT NULL DEFAULT 'ready'
                       CHECK (status IN ('ready','processing','pending_review')),
    file_path          VARCHAR(500),          -- NULL tant que le document n'est pas prêt
    related_reference  VARCHAR(100),          -- numéro de commande, de lot, etc. (traçabilité)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_documents_type_natural_key UNIQUE (doc_type, natural_key)
    -- Cette contrainte impose au niveau base le principe "générer une fois,
    -- puis réutiliser" : on vérifie toujours cette table avant de régénérer.
);

COMMENT ON TABLE app.documents IS 'Table unique interrogée par "Mes documents" pour tout type de document, quelle que soit son origine. Pour le MVP, seules les factures y sont écrites.';

CREATE INDEX idx_documents_customer_number ON app.documents(customer_number);
CREATE INDEX idx_documents_status          ON app.documents(status);

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON app.documents
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


-- -----------------------------------------------------------------------------
-- app.static_documents — plaquettes (optionnel, hors "Mes documents")
-- -----------------------------------------------------------------------------
CREATE TABLE app.static_documents (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    product_category VARCHAR(100),
    file_path        VARCHAR(500) NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE app.static_documents IS 'Brochures marketing, identiques pour tous les clients. Pas de customer_number, pas de statut, pas de lien avec app.documents.';


-- =============================================================================
-- 3. BONUS — HORS MVP (Phase 9, à ne traiter qu''une fois tout le reste livré)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- app.certificates — certificat de lot (origine / analyse / conformité)
-- -----------------------------------------------------------------------------
CREATE TABLE app.certificates (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_number   VARCHAR(20) NOT NULL REFERENCES erp_mock.customers(customer_number),
    certificate_type  VARCHAR(20) NOT NULL CHECK (certificate_type IN ('origin','analysis','conformity')),
    lot_number        VARCHAR(50) NOT NULL,
    product_code      VARCHAR(30) REFERENCES app.products(code),
    issue_date        DATE,
    technical_data    JSONB,
                      -- champs spécifiques par type (ex. acidity_index, peroxide_value
                      -- pour "analysis" ; country_of_origin, hs_code pour "origin") :
                      -- volontairement en JSONB pour ne pas migrer le schéma à chaque
                      -- nouveau type de certificat.
    document_id       BIGINT REFERENCES app.documents(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_certificates_natural_key UNIQUE (customer_number, lot_number, certificate_type)
);

COMMENT ON TABLE app.certificates IS 'BONUS hors MVP (Phase 9). Génération pensée synchrone (comme les factures), sans RabbitMQ/worker, pour rester dans le budget du délai imparti.';

CREATE INDEX idx_certificates_customer_number ON app.certificates(customer_number);
