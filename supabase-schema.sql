-- ============================================================
-- SCHÉMA SUPABASE POUR LISSAFI (v2 — avec authentification)
-- ============================================================
-- Exécuter ce script dans le SQL Editor de Supabase
-- ============================================================

-- 1. Table des produits
CREATE TABLE IF NOT EXISTS products (
    barcode TEXT NOT NULL,
    name TEXT NOT NULL,
    sell_price INTEGER NOT NULL DEFAULT 0,
    buy_price INTEGER NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0,
    min_stock INTEGER NOT NULL DEFAULT 5,
    category TEXT NOT NULL DEFAULT '',
    has_barcode BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    deleted BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (barcode, user_id)
);

-- Soft delete : ajoute la colonne sur les tables déjà créées (script idempotent)
ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;

-- 2. Table des ventes
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
    date BIGINT NOT NULL,
    total INTEGER NOT NULL,
    amount_paid INTEGER NOT NULL DEFAULT 0,
    change_given INTEGER NOT NULL DEFAULT 0,
    is_credit BOOLEAN NOT NULL DEFAULT false,
    client_id TEXT,
    synced BOOLEAN NOT NULL DEFAULT false,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
);

-- 3. Table des articles vendus
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    name TEXT NOT NULL,
    price INTEGER NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
);

-- 4. Table des clients
CREATE TABLE IF NOT EXISTS clients (
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT NOT NULL DEFAULT '',
    total_debt INTEGER NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    PRIMARY KEY (id, user_id)
);

-- 5. Table des transactions de dette
CREATE TABLE IF NOT EXISTS debt_transactions (
    id BIGSERIAL PRIMARY KEY,
    client_id TEXT NOT NULL,
    sale_id BIGINT REFERENCES sales(id) ON DELETE SET NULL,
    amount INTEGER NOT NULL,
    date BIGINT NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
);

-- 6. Table des paramètres applicatifs
CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    PRIMARY KEY (key, user_id)
);

-- 7. Table des tokens d'appareil (notifications push FCM)
CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE (fcm_token)
);

-- ============================================================
-- INDEX
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_user ON sales(user_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_debt_transactions_client ON debt_transactions(client_id);
CREATE INDEX IF NOT EXISTS idx_products_user ON products(user_id);
CREATE INDEX IF NOT EXISTS idx_clients_user ON clients(user_id);

-- ============================================================
-- ROW LEVEL SECURITY — Isolation par utilisateur
-- ============================================================

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE debt_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;

-- Chaque utilisateur ne voit QUE ses propres données
-- auth.uid() = l'ID de l'utilisateur connecté
-- Le script est idempotent : on peut le relancer sans erreur.

DROP POLICY IF EXISTS "User sees own products" ON products;
CREATE POLICY "User sees own products" ON products
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own sales" ON sales;
CREATE POLICY "User sees own sales" ON sales
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own sale_items" ON sale_items;
CREATE POLICY "User sees own sale_items" ON sale_items
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own clients" ON clients;
CREATE POLICY "User sees own clients" ON clients
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own debt_transactions" ON debt_transactions;
CREATE POLICY "User sees own debt_transactions" ON debt_transactions
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own settings" ON app_settings;
CREATE POLICY "User sees own settings" ON app_settings
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own device_tokens" ON device_tokens;
CREATE POLICY "User sees own device_tokens" ON device_tokens
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
