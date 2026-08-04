#!/usr/bin/env python3
"""Scrape continu : parcourt les requêtes Google Maps (Niamey) sans fin.

Chaque requête est espacée d'une pause pour rester discret (pas de ban IP).
Les doublons sont écartés par la base (UNIQUE name+address).

Usage :
    python3 -m scraper.scrape_loop --max 20 --pause 60
"""

import argparse
import asyncio
import time

from . import db, maps_scraper

# Requêtes (français + anglais) couvrant les types cibles à Niamey.
QUERIES = [
    "épicerie", "grocery store", "supermarché", "supermarket",
    "boutique", "magasin", "shop",
    "restaurant", "resto", "maquis", "café",
    "quincaillerie", "hardware store",
    "boulangerie", "bakery", "pâtisserie",
    "pharmacie", "pharmacy",
    "salon de coiffure", "coiffeur", "barbershop",
    "vente de tissus", "tissus", "fabrics",
    "bijouterie", "librairie", "libraire",
    "prêt-à-porter", "vêtements", "clothing store",
    "électroménager", "appliance store",
    "téléphonie", "téléphone", "phone shop", "boutique de téléphone",
    "cyber café", "internet cafe",
    "boucherie", "poissonnerie",
    "station service", "essence",
    "droguerie", "bazar", "friperie",
    # Variantes géo pour couvrir les quartiers
    "épicerie grand marché", "restaurant grand marché",
    "boutique grand marché", "quincaillerie grand marché",
    "pharmacie grand marché", "salon grand marché",
]


def run(max_results: int, pause: int) -> None:
    conn = db.connect()
    total = 0
    i = 0
    while True:
        q = QUERIES[i % len(QUERIES)]
        try:
            n = asyncio.run(maps_scraper.scrape(q, "Niamey", max_results, conn))
            total += n
            print(f"[{i+1}] {q}: {n} places (cumul {total})", flush=True)
        except Exception as e:  # noqa: BLE001
            print(f"[{i+1}] {q}: ERREUR {e}", flush=True)
        i += 1
        time.sleep(pause)


def main() -> None:
    ap = argparse.ArgumentParser(description="Scrape Google Maps en continu (Niamey)")
    ap.add_argument("--max", type=int, default=20, help="résultats max par requête")
    ap.add_argument("--pause", type=int, default=60, help="secondes entre requêtes")
    args = ap.parse_args()
    try:
        run(args.max, args.pause)
    except KeyboardInterrupt:
        print("\nArrêt du scrape continu.")


if __name__ == "__main__":
    main()
