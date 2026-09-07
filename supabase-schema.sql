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
-- GRAND BOUTIQUE — Multi-caisses (boutique partagée)
-- ============================================================
-- Une boutique = l'unité de partage. Son id EST le user_id du patron/propriétaire
-- (pas un UUID indépendant) : la migration des comptes existants est triviale
-- (shop_id = user_id) et local/serveur s'accordent sans réconciliation d'UUID.
-- Un vendeur rattaché porte shop_id = user_id du patron.

-- 8. Boutique (id = user_id du patron)
CREATE TABLE IF NOT EXISTS shops (
    id         UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name       TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL
);

-- 9. Appartenance + rôle (pilote la RLS)
CREATE TABLE IF NOT EXISTS shop_members (
    shop_id      UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role         TEXT NOT NULL DEFAULT 'vendeur' CHECK (role IN ('patron', 'vendeur')),
    caisse_label TEXT NOT NULL DEFAULT '',
    joined_at    BIGINT NOT NULL,
    PRIMARY KEY (shop_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_shop_members_user ON shop_members(user_id);

-- Colonne shop_id sur les 5 tables métier partagées (idempotent).
-- app_settings et device_tokens restent cloisonnées par user_id (statut premium propre au compte).
ALTER TABLE products           ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE sales              ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE sale_items         ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE clients            ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE debt_transactions  ADD COLUMN IF NOT EXISTS shop_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;

-- Backfill : chaque compte existant devient sa boutique solo (shop_id = user_id).
UPDATE products          SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE sales             SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE sale_items        SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE clients           SET shop_id = user_id WHERE shop_id IS NULL;
UPDATE debt_transactions SET shop_id = user_id WHERE shop_id IS NULL;

-- Amorce les boutiques solo pour tous les propriétaires de données existants.
INSERT INTO shops (id, name, created_at)
SELECT DISTINCT user_id, '', (extract(epoch FROM now()) * 1000)::bigint
FROM (
    SELECT user_id FROM products
    UNION SELECT user_id FROM sales
    UNION SELECT user_id FROM clients
) u
ON CONFLICT (id) DO NOTHING;

INSERT INTO shop_members (shop_id, user_id, role, caisse_label, joined_at)
SELECT id, id, 'patron', 'Caisse 1', created_at FROM shops
ON CONFLICT (shop_id, user_id) DO NOTHING;

-- ============================================================
-- INDEX
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_products_shop          ON products(shop_id);
CREATE INDEX IF NOT EXISTS idx_sales_shop             ON sales(shop_id);
CREATE INDEX IF NOT EXISTS idx_clients_shop           ON clients(shop_id);
CREATE INDEX IF NOT EXISTS idx_debt_transactions_shop ON debt_transactions(shop_id);
-- Catalogue unique par boutique (pour solo, shop_id = user_id → équivalent à la PK actuelle).
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_barcode_shop ON products(barcode, shop_id);

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

-- Les tables métier sont partagées PAR BOUTIQUE (offre Grand boutique) : chaque
-- utilisateur voit les lignes de sa boutique, pas seulement les siennes. Pour un
-- compte solo (shop_id = user_id), le comportement est identique à avant.
-- app_settings et device_tokens restent cloisonnées par user_id (auth.uid() = user_id).
-- Le script est idempotent : on peut le relancer sans erreur.

-- Helper d'appartenance. SECURITY DEFINER pour éviter la récursion RLS sur shop_members.
CREATE OR REPLACE FUNCTION public.is_shop_member(p_shop uuid)
RETURNS boolean
LANGUAGE sql SECURITY DEFINER SET search_path = public STABLE
AS $func$
    SELECT EXISTS (
        SELECT 1 FROM shop_members
        WHERE shop_id = p_shop AND user_id = auth.uid()
    );
$func$;
REVOKE EXECUTE ON FUNCTION public.is_shop_member(uuid) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.is_shop_member(uuid) TO authenticated;

DROP POLICY IF EXISTS "User sees own products" ON products;
DROP POLICY IF EXISTS "Shop sees products" ON products;
CREATE POLICY "Shop sees products" ON products
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own sales" ON sales;
DROP POLICY IF EXISTS "Shop sees sales" ON sales;
CREATE POLICY "Shop sees sales" ON sales
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own sale_items" ON sale_items;
DROP POLICY IF EXISTS "Shop sees sale_items" ON sale_items;
CREATE POLICY "Shop sees sale_items" ON sale_items
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own clients" ON clients;
DROP POLICY IF EXISTS "Shop sees clients" ON clients;
CREATE POLICY "Shop sees clients" ON clients
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own debt_transactions" ON debt_transactions;
DROP POLICY IF EXISTS "Shop sees debt_transactions" ON debt_transactions;
CREATE POLICY "Shop sees debt_transactions" ON debt_transactions
    FOR ALL USING (is_shop_member(shop_id)) WITH CHECK (is_shop_member(shop_id));

-- Boutique : un membre voit sa boutique et la liste de ses membres. L'écriture
-- passe par les fonctions SECURITY DEFINER (get_or_create_my_shop, appairage au Lot 2).
DROP POLICY IF EXISTS "Member sees own shop" ON shops;
CREATE POLICY "Member sees own shop" ON shops
    FOR SELECT USING (is_shop_member(id));

DROP POLICY IF EXISTS "Member sees shop roster" ON shop_members;
CREATE POLICY "Member sees shop roster" ON shop_members
    FOR SELECT USING (is_shop_member(shop_id));

DROP POLICY IF EXISTS "User sees own settings" ON app_settings;
CREATE POLICY "User sees own settings" ON app_settings
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "User sees own device_tokens" ON device_tokens;
CREATE POLICY "User sees own device_tokens" ON device_tokens
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

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

-- Renvoie le shop_id de l'utilisateur connecté, en créant sa boutique solo
-- (id = son user_id, rôle patron) s'il n'est encore membre d'aucune boutique.
-- Appelée par l'app au login (Lot 2) pour connaître sa boutique.
CREATE OR REPLACE FUNCTION public.get_or_create_my_shop()
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE
    v_shop uuid;
    v_now  bigint := (extract(epoch FROM now()) * 1000)::bigint;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'not_authenticated';
    END IF;

    SELECT shop_id INTO v_shop FROM shop_members WHERE user_id = auth.uid() LIMIT 1;
    IF v_shop IS NOT NULL THEN
        RETURN v_shop;
    END IF;

    INSERT INTO shops (id, name, created_at) VALUES (auth.uid(), '', v_now)
    ON CONFLICT (id) DO NOTHING;
    INSERT INTO shop_members (shop_id, user_id, role, caisse_label, joined_at)
    VALUES (auth.uid(), auth.uid(), 'patron', 'Caisse 1', v_now)
    ON CONFLICT (shop_id, user_id) DO NOTHING;

    RETURN auth.uid();
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.get_or_create_my_shop() FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.get_or_create_my_shop() TO authenticated;

-- Filet de sécurité : tant que l'app ne renseigne pas encore shop_id (avant le Lot 3),
-- toute ligne insérée sans shop_id se voit affecter shop_id = user_id (boutique solo).
-- Garantit que la RLS is_shop_member(shop_id) laisse passer les insertions existantes
-- (le patron est membre de sa boutique = son user_id). À conserver ensuite : une caisse
-- vendeur enverra explicitement le shop_id du patron, ce trigger ne s'appliquant qu'au NULL.
CREATE OR REPLACE FUNCTION public.default_shop_id()
RETURNS trigger
LANGUAGE plpgsql AS $func$
BEGIN
    IF NEW.shop_id IS NULL THEN
        NEW.shop_id := NEW.user_id;
    END IF;
    RETURN NEW;
END;
$func$;

DROP TRIGGER IF EXISTS trg_default_shop_id ON products;
CREATE TRIGGER trg_default_shop_id BEFORE INSERT ON products
    FOR EACH ROW EXECUTE FUNCTION public.default_shop_id();
DROP TRIGGER IF EXISTS trg_default_shop_id ON sales;
CREATE TRIGGER trg_default_shop_id BEFORE INSERT ON sales
    FOR EACH ROW EXECUTE FUNCTION public.default_shop_id();
DROP TRIGGER IF EXISTS trg_default_shop_id ON sale_items;
CREATE TRIGGER trg_default_shop_id BEFORE INSERT ON sale_items
    FOR EACH ROW EXECUTE FUNCTION public.default_shop_id();
DROP TRIGGER IF EXISTS trg_default_shop_id ON clients;
CREATE TRIGGER trg_default_shop_id BEFORE INSERT ON clients
    FOR EACH ROW EXECUTE FUNCTION public.default_shop_id();
DROP TRIGGER IF EXISTS trg_default_shop_id ON debt_transactions;
CREATE TRIGGER trg_default_shop_id BEFORE INSERT ON debt_transactions
    FOR EACH ROW EXECUTE FUNCTION public.default_shop_id();

-- ============================================================
-- GRAND BOUTIQUE — Appairage des caisses (patron ↔ vendeurs)
-- ============================================================
-- Codes d'appairage à usage unique, gérés uniquement via fonctions SECURITY
-- DEFINER (comme premium_codes) : ni lisibles ni modifiables par les clients.
CREATE TABLE IF NOT EXISTS shop_pairing_codes (
    code       TEXT PRIMARY KEY,
    shop_id    UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    used_by    UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    used_at    BIGINT
);
ALTER TABLE shop_pairing_codes ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON shop_pairing_codes FROM anon, authenticated;

-- Suis-je patron de cette boutique ?
CREATE OR REPLACE FUNCTION public.is_shop_patron(p_shop uuid)
RETURNS boolean
LANGUAGE sql SECURITY DEFINER SET search_path = public STABLE
AS $func$
    SELECT EXISTS (
        SELECT 1 FROM shop_members
        WHERE shop_id = p_shop AND user_id = auth.uid() AND role = 'patron'
    );
$func$;
REVOKE EXECUTE ON FUNCTION public.is_shop_patron(uuid) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.is_shop_patron(uuid) TO authenticated;

-- Nombre de caisses autorisé pour la boutique du patron (app_settings.max_caisses,
-- clé server-managed posée par redeem_premium_code au Lot 5). Défaut 1 (mono-caisse) :
-- sans l'offre Grand boutique, on ne peut pas rattacher de 2e caisse.
CREATE OR REPLACE FUNCTION public.shop_max_caisses(p_shop uuid)
RETURNS int
LANGUAGE sql SECURITY DEFINER SET search_path = public STABLE
AS $func$
    SELECT COALESCE(
        (SELECT NULLIF(value, '')::int FROM app_settings WHERE user_id = p_shop AND key = 'max_caisses'),
        1
    );
$func$;

-- Le patron génère un code d'appairage (valable 15 min). Refuse si le quota de
-- caisses est déjà atteint.
CREATE OR REPLACE FUNCTION public.create_pairing_code()
RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE
    v_shop uuid;
    v_code text;
    v_now  bigint := (extract(epoch FROM now()) * 1000)::bigint;
    v_max  int;
    v_count int;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'not_authenticated'; END IF;
    SELECT shop_id INTO v_shop FROM shop_members
        WHERE user_id = auth.uid() AND role = 'patron' LIMIT 1;
    IF v_shop IS NULL THEN RAISE EXCEPTION 'not_patron'; END IF;

    v_max := shop_max_caisses(v_shop);
    SELECT count(*) INTO v_count FROM shop_members WHERE shop_id = v_shop;
    IF v_count >= v_max THEN RAISE EXCEPTION 'quota_caisses_atteint'; END IF;

    v_code := 'LSF-' || upper(substr(md5(random()::text || gen_random_uuid()::text), 1, 6));
    INSERT INTO shop_pairing_codes (code, shop_id, created_at, expires_at)
        VALUES (v_code, v_shop, v_now, v_now + 15 * 60 * 1000);
    RETURN v_code;
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.create_pairing_code() FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.create_pairing_code() TO authenticated;

-- Un vendeur rejoint une boutique via un code. Valide expiration/unicité/quota,
-- détache l'utilisateur de son éventuelle boutique précédente (1 boutique active),
-- puis l'inscrit comme vendeur. Renvoie {shop_id, role}.
CREATE OR REPLACE FUNCTION public.join_shop_with_code(p_code text)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE
    v_shop uuid; v_exp bigint; v_used uuid;
    v_now bigint := (extract(epoch FROM now()) * 1000)::bigint;
    v_max int; v_count int;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'not_authenticated'; END IF;
    SELECT shop_id, expires_at, used_by INTO v_shop, v_exp, v_used
        FROM shop_pairing_codes WHERE code = p_code FOR UPDATE;
    IF v_shop IS NULL THEN RAISE EXCEPTION 'code_invalide'; END IF;
    IF v_used IS NOT NULL THEN RAISE EXCEPTION 'code_deja_utilise'; END IF;
    IF v_now > v_exp THEN RAISE EXCEPTION 'code_expire'; END IF;

    v_max := shop_max_caisses(v_shop);
    SELECT count(*) INTO v_count FROM shop_members WHERE shop_id = v_shop;
    IF v_count >= v_max THEN RAISE EXCEPTION 'quota_caisses_atteint'; END IF;

    -- Une seule boutique active par utilisateur : quitte l'ancienne appartenance.
    -- (Les données de son ancienne boutique solo restent sous son propre shop_id,
    -- simplement plus visibles tant qu'il est rattaché — acceptable en v1.)
    DELETE FROM shop_members WHERE user_id = auth.uid();
    INSERT INTO shop_members (shop_id, user_id, role, caisse_label, joined_at)
        VALUES (v_shop, auth.uid(), 'vendeur', '', v_now);
    UPDATE shop_pairing_codes SET used_by = auth.uid(), used_at = v_now WHERE code = p_code;

    RETURN jsonb_build_object('shop_id', v_shop, 'role', 'vendeur');
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.join_shop_with_code(text) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.join_shop_with_code(text) TO authenticated;

-- Liste des membres de MA boutique (patron uniquement ; vide sinon).
CREATE OR REPLACE FUNCTION public.my_shop_members()
RETURNS TABLE(member_id uuid, role text, caisse_label text, joined_at bigint)
LANGUAGE sql SECURITY DEFINER SET search_path = public STABLE
AS $func$
    SELECT m.user_id, m.role, m.caisse_label, m.joined_at
    FROM shop_members m
    WHERE m.shop_id IN (
        SELECT shop_id FROM shop_members WHERE user_id = auth.uid() AND role = 'patron'
    )
    ORDER BY m.joined_at ASC;
$func$;
REVOKE EXECUTE ON FUNCTION public.my_shop_members() FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.my_shop_members() TO authenticated;

-- Le patron détache une caisse vendeur.
CREATE OR REPLACE FUNCTION public.remove_shop_member(p_user uuid)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
DECLARE v_shop uuid;
BEGIN
    SELECT shop_id INTO v_shop FROM shop_members
        WHERE user_id = auth.uid() AND role = 'patron' LIMIT 1;
    IF v_shop IS NULL THEN RAISE EXCEPTION 'not_patron'; END IF;
    IF p_user = auth.uid() THEN RAISE EXCEPTION 'cannot_remove_self'; END IF;
    DELETE FROM shop_members WHERE shop_id = v_shop AND user_id = p_user AND role = 'vendeur';
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.remove_shop_member(uuid) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.remove_shop_member(uuid) TO authenticated;

-- Un vendeur quitte la boutique et retrouve sa boutique solo.
CREATE OR REPLACE FUNCTION public.leave_shop()
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $func$
BEGIN
    DELETE FROM shop_members WHERE user_id = auth.uid() AND role = 'vendeur';
    RETURN get_or_create_my_shop();
END;
$func$;
REVOKE EXECUTE ON FUNCTION public.leave_shop() FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public.leave_shop() TO authenticated;

-- ============================================================
-- 8. ACTIVATION PREMIUM CÔTÉ SERVEUR (les codes ne vivent plus dans l'APK)
-- ============================================================
-- Les codes sont ici, à usage unique, validés côté serveur. L'app appelle
-- redeem_premium_code() avec le code saisi ; la fonction vérifie le code, le
-- marque utilisé et pose les réglages premium. La table n'est ni lisible ni
-- modifiable par les clients (RLS + REVOKE) — seul service_role / la fonction.
CREATE TABLE IF NOT EXISTS public.premium_codes (
    code TEXT PRIMARY KEY,
    plan TEXT NOT NULL CHECK (plan IN ('plus', 'business', 'grand_boutique')),
    used_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    used_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (extract(epoch FROM now()) * 1000)::bigint
);

-- Élargit la contrainte de plan sur une table déjà créée (idempotent).
ALTER TABLE public.premium_codes DROP CONSTRAINT IF EXISTS premium_codes_plan_check;
ALTER TABLE public.premium_codes ADD CONSTRAINT premium_codes_plan_check
    CHECK (plan IN ('plus', 'business', 'grand_boutique'));

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
    v_max_caisses int;
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
    -- Nombre de caisses inclus : 2 pour Grand boutique, 1 sinon (mono-caisse).
    v_max_caisses := CASE v_plan WHEN 'grand_boutique' THEN 2 ELSE 1 END;

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
        ('max_caisses', v_max_caisses::text, auth.uid()),
        ('demo_taken', 'true', auth.uid())
    ON CONFLICT (key, user_id) DO UPDATE SET value = EXCLUDED.value;

    RETURN jsonb_build_object('ok', true, 'plan', v_plan, 'premium_expiry', v_expiry, 'max_caisses', v_max_caisses);
END;
$func$;

REVOKE EXECUTE ON FUNCTION public.redeem_premium_code(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.redeem_premium_code(text) TO authenticated;
