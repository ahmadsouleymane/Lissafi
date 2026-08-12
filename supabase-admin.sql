-- ============================================================
-- SCHÉMA BACK-OFFICE LISSAFI (admin web, déployé sur Vercel)
-- ============================================================
-- À exécuter DANS CET ORDRE : supabase-schema.sql PUIS ce script.
-- Idempotent : on peut le relancer sans erreur.
-- ============================================================

-- 1. Comptes administrateurs du back-office
-- Pour te donner accès :
--   INSERT INTO public.admins (user_id, email)
--   VALUES ('<TON_USER_ID_SUPABASE>', 'ton@email.com');
-- (Le user_id est celui de ton compte auth.users)
CREATE TABLE IF NOT EXISTS public.admins (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Helper RLS : l'utilisateur connecté est-il admin ?
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT EXISTS (SELECT 1 FROM public.admins WHERE user_id = auth.uid());
$func$;

-- ============================================================
-- 2. Journal des événements de l'application (logs, erreurs, reçus)
--    Rempli par l'app Android (fire-and-forget, jamais bloquant).
--    event_type : app_start | sync | sync_error | receipt | error | session_invalid | ticket
--    level      : debug | info | warn | error
--    meta       : JSON libre (méthode d'impression, montant, version app…)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.app_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    level TEXT NOT NULL DEFAULT 'info',
    message TEXT NOT NULL DEFAULT '',
    meta TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL
);

ALTER TABLE public.app_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users insert own logs" ON public.app_logs;
CREATE POLICY "users insert own logs" ON public.app_logs
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "users read own logs or admin" ON public.app_logs;
CREATE POLICY "users read own logs or admin" ON public.app_logs
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

CREATE INDEX IF NOT EXISTS idx_app_logs_user ON public.app_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_app_logs_created ON public.app_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_logs_event ON public.app_logs(event_type);

-- ============================================================
-- 3. Signalements & demandes de support (envoyés depuis l'app)
--    status : open | in_progress | resolved | closed
--    priority : low | normal | high | urgent
-- ============================================================
CREATE TABLE IF NOT EXISTS public.support_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    subject TEXT NOT NULL DEFAULT '',
    message TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'open',
    priority TEXT NOT NULL DEFAULT 'normal',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users insert own tickets" ON public.support_tickets;
CREATE POLICY "users insert own tickets" ON public.support_tickets
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "users read own tickets or admin" ON public.support_tickets;
CREATE POLICY "users read own tickets or admin" ON public.support_tickets
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "admins update tickets" ON public.support_tickets;
CREATE POLICY "admins update tickets" ON public.support_tickets
    FOR UPDATE USING (public.is_admin());

CREATE INDEX IF NOT EXISTS idx_tickets_user ON public.support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON public.support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_created ON public.support_tickets(created_at DESC);

-- Réponses aux tickets (du commerçant OU de l'admin)
CREATE TABLE IF NOT EXISTS public.ticket_replies (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES public.support_tickets(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT false,
    created_at BIGINT NOT NULL
);

ALTER TABLE public.ticket_replies ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "ticket replies insert" ON public.ticket_replies;
CREATE POLICY "ticket replies insert" ON public.ticket_replies
    FOR INSERT WITH CHECK (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "ticket replies read" ON public.ticket_replies;
CREATE POLICY "ticket replies read" ON public.ticket_replies
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

CREATE INDEX IF NOT EXISTS idx_ticket_replies_ticket ON public.ticket_replies(ticket_id);

-- ============================================================
-- 4. Paramètres globaux du back-office (éditables dans /reglages)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.admin_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

INSERT INTO public.admin_settings (key, value) VALUES
    ('premium_price_fcfa', '10000'),
    ('premium_days', '365'),
    ('demo_days', '7'),
    ('recap_notifications_enabled', 'true')
ON CONFLICT (key) DO NOTHING;

-- ============================================================
-- 5. Journal des actions admin (traçabilité : qui a fait quoi)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.admin_actions (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    action TEXT NOT NULL,
    target_user_id UUID,
    details TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_actions_created ON public.admin_actions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_actions_target ON public.admin_actions(target_user_id);

-- ============================================================
-- 5bis. Journal des notifications push (manuelles + récap auto)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.notification_log (
    id BIGSERIAL PRIMARY KEY,
    kind TEXT NOT NULL,                        -- 'manuel' | 'recap_quotidien'
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    admin_user_id UUID,                        -- NULL pour les envois automatiques
    target_summary TEXT NOT NULL DEFAULT '',
    recap_date TEXT,                            -- 'YYYY-MM-DD' (heure Niamey), recap uniquement
    recipients INT NOT NULL DEFAULT 0,
    success INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_log_recap_date
    ON public.notification_log(recap_date) WHERE kind = 'recap_quotidien';
CREATE INDEX IF NOT EXISTS idx_notification_log_created ON public.notification_log(created_at DESC);

-- ============================================================
-- 6. FONCTIONS D'AGRÉGATION (SECURITY DEFINER)
--    Appelables côté serveur via le rôle service — aucun accès
--    direct au schéma auth n'est exposé au client.
--    Délimiteurs $func$ nommés : sans ambiguïté pour le SQL Editor.
-- ============================================================

-- Statistiques globales pour le dashboard
CREATE OR REPLACE FUNCTION public.admin_stats()
RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, auth
AS $func$
    SELECT jsonb_build_object(
        'users',            (SELECT count(*) FROM auth.users),
        'users_today',      (SELECT count(*) FROM auth.users WHERE created_at >= date_trunc('day', now())),
        'active_today',     (SELECT count(*) FROM auth.users WHERE last_sign_in_at >= date_trunc('day', now())),
        'premium',          (SELECT count(*) FROM app_settings WHERE key = 'is_premium' AND value = 'true'),
        'demo',             (SELECT count(*) FROM app_settings WHERE key = 'demo_taken' AND value = 'true'),
        'products',         (SELECT count(*) FROM products),
        'sales',            (SELECT count(*) FROM sales),
        'sales_today',      (SELECT count(*) FROM sales WHERE date >= (extract(epoch FROM date_trunc('day', now())) * 1000)::bigint),
        'sales_total_fcfa', (SELECT coalesce(sum(total), 0) FROM sales),
        'sales_today_fcfa', (SELECT coalesce(sum(total), 0) FROM sales WHERE date >= (extract(epoch FROM date_trunc('day', now())) * 1000)::bigint),
        'credit_total_fcfa',(SELECT coalesce(sum(total), 0) FROM sales WHERE is_credit = true),
        'debt_total_fcfa',  (SELECT coalesce(sum(total_debt), 0) FROM clients),
        'clients',          (SELECT count(*) FROM clients),
        'debt_transactions',(SELECT count(*) FROM debt_transactions),
        'receipts',         (SELECT count(*) FROM app_logs WHERE event_type = 'receipt'),
        'logs',             (SELECT count(*) FROM app_logs),
        'errors_24h',       (SELECT count(*) FROM app_logs WHERE level = 'error' AND created_at >= (extract(epoch FROM now() - interval '24 hours') * 1000)::bigint),
        'tickets_open',     (SELECT count(*) FROM support_tickets WHERE status IN ('open','in_progress')),
        'premium_expiring_30d',
            (SELECT count(*)
             FROM app_settings a
             JOIN app_settings e ON e.key = 'premium_expiry' AND e.user_id = a.user_id
             WHERE a.key = 'is_premium' AND a.value = 'true'
               AND e.value <> '' AND e.value ~ '^[0-9]+$'
               AND e.value::bigint > 0
               AND e.value::bigint <= (extract(epoch FROM now() + interval '30 days') * 1000)::bigint)
    );
$func$;

-- Séries de ventes (N derniers jours) pour le graphique du dashboard
-- Bornes UTC exactes : generate_series renvoie des timestamptz, on compare
-- s.date (epoch millis UTC) aux minuit UTC de chaque jour.
CREATE OR REPLACE FUNCTION public.admin_sales_series(days int DEFAULT 30)
RETURNS TABLE(day date, sales bigint, total numeric)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT d::date AS day,
           count(s.id)::bigint AS sales,
           coalesce(sum(s.total), 0) AS total
    FROM generate_series(
             date_trunc('day', now()) - (days - 1) * interval '1 day',
             date_trunc('day', now()),
             interval '1 day'
         ) d
    LEFT JOIN sales s
      ON s.date >= (extract(epoch FROM d) * 1000)::bigint
     AND s.date <  (extract(epoch FROM d + interval '1 day') * 1000)::bigint
    GROUP BY d
    ORDER BY d;
$func$;

-- Séries d'inscriptions (N derniers jours)
CREATE OR REPLACE FUNCTION public.admin_signups_series(days int DEFAULT 30)
RETURNS TABLE(day date, signups bigint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, auth
AS $func$
    SELECT d::date AS day,
           count(u.id)::bigint AS signups
    FROM generate_series(
             date_trunc('day', now()) - (days - 1) * interval '1 day',
             date_trunc('day', now()),
             interval '1 day'
         ) d
    LEFT JOIN auth.users u
      ON u.created_at >= d AND u.created_at < d + interval '1 day'
    GROUP BY d
    ORDER BY d;
$func$;

-- Liste des comptes + indicateurs agrégés (pour /comptes et /premium)
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
            -- Cast sécurisé : une valeur corrompue ne doit pas faire échouer la liste
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
    ) x;
$func$;

-- Emails par user_id (léger, pour afficher les emails dans les listes)
CREATE OR REPLACE FUNCTION public.admin_user_emails()
RETURNS TABLE(user_id uuid, email text)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = auth
AS $func$
    SELECT id, email FROM auth.users;
$func$;

-- Journal des connexions/auth (auth.audit_log_entries)
-- Schéma RÉEL Supabase : instance_id, id, payload (json), created_at, ip_address.
-- Il n'y a NI colonne auth_event NI colonne user_id : l'événement est dans
-- payload->>'action' et l'utilisateur concerné dans payload->>'user_id'.
CREATE OR REPLACE FUNCTION public.admin_audit_logs(from_ts bigint DEFAULT 0, to_ts bigint DEFAULT 0, lim int DEFAULT 500)
RETURNS TABLE(created_at timestamptz, auth_event text, ip text, user_id uuid, payload jsonb)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = auth
AS $func$
    SELECT a.created_at,
           a.payload->>'action' AS auth_event,
           a.ip_address::text AS ip,
           -- user_id vit dans le payload ; cast sécurisé (UUID valide ou NULL)
           CASE WHEN a.payload->>'user_id' ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
                THEN (a.payload->>'user_id')::uuid
                ELSE NULL END AS user_id,
           a.payload::jsonb AS payload
    FROM auth.audit_log_entries a
    WHERE a.created_at >= CASE WHEN from_ts = 0 THEN '1970-01-01' ELSE to_timestamp(from_ts / 1000.0) END
      AND a.created_at <= CASE WHEN to_ts = 0 THEN now() ELSE to_timestamp(to_ts / 1000.0) END
    ORDER BY a.created_at DESC
    LIMIT CASE WHEN lim <= 0 THEN 1000 ELSE lim END;
$func$;

-- Journal applicatif (app_logs) avec email de l'utilisateur
CREATE OR REPLACE FUNCTION public.admin_logs(
    from_ts bigint DEFAULT 0,
    to_ts bigint DEFAULT 0,
    lvl text DEFAULT '',
    etype text DEFAULT '',
    uid uuid DEFAULT NULL,
    lim int DEFAULT 200
)
RETURNS TABLE(id bigint, user_id uuid, email text, event_type text, level text, message text, meta text, created_at bigint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, auth
AS $func$
    SELECT l.id, l.user_id, u.email, l.event_type, l.level, l.message, l.meta, l.created_at
    FROM app_logs l
    LEFT JOIN auth.users u ON u.id = l.user_id
    WHERE (from_ts = 0 OR l.created_at >= from_ts)
      AND (to_ts = 0 OR l.created_at <= to_ts)
      AND (lvl = '' OR l.level = lvl)
      AND (etype = '' OR l.event_type = etype)
      AND (uid IS NULL OR l.user_id = uid)
    ORDER BY l.created_at DESC
    LIMIT CASE WHEN lim <= 0 THEN 200 ELSE lim END;
$func$;


-- Agrégats de la veille par utilisateur, pour le récap automatique quotidien.
-- Ne renvoie que les utilisateurs ayant eu au moins une vente dans la fenêtre ;
-- les utilisateurs sans vente sont traités côté application (valeurs à 0).
CREATE OR REPLACE FUNCTION public.admin_recap_yesterday(from_ts bigint, to_ts bigint)
RETURNS TABLE(user_id uuid, sales_total bigint, sales_count bigint, new_debts_total bigint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $func$
    SELECT
        s.user_id,
        coalesce(sum(s.total), 0)::bigint AS sales_total,
        count(s.id)::bigint AS sales_count,
        coalesce((
            SELECT sum(dt.amount) FROM debt_transactions dt
            WHERE dt.user_id = s.user_id
              AND dt.date BETWEEN from_ts AND to_ts
              AND dt.amount > 0
        ), 0)::bigint AS new_debts_total
    FROM sales s
    WHERE s.date BETWEEN from_ts AND to_ts
    GROUP BY s.user_id;
$func$;

-- ============================================================
-- 7. MISE À JOUR du schéma existant : index sur app_settings pour les pivots
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_app_settings_key ON public.app_settings(key);
