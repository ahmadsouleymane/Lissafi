-- ============================================================
-- NETTOYAGE DES DONNÉES DE TEST + COMPTE DÉMO HORS DES STATS
-- ============================================================
-- À coller dans le SQL Editor Supabase (rôle postgres), APRÈS
-- supabase-schema.sql et supabase-admin.sql. Idempotent.
--
-- IMPORTANT : un compte de connexion DOIT exister dans auth.users
-- (c'est la base de l'authentification GoTrue). On ne peut pas avoir
-- de compte « non enregistré ». En revanche, on peut :
--   1. supprimer proprement les comptes de test (cascade automatique),
--   2. utiliser un compte dédié, isolé par user_id (RLS),
--   3. l'EXCLURE des statistiques du back-office pour ne pas fausser
--      les données réelles.
-- ============================================================

-- ------------------------------------------------------------
-- ÉTAPE 1 — DÉCOUVERTE (ne supprime RIEN)
-- Liste tous les comptes + volume de données. Reporte dans l'ÉTAPE 2
-- les emails des comptes de test à supprimer.
-- ------------------------------------------------------------
SELECT
    u.id,
    u.email,
    u.created_at,
    (SELECT count(*) FROM public.products p          WHERE p.user_id = u.id) AS products,
    (SELECT count(*) FROM public.sales s             WHERE s.user_id = u.id) AS sales,
    (SELECT count(*) FROM public.clients c           WHERE c.user_id = u.id) AS clients,
    (SELECT count(*) FROM public.app_settings a      WHERE a.user_id = u.id) AS settings,
    (SELECT bool_or(x.email IS NOT NULL) FROM public.admins x WHERE x.user_id = u.id) AS is_admin
FROM auth.users u
ORDER BY u.created_at;

-- ------------------------------------------------------------
-- ÉTAPE 2 — SUPPRESSION des comptes de test (ACTIF)
-- Liste d'emails à supprimer (voir ÉTAPE 1 pour la liste réelle).
-- Les tables app ont `REFERENCES auth.users(id) ON DELETE CASCADE` :
-- la suppression du compte emporte produits, ventes, articles, clients,
-- dettes, réglages, tokens, logs et tickets. Ne touche PAS aux autres
-- comptes, ni à l'audit (admin_actions / notification_log) — volontaire.
--
-- ⚠️ SÉCURITÉ : tout compte présent dans public.admins (admin du
-- back-office) est SKIPPÉ — on ne supprime jamais l'accès admin.
-- Si l'un des emails ci-dessous est ton compte admin, il restera.
-- ------------------------------------------------------------
DO $$
DECLARE
    test_emails text[] := ARRAY[
        'ahmadsouleymane1302@gmail.com',
        'demo@lissafi.app',
        'test@lissafi.app',
        'hasanazzir@gmail.com',
        'alhousseinimoussa48@gmail.com'
    ];
    deleted int;
    u record;
BEGIN
    -- Contrôle préalable : signale les comptes admin (non supprimés).
    FOR u IN SELECT au.email
             FROM auth.users au
             JOIN public.admins a ON a.user_id = au.id
             WHERE au.email = ANY(test_emails)
    LOOP
        RAISE NOTICE '⚠️ % est un ADMIN back-office — conservé', u.email;
    END LOOP;

    -- Suppression cascade, sauf les admins back-office.
    DELETE FROM auth.users
    WHERE email = ANY(test_emails)
      AND id NOT IN (SELECT user_id FROM public.admins);

    GET DIAGNOSTICS deleted = ROW_COUNT;
    RAISE NOTICE '% compte(s) supprimé(s) (cascade).', deleted;
END $$;

-- Puis (si tu veux un compte démo propre et frais) relance :
--   scripts/seed-demo.sql  → recrée demo@lissafi.app / demo123456 (boutique fictive)
--   (il est idempotent et ne touche que le compte démo)

-- ------------------------------------------------------------
-- ÉTAPE 3 — Compte démo dédié
-- Pour (re)créer un compte démo propre (boutique fictive complète) :
--   scripts/seed-demo.sql  (demo@lissafi.app / demo123456)
-- Ce script est idempotent et ne touche QUE les données du compte démo.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- ÉTAPE 4 — EXCLURE LE COMPTE DÉMO DES STATS DU BACK-OFFICE
-- Sans ça, les démos gonfleraient le dashboard (utilisateurs, CA,
-- ventes, premium…). Ajoute les emails démo à exclure ci-dessous.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_demo_user(uid uuid)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT EXISTS (
        SELECT 1 FROM auth.users u
        WHERE u.id = uid
          AND u.email IN ('demo@lissafi.app')          -- ← à étendre si besoin
    );
$func$;

CREATE OR REPLACE FUNCTION public.admin_stats()
RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, auth
AS $func$
    SELECT jsonb_build_object(
        'users',            (SELECT count(*) FROM auth.users u WHERE NOT public.is_demo_user(u.id)),
        'users_today',      (SELECT count(*) FROM auth.users u WHERE NOT public.is_demo_user(u.id) AND u.created_at >= date_trunc('day', now())),
        'active_today',     (SELECT count(*) FROM auth.users u WHERE NOT public.is_demo_user(u.id) AND u.last_sign_in_at >= date_trunc('day', now())),
        'premium',          (SELECT count(*) FROM app_settings a JOIN auth.users u ON u.id = a.user_id WHERE a.key = 'is_premium' AND a.value = 'true' AND NOT public.is_demo_user(u.id)),
        'demo',             (SELECT count(*) FROM app_settings a JOIN auth.users u ON u.id = a.user_id WHERE a.key = 'demo_taken' AND a.value = 'true' AND NOT public.is_demo_user(u.id)),
        'products',         (SELECT count(*) FROM products p JOIN auth.users u ON u.id = p.user_id WHERE NOT public.is_demo_user(u.id)),
        'sales',            (SELECT count(*) FROM sales s JOIN auth.users u ON u.id = s.user_id WHERE NOT public.is_demo_user(u.id)),
        'sales_today',      (SELECT count(*) FROM sales s JOIN auth.users u ON u.id = s.user_id WHERE NOT public.is_demo_user(u.id) AND s.date >= (extract(epoch FROM date_trunc('day', now())) * 1000)::bigint),
        'sales_total_fcfa', (SELECT coalesce(sum(s.total), 0) FROM sales s JOIN auth.users u ON u.id = s.user_id WHERE NOT public.is_demo_user(u.id)),
        'sales_today_fcfa', (SELECT coalesce(sum(s.total), 0) FROM sales s JOIN auth.users u ON u.id = s.user_id WHERE NOT public.is_demo_user(u.id) AND s.date >= (extract(epoch FROM date_trunc('day', now())) * 1000)::bigint),
        'credit_total_fcfa',(SELECT coalesce(sum(s.total), 0) FROM sales s JOIN auth.users u ON u.id = s.user_id WHERE s.is_credit = true AND NOT public.is_demo_user(u.id)),
        'debt_total_fcfa',  (SELECT coalesce(sum(c.total_debt), 0) FROM clients c JOIN auth.users u ON u.id = c.user_id WHERE NOT public.is_demo_user(u.id)),
        'clients',          (SELECT count(*) FROM clients c JOIN auth.users u ON u.id = c.user_id WHERE NOT public.is_demo_user(u.id)),
        'debt_transactions',(SELECT count(*) FROM debt_transactions d JOIN auth.users u ON u.id = d.user_id WHERE NOT public.is_demo_user(u.id)),
        'receipts',         (SELECT count(*) FROM app_logs l JOIN auth.users u ON u.id = l.user_id WHERE l.event_type = 'receipt' AND NOT public.is_demo_user(u.id)),
        'logs',             (SELECT count(*) FROM app_logs l JOIN auth.users u ON u.id = l.user_id WHERE NOT public.is_demo_user(u.id)),
        'errors_24h',       (SELECT count(*) FROM app_logs l JOIN auth.users u ON u.id = l.user_id WHERE l.level = 'error' AND l.created_at >= (extract(epoch FROM now() - interval '24 hours') * 1000)::bigint AND NOT public.is_demo_user(u.id)),
        'tickets_open',     (SELECT count(*) FROM support_tickets t JOIN auth.users u ON u.id = t.user_id WHERE t.status IN ('open','in_progress') AND NOT public.is_demo_user(u.id)),
        'premium_expiring_30d',
            (SELECT count(*)
             FROM app_settings a
             JOIN auth.users u ON u.id = a.user_id
             JOIN app_settings e ON e.key = 'premium_expiry' AND e.user_id = a.user_id
             WHERE a.key = 'is_premium' AND a.value = 'true'
               AND e.value <> '' AND e.value ~ '^[0-9]+$'
               AND e.value::bigint > 0
               AND e.value::bigint <= (extract(epoch FROM now() + interval '30 days') * 1000)::bigint
               AND NOT public.is_demo_user(u.id))
    );
$func$;

-- Même exclusion dans la liste des comptes (page /comptes et /premium)
CREATE OR REPLACE FUNCTION public.admin_user_summaries()
RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, auth
AS $func$
    SELECT coalesce(jsonb_agg(row_to_json(x) ORDER BY x.created_at DESC), '[]'::jsonb)
    FROM (
        SELECT
            u.id AS user_id,
            u.email,
            u.created_at,
            u.last_sign_in_at,
            coalesce(s.shop_name, '') AS shop_name,
            coalesce(s.shop_phone, '') AS shop_phone,
            coalesce(s.is_premium, 'false') = 'true' AS premium,
            coalesce(
                (CASE WHEN s.premium_expiry ~ '^[0-9]+$' THEN s.premium_expiry::bigint ELSE 0 END),
                0
            ) AS premium_expiry,
            coalesce(s.demo_taken, 'false') = 'true' AS demo_taken,
            coalesce(s.activation_code, '') AS activation_code,
            coalesce(p.product_count, 0) AS product_count,
            coalesce(sa.sale_count, 0) AS sale_count,
            coalesce(sa.sales_total, 0) AS sales_total,
            coalesce(c.client_count, 0) AS client_count,
            coalesce(r.receipt_count, 0) AS receipt_count
        FROM auth.users u
        LEFT JOIN (
            SELECT user_id,
                   max(value) FILTER (WHERE key = 'shop_name') AS shop_name,
                   max(value) FILTER (WHERE key = 'shop_phone') AS shop_phone,
                   max(value) FILTER (WHERE key = 'is_premium') AS is_premium,
                   max(value) FILTER (WHERE key = 'premium_expiry') AS premium_expiry,
                   max(value) FILTER (WHERE key = 'demo_taken') AS demo_taken,
                   max(value) FILTER (WHERE key = 'activation_code') AS activation_code
            FROM app_settings
            GROUP BY user_id
        ) s ON s.user_id = u.id
        LEFT JOIN (SELECT user_id, count(*) AS product_count FROM products GROUP BY user_id) p ON p.user_id = u.id
        LEFT JOIN (SELECT user_id, count(*) AS sale_count, coalesce(sum(total), 0) AS sales_total FROM sales GROUP BY user_id) sa ON sa.user_id = u.id
        LEFT JOIN (SELECT user_id, count(*) AS client_count FROM clients GROUP BY user_id) c ON c.user_id = u.id
        LEFT JOIN (SELECT user_id, count(*) AS receipt_count FROM app_logs WHERE event_type = 'receipt' GROUP BY user_id) r ON r.user_id = u.id
        WHERE NOT public.is_demo_user(u.id)
    ) x;
$func$;
