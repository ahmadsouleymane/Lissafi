# Lissafi — Prospection automatisée (scrape → filtre → messages → envoi)

## Situation actuelle (important)

L'envoi automatisé via le **WhatsApp Cloud API de Meta** exige une
**vérification de business** (que tu n'as pas encore). On passe donc par un
**envoi semi-manuel** : le robot prépare la liste filtrée + un message
personnalisé par prospect, et tu envoies toi-même via l'appli **WhatsApp
Business** en ouvrant des liens **wa.me pré-remplis**. Zéro API Meta, zéro
risque de ban Cloud API.

Le jour où tu auras ta **NINEA** (CCI/CFE Niger), tu pourras reprendre le
workflow n8n déjà construit pour tout automatiser (section n8n plus bas).

## Le flux complet

```
Google Maps / Places API
   ↓  scraper/ (Python)
Base SQLite  lissafi.db
   ↓  filtre « business physique » + type cible
Liste prospects (nom, type, adresse, téléphone)
   ↓  prepare_outreach.py (DeepSeek)
Messages personnalisés + liens wa.me pré-remplis
   ↓  toi, via l'appli WhatsApp Business
Envoi — message déjà écrit, tu tapes juste « envoyer »
```

## 1. Le robot de scraping — `scraper/`

Installation (une fois) :

```bash
pip3 install playwright
python3 -m playwright install chromium
```

Lancer :

```bash
# Source 1 — Google Places API (légale, clé requise)
export GOOGLE_PLACES_API_KEY=TaCle
python3 -m scraper.main places "épicerie" "Niamey" --max 100

# Source 2 — Google Maps UI via Playwright (gratuit, sans clé)
python3 -m scraper.main maps "restaurant" "Niamey" --max 50

# Résumé de la base
python3 -m scraper.main stats
```

**Le filtre « business physique »** : une place est une cible seulement si
adresse de rue réelle + coordonnées GPS dans la boîte de Niamey + catégorie
cible (épicerie, restaurant, quincaillerie…) + pas fermée définitivement + pas
de signaux « en ligne / e-shop / livraison uniquement ». Chaque place reçoit un
score. Tout est réglable dans `scraper/config.py`.

## 2. Préparer les messages — `prepare_outreach.py`

```bash
export DEEPSEEK_API_KEY=TaCle
python3 -m scraper.prepare_outreach            # messages DeepSeek + liens wa.me
python3 -m scraper.prepare_outreach --dry      # test sans API (message modèle)
python3 -m scraper.prepare_outreach -o mes_prospects.csv
```

Produit `outreach_ready.csv` : `nom | type | adresse | telephone | message | lien`.

- `lien` = `https://wa.me/227XXXXXXXX?text=...` → message **pré-rempli**.
- Chaque message généré est **enregistré en base** : relancer le script ne
  re-appelle pas DeepSeek pour les prospects déjà préparés.
- `--dry` utilise un message modèle pour tester le flux sans dépenser d'API.

## 3. Envoi (semi-manuel, via l'appli WhatsApp Business)

1. Ouvre `outreach_ready.csv` dans un tableur.
2. Clique le lien `lien` du premier prospect → WhatsApp Business s'ouvre avec le
   message déjà écrit.
3. Personnalise un mot si besoin, tape **Envoyer**.
4. Passe au suivant (≈ 20-40 envois/heure).
5. Si un numéro n'est pas sur WhatsApp, le lien ne connecte pas : passe.

Astuce : garde une copie de ta liste dans une Google Sheet (colonnes
`nom | type | adresse | telephone | statut`) pour tracer les envois —
`envoyé / rdv / refusé`.

## n8n (pour plus tard, quand business enregistré)

Le workflow `lissafi-prospection-workflow.json` automatise tout : vérif numéro
via le Cloud API, message DeepSeek, envoi via template approuvé, log Google
Sheets. Il n'est utilisable qu'avec un **Cloud API vérifié** (NINEA). L'instance
n8n est installée en local dans `n8n-instance/` :
`cd n8n-instance && n8n start` → http://localhost:5678

## Prudence

- Scraper Google Maps viole les conditions d'utilisation de Google : reste à
  **petit volume** et espace les lancements — ta IP est exposée.
- L'envoi de messages non sollicités fait courir un risque de signalement
  WhatsApp Business : reste personnalisé, respecte les « STOP », ne spamme pas.
- **Facebook** : évite le scraping pur (anti-bot violent, données pauvres pour
  l'informel à Niamey). Utilise les API officielles (Pages API) ou tes groupes
  WhatsApp existants.
