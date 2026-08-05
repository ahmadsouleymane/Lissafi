-- ============================================================
-- LISSAFI — SEED DE DONNÉES DE DÉMONSTRATION
-- ============================================================
-- À exécuter UNE FOIS dans le SQL Editor de Supabase.
-- Crée le compte démo + une boutique niameyenne fictive remplie :
--   produits, clients, ventes, dettes, paramètres boutique et Premium activé.
--
-- Compte démo créé :
--   Email    : demo@lissafi.app
--   Mot de passe : demo123456
--   (Compte déjà confirmé → connexion immédiate dans l'app)
--
-- Idempotent : on peut le relancer, il réinitialise TOUTES les données
-- du compte démo à l'état ci-dessous (aucune donnée des autres comptes
-- n'est touchée — RLS / filtres user_id).
-- ============================================================

DO $$
DECLARE
  uid uuid;
  base bigint;                     -- timestamp courant en millisecondes
  day bigint := 86400000;          -- 1 jour en ms
  h   bigint := 3600000;           -- 1 heure en ms
  v1 bigint; v2 bigint; v3 bigint; v4 bigint; v5 bigint;
  v6 bigint; v7 bigint; v8 bigint; v9 bigint; v10 bigint;
  v11 bigint; v12 bigint; v13 bigint; v14 bigint; v15 bigint;
  v16 bigint; v17 bigint; v18 bigint; v19 bigint; v20 bigint;
  v21 bigint; v22 bigint; v23 bigint; v24 bigint; v25 bigint;
  v26 bigint; v27 bigint; v28 bigint; v29 bigint; v30 bigint;
  v31 bigint; v32 bigint;
BEGIN
  base := (extract(epoch from now()) * 1000)::bigint;

  -- ============================================================
  -- 1. COMPTE DÉMO
  -- ============================================================
  SELECT id INTO uid FROM auth.users WHERE email = 'demo@lissafi.app';
  IF uid IS NULL THEN
    INSERT INTO auth.users
      (id, email, encrypted_password, email_confirmed_at, aud, role)
    VALUES
      (gen_random_uuid(), 'demo@lissafi.app', crypt('demo123456', gen_salt('bf')), now(), 'authenticated', 'authenticated')
    RETURNING id INTO uid;
  END IF;

  -- Mot de passe toujours remis à zéro (utile si on relance après un test)
  UPDATE auth.users
     SET encrypted_password = crypt('demo123456', gen_salt('bf')),
         email_confirmed_at = COALESCE(email_confirmed_at, now()),
         aud                = 'authenticated',
         role               = 'authenticated'
   WHERE email = 'demo@lissafi.app';

  -- Ligne identities (nécessaire sur les versions récentes de GoTrue)
  INSERT INTO auth.identities
    (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
  VALUES
    (uid::text, uid, jsonb_build_object('sub', uid::text, 'email', 'demo@lissafi.app'), 'email', now(), now(), now())
  ON CONFLICT DO NOTHING;

  RAISE NOTICE 'Compte démo : demo@lissafi.app / demo123456 (uid %)', uid;

  -- ============================================================
  -- 2. NETTOYAGE (réinitialise la démo — ordre respecte les FK)
  -- ============================================================
  DELETE FROM sale_items        WHERE user_id = uid;
  DELETE FROM debt_transactions WHERE user_id = uid;
  DELETE FROM sales             WHERE user_id = uid;
  DELETE FROM clients           WHERE user_id = uid;
  DELETE FROM products          WHERE user_id = uid;
  DELETE FROM app_settings      WHERE user_id = uid;

  -- ============================================================
  -- 3. PARAMÈTRES BOUTIQUE + PREMIUM
  -- ============================================================
  INSERT INTO app_settings (key, value, user_id) VALUES
    ('shop_name',        'Boutique Albaraka',            uid),
    ('shop_phone',       '+227 91 12 34 56',             uid),
    ('admin_pin',        '1234',                         uid),
    ('is_premium',       'true',                         uid),
    ('premium_expiry',   (base + 365 * day)::text,       uid),
    ('activation_code',  'LISSAFI-PREMIUM-0001',         uid),
    ('demo_taken',       'true',                         uid);

  -- ============================================================
  -- 4. PRODUITS (23 articles — prix réels FCFA, Niamey)
  -- ============================================================
  INSERT INTO products
    (barcode, name, sell_price, buy_price, stock, min_stock, category, has_barcode, created_at, updated_at, user_id)
  VALUES
    ('6111431100015', 'Riz parfumé 25kg',      10500, 9500, 12,  3,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100022', 'Huile végétale 1L',      1000,  850, 40, 10,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100039', 'Sucre en poudre 1kg',     700,  600, 30, 10,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100046', 'Lait Nido 400g',         1850, 1600, 18,  6,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100053', 'Thé vert 250g',          1500, 1200, 22,  8,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100060', 'Pâtes alimentaires 500g', 350,  250, 60, 15,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100077', 'Concentré de tomate',     400,  300, 50, 12,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100084', 'Sel iodé 1kg',            300,  200, 40, 10,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100091', 'Farine de blé 1kg',       600,  450, 20,  8,  'Épicerie', true,  base - 35*day, base - 1*day, uid),
    ('6111431100107', 'Oignons (sac 10kg)',    11000, 9000,  6,  2,  'Épicerie', false, base - 35*day, base - 1*day, uid),
    ('6111431100114', 'Tomates (tas)',           800,  500, 15,  5,  'Épicerie', false, base - 35*day, base - 1*day, uid),
    ('6111431100121', 'Eau minérale 1.5L',       300,  200, 80, 24,  'Boissons', true,  base - 35*day, base - 1*day, uid),
    ('6111431100138', 'Coca-Cola 33cl',          500,  300, 48, 12,  'Boissons', true,  base - 35*day, base - 1*day, uid),
    ('6111431100145', 'Jus de mangue 1L',       1500, 1100,  9, 10,  'Boissons', true,  base - 35*day, base - 1*day, uid),
    ('6111431100152', 'Savon en poudre 500g',    650,  500, 25,  8,  'Hygiène',  true,  base - 35*day, base - 1*day, uid),
    ('6111431100169', 'Savon de toilette 250g',  500,  350, 35, 10,  'Hygiène',  true,  base - 35*day, base - 1*day, uid),
    ('6111431100176', 'Piles AAA (paquet)',      750,  500, 30,  6,  'Divers',   true,  base - 35*day, base - 1*day, uid),
    ('6111431100183', 'Allumettes (paquet)',     100,   50, 100, 20,  'Divers',   false, base - 35*day, base - 1*day, uid),
    ('6111431100190', 'Bonbons (sachet)',        500,  350, 40, 10,  'Divers',   false, base - 35*day, base - 1*day, uid),
    ('6111431100206', 'Cahier 200 pages',        600,  400, 55, 10,  'Scolaire', true,  base - 35*day, base - 1*day, uid),
    ('6111431100213', 'Stylo à bille',           200,  100, 100, 20,  'Scolaire', true,  base - 35*day, base - 1*day, uid),
    ('6111431100220', 'Pain (baguette)',         250,  150,  0, 10,  'Épicerie', false, base - 35*day, base - 1*day, uid),
    ('6111431100237', 'Sachet d''eau (pack)',    500,  350,  3, 10,  'Boissons', false, base - 35*day, base - 1*day, uid);

  -- ============================================================
  -- 5. CLIENTS (8 — dont 5 avec des dettes cohérentes)
  --    total_debt = somme des debt_transactions du client
  -- ============================================================
  INSERT INTO clients (id, name, phone, total_debt, created_at, updated_at, user_id) VALUES
    ('10000000-0000-4000-8000-000000000001', 'Amadou Salifou',   '+227 91 22 33 44', 12000, base - 35*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000002', 'Mariama Issoufou', '+227 96 55 44 33',  8500, base - 32*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000003', 'Ibrahim Moussa',   '+227 97 88 99 00',     0, base - 30*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000004', 'Fati Seyni',       '+227 90 12 34 56', 15000, base - 28*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000005', 'Habibou Oumarou',  '+227 93 45 67 89',     0, base - 25*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000006', 'Zeinabou Abdou',   '+227 92 10 98 76',  6500, base - 22*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000007', 'Moussa Boubacar',  '+227 94 32 10 98',     0, base - 18*day, base - 1*day, uid),
    ('10000000-0000-4000-8000-000000000008', 'Rakia Ali',        '+227 91 67 89 01', 20000, base - 15*day, base - 1*day, uid);

  -- ============================================================
  -- 6. VENTES + ARTICLES (32 ventes sur ~26 jours)
  --    Les ventes à crédit créent leur transaction de dette
  --    (note "Vente N°<id>" comme le fait l'app).
  -- ============================================================

  -- Vente 1 — d26, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 26*day + 8.5*h, 13200, 15000, 1800, false, NULL, true, uid) RETURNING id INTO v1;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v1, '6111431100015', 'Riz parfumé 25kg',     10500, 1, uid),
    (v1, '6111431100022', 'Huile végétale 1L',     1000, 2, uid),
    (v1, '6111431100039', 'Sucre en poudre 1kg',    700, 1, uid);

  -- Vente 2 — d25, CRÉDIT Mariama (a2) 4750
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 25*day + 10.25*h, 4750, 0, 0, true, '10000000-0000-4000-8000-000000000002', true, uid) RETURNING id INTO v2;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v2, '6111431100046', 'Lait Nido 400g',         1850, 1, uid),
    (v2, '6111431100039', 'Sucre en poudre 1kg',     700, 2, uid),
    (v2, '6111431100053', 'Thé vert 250g',          1500, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000002', v2, 4750, base - 25*day + 10.25*h, format('Vente N°%s', v2), uid);

  -- Vente 3 — d24, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 24*day + 9.75*h, 3650, 5000, 1350, false, NULL, true, uid) RETURNING id INTO v3;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v3, '6111431100077', 'Concentré de tomate',      400, 5, uid),
    (v3, '6111431100084', 'Sel iodé 1kg',             300, 2, uid),
    (v3, '6111431100060', 'Pâtes alimentaires 500g',  350, 3, uid);

  -- Vente 4 — d23, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 23*day + 11*h, 4400, 5000, 600, false, NULL, true, uid) RETURNING id INTO v4;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v4, '6111431100138', 'Coca-Cola 33cl',           500, 6, uid),
    (v4, '6111431100121', 'Eau minérale 1.5L',        300, 4, uid),
    (v4, '6111431100183', 'Allumettes (paquet)',      100, 2, uid);

  -- Vente 5 — d22, CRÉDIT Amadou (a1) 4350
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 22*day + 16.33*h, 4350, 0, 0, true, '10000000-0000-4000-8000-000000000001', true, uid) RETURNING id INTO v5;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v5, '6111431100039', 'Sucre en poudre 1kg',     700,  2, uid),
    (v5, '6111431100053', 'Thé vert 250g',          1500,  1, uid),
    (v5, '6111431100152', 'Savon en poudre 500g',    650,  1, uid),
    (v5, '6111431100114', 'Tomates (tas)',           800,  2, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000001', v5, 4350, base - 22*day + 16.33*h, format('Vente N°%s', v5), uid);

  -- Vente 6 — d21, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 21*day + 9.17*h, 3700, 5000, 1300, false, NULL, true, uid) RETURNING id INTO v6;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v6, '6111431100091', 'Farine de blé 1kg',       600, 2, uid),
    (v6, '6111431100039', 'Sucre en poudre 1kg',     700, 3, uid),
    (v6, '6111431100077', 'Concentré de tomate',      400, 1, uid);

  -- Vente 7 — d20, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 20*day + 10.67*h, 3950, 4000, 50, false, NULL, true, uid) RETURNING id INTO v7;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v7, '6111431100091', 'Farine de blé 1kg',       600, 2, uid),
    (v7, '6111431100039', 'Sucre en poudre 1kg',     700, 3, uid),
    (v7, '6111431100152', 'Savon en poudre 500g',    650, 1, uid);

  -- Vente 8 — d19, CRÉDIT Mariama (a2) 3750
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 19*day + 15*h, 3750, 0, 0, true, '10000000-0000-4000-8000-000000000002', true, uid) RETURNING id INTO v8;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v8, '6111431100022', 'Huile végétale 1L',     1000, 1, uid),
    (v8, '6111431100053', 'Thé vert 250g',         1500, 1, uid),
    (v8, '6111431100152', 'Savon en poudre 500g',   650, 1, uid),
    (v8, '6111431100169', 'Savon de toilette 250g', 500, 1, uid),
    (v8, '6111431100183', 'Allumettes (paquet)',    100, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000002', v8, 3750, base - 19*day + 15*h, format('Vente N°%s', v8), uid);

  -- Vente 9 — d18, CRÉDIT Rakia (a8) 13900
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 18*day + 10*h, 13900, 0, 0, true, '10000000-0000-4000-8000-000000000008', true, uid) RETURNING id INTO v9;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v9, '6111431100015', 'Riz parfumé 25kg',      10500, 1, uid),
    (v9, '6111431100022', 'Huile végétale 1L',      1000, 2, uid),
    (v9, '6111431100039', 'Sucre en poudre 1kg',     700, 2, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000008', v9, 13900, base - 18*day + 10*h, format('Vente N°%s', v9), uid);

  -- Vente 10 — d17, CRÉDIT Amadou (a1) 4150
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 17*day + 9.5*h, 4150, 0, 0, true, '10000000-0000-4000-8000-000000000001', true, uid) RETURNING id INTO v10;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v10, '6111431100046', 'Lait Nido 400g',        1850, 1, uid),
    (v10, '6111431100039', 'Sucre en poudre 1kg',    700, 2, uid),
    (v10, '6111431100084', 'Sel iodé 1kg',           300, 2, uid),
    (v10, '6111431100183', 'Allumettes (paquet)',    100, 3, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000001', v10, 4150, base - 17*day + 9.5*h, format('Vente N°%s', v10), uid);

  -- Vente 11 — d16, CRÉDIT Fati (a4) 12500
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 16*day + 9.75*h, 12500, 0, 0, true, '10000000-0000-4000-8000-000000000004', true, uid) RETURNING id INTO v11;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v11, '6111431100015', 'Riz parfumé 25kg',     10500, 1, uid),
    (v11, '6111431100022', 'Huile végétale 1L',     1000, 1, uid),
    (v11, '6111431100039', 'Sucre en poudre 1kg',    700, 1, uid),
    (v11, '6111431100084', 'Sel iodé 1kg',           300, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000004', v11, 12500, base - 16*day + 9.75*h, format('Vente N°%s', v11), uid);

  -- Vente 12 — d16, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 16*day + 17.5*h, 2500, 3000, 500, false, NULL, true, uid) RETURNING id INTO v12;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v12, '6111431100220', 'Pain (baguette)',         250, 4, uid),
    (v12, '6111431100138', 'Coca-Cola 33cl',          500, 2, uid),
    (v12, '6111431100169', 'Savon de toilette 250g',  500, 1, uid);

  -- Vente 13 — d15, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 15*day + 9*h, 4050, 5000, 950, false, NULL, true, uid) RETURNING id INTO v13;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v13, '6111431100053', 'Thé vert 250g',          1500, 1, uid),
    (v13, '6111431100039', 'Sucre en poudre 1kg',     700, 1, uid),
    (v13, '6111431100046', 'Lait Nido 400g',         1850, 1, uid);

  -- Vente 14 — d14, CRÉDIT Rakia (a8) 5100
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 14*day + 9.67*h, 5100, 0, 0, true, '10000000-0000-4000-8000-000000000008', true, uid) RETURNING id INTO v14;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v14, '6111431100046', 'Lait Nido 400g',        1850, 1, uid),
    (v14, '6111431100053', 'Thé vert 250g',         1500, 1, uid),
    (v14, '6111431100152', 'Savon en poudre 500g',   650, 1, uid),
    (v14, '6111431100114', 'Tomates (tas)',          800, 2, uid),
    (v14, '6111431100084', 'Sel iodé 1kg',           300, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000008', v14, 5100, base - 14*day + 9.67*h, format('Vente N°%s', v14), uid);

  -- Vente 15 — d13, CRÉDIT Zeinabou (a6) 5750
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 13*day + 9.33*h, 5750, 0, 0, true, '10000000-0000-4000-8000-000000000006', true, uid) RETURNING id INTO v15;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v15, '6111431100046', 'Lait Nido 400g',        1850, 1, uid),
    (v15, '6111431100022', 'Huile végétale 1L',     1000, 1, uid),
    (v15, '6111431100039', 'Sucre en poudre 1kg',    700, 2, uid),
    (v15, '6111431100053', 'Thé vert 250g',         1500, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000006', v15, 5750, base - 13*day + 9.33*h, format('Vente N°%s', v15), uid);

  -- Vente 16 — d13, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 13*day + 12.17*h, 3500, 3500, 0, false, NULL, true, uid) RETURNING id INTO v16;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v16, '6111431100206', 'Cahier 200 pages',       600, 3, uid),
    (v16, '6111431100213', 'Stylo à bille',          200, 5, uid),
    (v16, '6111431100060', 'Pâtes alimentaires 500g', 350, 2, uid);

  -- Vente 17 — d12, CRÉDIT Rakia (a8) 2500
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 12*day + 8.67*h, 2500, 0, 0, true, '10000000-0000-4000-8000-000000000008', true, uid) RETURNING id INTO v17;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v17, '6111431100022', 'Huile végétale 1L',     1000, 1, uid),
    (v17, '6111431100091', 'Farine de blé 1kg',      600, 1, uid),
    (v17, '6111431100138', 'Coca-Cola 33cl',         500, 1, uid),
    (v17, '6111431100121', 'Eau minérale 1.5L',      300, 1, uid),
    (v17, '6111431100183', 'Allumettes (paquet)',    100, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000008', v17, 2500, base - 12*day + 8.67*h, format('Vente N°%s', v17), uid);

  -- Vente 18 — d12, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 12*day + 15.33*h, 4700, 5000, 300, false, NULL, true, uid) RETURNING id INTO v18;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v18, '6111431100152', 'Savon en poudre 500g',    650, 2, uid),
    (v18, '6111431100121', 'Eau minérale 1.5L',       300, 6, uid),
    (v18, '6111431100114', 'Tomates (tas)',           800, 4, uid);

  -- Vente 19 — d11, CRÉDIT Fati (a4) 2850
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 11*day + 10*h, 2850, 0, 0, true, '10000000-0000-4000-8000-000000000004', true, uid) RETURNING id INTO v19;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v19, '6111431100046', 'Lait Nido 400g',         1850, 1, uid),
    (v19, '6111431100022', 'Huile végétale 1L',      1000, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000004', v19, 2850, base - 11*day + 10*h, format('Vente N°%s', v19), uid);

  -- Vente 20 — d11, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 11*day + 16*h, 13900, 15000, 1100, false, NULL, true, uid) RETURNING id INTO v20;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v20, '6111431100015', 'Riz parfumé 25kg',     10500, 1, uid),
    (v20, '6111431100022', 'Huile végétale 1L',     1000, 2, uid),
    (v20, '6111431100039', 'Sucre en poudre 1kg',    700, 2, uid);

  -- Vente 21 — d10, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 10*day + 9.25*h, 3700, 5000, 1300, false, NULL, true, uid) RETURNING id INTO v21;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v21, '6111431100060', 'Pâtes alimentaires 500g', 350, 4, uid),
    (v21, '6111431100114', 'Tomates (tas)',           800, 3, uid),
    (v21, '6111431100084', 'Sel iodé 1kg',            300, 2, uid),
    (v21, '6111431100183', 'Allumettes (paquet)',     100, 5, uid);

  -- Vente 22 — d9, CRÉDIT Zeinabou (a6) 750
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 9*day + 9.5*h, 750, 0, 0, true, '10000000-0000-4000-8000-000000000006', true, uid) RETURNING id INTO v22;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v22, '6111431100060', 'Pâtes alimentaires 500g', 350, 1, uid),
    (v22, '6111431100077', 'Concentré de tomate',     400, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000006', v22, 750, base - 9*day + 9.5*h, format('Vente N°%s', v22), uid);

  -- Vente 23 — d9, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 9*day + 11.83*h, 6700, 7000, 300, false, NULL, true, uid) RETURNING id INTO v23;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v23, '6111431100145', 'Jus de mangue 1L',        1500, 3, uid),
    (v23, '6111431100138', 'Coca-Cola 33cl',           500, 2, uid),
    (v23, '6111431100121', 'Eau minérale 1.5L',        300, 4, uid);

  -- Vente 24 — d8, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 8*day + 10.75*h, 6100, 7000, 900, false, NULL, true, uid) RETURNING id INTO v24;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v24, '6111431100053', 'Thé vert 250g',           1500, 2, uid),
    (v24, '6111431100039', 'Sucre en poudre 1kg',      700, 3, uid),
    (v24, '6111431100169', 'Savon de toilette 250g',   500, 2, uid);

  -- Vente 25 — d7, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 7*day + 9.83*h, 4600, 5000, 400, false, NULL, true, uid) RETURNING id INTO v25;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v25, '6111431100091', 'Farine de blé 1kg',       600, 3, uid),
    (v25, '6111431100121', 'Eau minérale 1.5L',       300, 6, uid),
    (v25, '6111431100084', 'Sel iodé 1kg',            300, 2, uid),
    (v25, '6111431100183', 'Allumettes (paquet)',     100, 4, uid);

  -- Vente 26 — d6, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 6*day + 10.17*h, 4550, 5000, 450, false, NULL, true, uid) RETURNING id INTO v26;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v26, '6111431100138', 'Coca-Cola 33cl',          500, 3, uid),
    (v26, '6111431100206', 'Cahier 200 pages',        600, 2, uid),
    (v26, '6111431100213', 'Stylo à bille',           200, 4, uid),
    (v26, '6111431100060', 'Pâtes alimentaires 500g', 350, 3, uid);

  -- Vente 27 — d5, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 5*day + 8.25*h, 4500, 5000, 500, false, NULL, true, uid) RETURNING id INTO v27;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v27, '6111431100039', 'Sucre en poudre 1kg',     700, 3, uid),
    (v27, '6111431100053', 'Thé vert 250g',          1500, 1, uid),
    (v27, '6111431100121', 'Eau minérale 1.5L',       300, 3, uid);

  -- Vente 28 — d4, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 4*day + 9.08*h, 6000, 6000, 0, false, NULL, true, uid) RETURNING id INTO v28;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v28, '6111431100046', 'Lait Nido 400g',         1850, 2, uid),
    (v28, '6111431100022', 'Huile végétale 1L',      1000, 2, uid),
    (v28, '6111431100084', 'Sel iodé 1kg',            300, 1, uid);

  -- Vente 29 — d3, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 3*day + 11.33*h, 11950, 15000, 3050, false, NULL, true, uid) RETURNING id INTO v29;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v29, '6111431100015', 'Riz parfumé 25kg',      10500, 1, uid),
    (v29, '6111431100152', 'Savon en poudre 500g',    650, 1, uid),
    (v29, '6111431100114', 'Tomates (tas)',           800, 2, uid);

  -- Vente 30 — d2, CRÉDIT Amadou (a1) 3800
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 2*day + 10*h, 3800, 0, 0, true, '10000000-0000-4000-8000-000000000001', true, uid) RETURNING id INTO v30;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v30, '6111431100039', 'Sucre en poudre 1kg',     700, 2, uid),
    (v30, '6111431100114', 'Tomates (tas)',           800, 2, uid),
    (v30, '6111431100169', 'Savon de toilette 250g',  500, 1, uid),
    (v30, '6111431100121', 'Eau minérale 1.5L',       300, 2, uid),
    (v30, '6111431100138', 'Coca-Cola 33cl',          500, 1, uid);
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id)
  VALUES ('10000000-0000-4000-8000-000000000001', v30, 3800, base - 2*day + 10*h, format('Vente N°%s', v30), uid);

  -- Vente 31 — d1, comptant
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 1*day + 9.5*h, 3200, 3500, 300, false, NULL, true, uid) RETURNING id INTO v31;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v31, '6111431100169', 'Savon de toilette 250g',  500, 2, uid),
    (v31, '6111431100060', 'Pâtes alimentaires 500g', 350, 2, uid),
    (v31, '6111431100084', 'Sel iodé 1kg',            300, 2, uid),
    (v31, '6111431100121', 'Eau minérale 1.5L',       300, 3, uid);

  -- Vente 32 — aujourd'hui (comptant) : la démo "directe" du jour
  INSERT INTO sales (date, total, amount_paid, change_given, is_credit, client_id, synced, user_id)
  VALUES (base - 3*h, 12600, 15000, 2400, false, NULL, true, uid) RETURNING id INTO v32;
  INSERT INTO sale_items (sale_id, barcode, name, price, quantity, user_id) VALUES
    (v32, '6111431100015', 'Riz parfumé 25kg',     10500, 1, uid),
    (v32, '6111431100022', 'Huile végétale 1L',     1000, 1, uid),
    (v32, '6111431100039', 'Sucre en poudre 1kg',    700, 1, uid),
    (v32, '6111431100077', 'Concentré de tomate',    400, 1, uid);

  -- ============================================================
  -- 7. REMBOURSEMENTS DE DETTES (transactions négatives)
  --    total_debt de chaque client = somme de ses transactions.
  -- ============================================================
  INSERT INTO debt_transactions (client_id, sale_id, amount, date, note, user_id) VALUES
    -- Amadou (a1) : 4350 + 4150 + 3800 - 300 = 12000
    ('10000000-0000-4000-8000-000000000001', NULL, -300,  base - 1*day + 12*h, 'Remboursement', uid),
    -- Fati (a4) : 12500 + 2850 - 350 = 15000
    ('10000000-0000-4000-8000-000000000004', NULL, -350,  base - 6*day + 16*h, 'Remboursement', uid),
    -- Rakia (a8) : 13900 + 5100 + 2500 - 1500 = 20000
    ('10000000-0000-4000-8000-000000000008', NULL, -1500, base - 5*day + 15*h, 'Remboursement', uid);

  RAISE NOTICE 'Seed Lissafi terminé : boutique remplie pour demo@lissafi.app';
END $$;
