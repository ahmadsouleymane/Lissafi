#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scrapeur Google Maps local — Lissafi prospection.

Usage :
    python3 google_maps_scraper.py "épicerie" "Niamey" --max 50 -o leads.csv

Prérequis :
    pip install playwright
    playwright install chromium

Notes importantes :
    - Scraper Google Maps viole les conditions d'utilisation de Google.
      Reste à PETIT volume, espace les requêtes, et utilise le résultat comme
      piste, pas comme base de données massives.
    - Ta propre IP est exposée : au-delà de quelques centaines de résultats,
      passe par une API (Outscraper) qui gère les rotations d'IP.
"""

import argparse
import asyncio
import csv
import re
import random
import sys

from playwright.async_api import async_playwright


def normalize_phone(raw: str) -> str:
    """Met le numéro au format international +227 pour le Niger."""
    if not raw:
        return ""
    digits = re.sub(r"\D", "", raw)
    if not digits:
        return ""
    if len(digits) == 8:  # numéro local nigérien (8 chiffres)
        return "+227" + digits
    if digits.startswith("227") and len(digits) == 11:
        return "+" + digits
    if digits.startswith("00227"):
        return "+" + digits[2:]
    return digits


async def scrape(query: str, max_results: int, out_path: str) -> None:
    results = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(
            locale="fr-FR", viewport={"width": 1280, "height": 900}
        )
        await page.goto("https://www.google.com/maps")
        await page.wait_for_timeout(2500)

        box = page.locator("input#searchboxinput, input[name='q']")
        await box.click()
        await box.fill(query)
        await box.press("Enter")
        await page.wait_for_timeout(5000)

        # On cherche les lignes de résultats dans le panneau de gauche.
        result_handles = []
        for _ in range(max_results // 10 + 2):
            result_handles = await page.query_selector_all('a[aria-label]:not([aria-label=""])')
            if len(result_handles) >= max_results:
                break
            # Scroll du panneau de résultats pour charger la suite.
            await page.mouse.wheel(0, 2500)
            await page.wait_for_timeout(1500)

        seen_urls = set()
        for handle in result_handles[: max_results]:
            try:
                await handle.click()
                await page.wait_for_timeout(2500)

                name = await safe_text(page, 'h1.DUwDvf')
                if not name or name in seen_urls:
                    continue
                seen_urls.add(name)

                category = await safe_text(page, 'button.DkEaL')
                address = await safe_text(page, 'button[data-item-id="address"]')
                phone = await safe_text(page, 'button[data-item-id^="phone:tel"]')

                results.append({
                    "nom": name,
                    "type": category,
                    "adresse": address,
                    "telephone": normalize_phone(phone),
                    "statut": "",
                })
                print(f"[{len(results):3d}] {name} — {phone or 'sans tel'}")

                if len(results) >= max_results:
                    break

                await page.wait_for_timeout(random.randint(1500, 3000))  # rester discret
            except Exception:
                continue

        await browser.close()

    if not results:
        print("Aucun résultat trouvé (sélecteurs Google Maps à ajuster ?).")
        return

    with open(out_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["nom", "type", "adresse", "telephone", "statut"])
        writer.writeheader()
        writer.writerows(results)

    print(f"\n{len(results)} prospects écrits dans {out_path}")
    print("Pense à vider les colonnes 'telephone' vides avant d'importer dans Google Sheets.")


async def safe_text(page, selector: str) -> str:
    try:
        locator = page.locator(selector).first
        if await locator.count():
            return (await locator.inner_text()).strip()
    except Exception:
        pass
    return ""


def main() -> None:
    parser = argparse.ArgumentParser(description="Scrapeur Google Maps local pour Lissafi")
    parser.add_argument("query", help="ex: 'épicerie' ou 'quincaillerie'")
    parser.add_argument("location", help="ex: 'Niamey' ou 'Grand Marché Niamey'")
    parser.add_argument("--max", type=int, default=50, help="nombre max de résultats (défaut 50)")
    parser.add_argument("-o", "--out", default="leads.csv", help="fichier CSV de sortie")
    args = parser.parse_args()

    query = f"{args.query} {args.location}"
    print(f"Recherche : {query} (max {args.max})")
    try:
        asyncio.run(scrape(query, args.max, args.out))
    except KeyboardInterrupt:
        sys.exit(1)


if __name__ == "__main__":
    main()
