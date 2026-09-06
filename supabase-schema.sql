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
    -- Annulation douce : une vente n'est jamais supprimée, elle est marquée
    -- annulée (trace anti-fraude). Exclue du CA mais conservée en base.
    cancelled BOOLEAN NOT NULL DEFAULT false,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Annulation douce : ajoute la colonne sur les tables déjà créées (idempotent).
ALTER TABLE sales ADD COLUMN IF NOT EXISTS cancelled BOOLEAN NOT NULL DEFAULT false;

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

-- 8. Journal d'audit des ventes (append-only) — trace inaltérable des
-- créations / modifications / annulations. Anti-fraude : une vente ne peut
-- pas être dissimulée sans laisser d'entrée horodatée synchronisée ici.
CREATE TABLE IF NOT EXISTS sale_audit_log (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    action TEXT NOT NULL,      -- 'created' | 'modified' | 'cancelled'
    details TEXT NOT NULL DEFAULT '',
    date BIGINT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
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
CREATE INDEX IF NOT EXISTS idx_sale_audit_sale ON sale_audit_log(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_audit_user ON sale_audit_log(user_id);

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
ALTER TABLE sale_audit_log ENABLE ROW LEVEL SECURITY;

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

-- Journal d'audit : APPEND-ONLY côté serveur. L'utilisateur peut lire et
-- insérer SES entrées, mais jamais les modifier ni les supprimer (aucune
-- policy UPDATE/DELETE). C'est la garantie anti-fraude : même le propriétaire
-- du compte ne peut pas effacer la trace d'une vente. Le back-office
-- (service_role) bypasse la RLS pour la lecture/le contrôle admin.
DROP POLICY IF EXISTS "User reads own audit" ON sale_audit_log;
CREATE POLICY "User reads own audit" ON sale_audit_log
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "User inserts own audit" ON sale_audit_log;
CREATE POLICY "User inserts own audit" ON sale_audit_log
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Réassigne un token FCM au compte connecté (auth.uid()).
-- SECURITY DEFINER : contourne la RLS pour permettre à un 2e utilisateur de
-- reprendre un token déjà lié à un autre compte sur le même appareil.
CREATE OR REPLACE FUNCTION public.upsert_device_token(p_token text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
BEGIN
    INSERT INTO device_tokens (user_id, fcm_token, updated_at)
    VALUES (auth.uid(), p_token, (extract(epoch FROM now()) * 1000)::bigint)
    ON CONFLICT (fcm_token) DO UPDATE
        SET user_id = EXCLUDED.user_id, updated_at = EXCLUDED.updated_at;
END;
$func$;

REVOKE EXECUTE ON FUNCTION public.upsert_device_token(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.upsert_device_token(text) TO authenticated;

-- ============================================================
-- 8. ACTIVATION PREMIUM CÔTÉ SERVEUR (les codes ne vivent plus dans l'APK)
-- ============================================================
-- Les codes sont ici, à usage unique, validés côté serveur. L'app appelle
-- redeem_premium_code() avec le code saisi ; la fonction vérifie le code, le
-- marque utilisé et pose les réglages premium. La table n'est ni lisible ni
-- modifiable par les clients (RLS + REVOKE) — seul service_role / la fonction.
CREATE TABLE IF NOT EXISTS public.premium_codes (
    code TEXT PRIMARY KEY,
    plan TEXT NOT NULL CHECK (plan IN ('plus', 'business')),
    used_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    used_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch FROM now()) * 1000)::bigint
);

ALTER TABLE public.premium_codes ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.premium_codes FROM anon, authenticated;

-- Génère 20 codes Plus + 5 codes Business la PREMIÈRE fois uniquement
-- (idempotent : les runs suivants ne recréent rien).
INSERT INTO public.premium_codes (code, plan)
SELECT 'LISSAFI-PLUS-' || upper(substr(md5(random()::text || gen_random_uuid()::text), 1, 10)), 'plus'
FROM generate_series(1, 20)
WHERE NOT EXISTS (SELECT 1 FROM public.premium_codes);

INSERT INTO public.premium_codes (code, plan)
SELECT 'LISSAFI-BUSINESS-' || upper(substr(md5(random()::text || gen_random_uuid()::text), 1, 10)), 'business'
FROM generate_series(1, 5)
WHERE NOT EXISTS (SELECT 1 FROM public.premium_codes WHERE plan = 'business');

-- Valide et active un code pour l'utilisateur connecté (auth.uid()).
-- SECURITY DEFINER : pose les réglages malgré la RLS. Ouvre la fenêtre
-- `lissafi.redeem` que le trigger guard_premium_keys (supabase-admin.sql)
-- exige pour autoriser l'écriture — un utilisateur lambda ne peut pas la
-- reproduire lui-même (config non positionnable par PostgREST).
CREATE OR REPLACE FUNCTION public.redeem_premium_code(p_code text)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE
    v_plan text;
    v_expiry bigint;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'not_authenticated';
    END IF;

    SELECT plan INTO v_plan
    FROM public.premium_codes
    WHERE code = p_code AND used_by IS NULL
    FOR UPDATE;

    IF v_plan IS NULL THEN
        RAISE EXCEPTION 'code_invalide_ou_utilise';
    END IF;

    v_expiry := (extract(epoch FROM now() + interval '365 days') * 1000)::bigint;

    UPDATE public.premium_codes
    SET used_by = auth.uid(),
        used_at = (extract(epoch FROM now()) * 1000)::bigint
    WHERE code = p_code;

    PERFORM set_config('lissafi.redeem', 'true', true);

    INSERT INTO public.app_settings (key, value, user_id) VALUES
        ('is_premium', 'true', auth.uid()),
        ('plan', v_plan, auth.uid()),
        ('premium_expiry', v_expiry::text, auth.uid()),
        ('activation_code', p_code, auth.uid()),
        ('demo_taken', 'true', auth.uid())
    ON CONFLICT (key, user_id) DO UPDATE SET value = EXCLUDED.value;

    RETURN jsonb_build_object('ok', true, 'plan', v_plan, 'premium_expiry', v_expiry);
END;
$func$;

REVOKE EXECUTE ON FUNCTION public.redeem_premium_code(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.redeem_premium_code(text) TO authenticated;
