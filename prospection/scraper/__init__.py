"""Package scrapeur Lissafi (Google Maps / Places API).

Sources :
  - places_api : Google Places API (légale, JSON structuré, clé requise)
  - maps       : Google Maps UI via Playwright (gratuit, sans clé)

Voir main.py pour les commandes.
"""

from .env import load_env

# Charge prospection/.env dès l'import du package (clés DeepSeek, Places…).
load_env()

