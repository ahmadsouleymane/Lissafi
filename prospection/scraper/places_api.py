"""Source 1 — Google Places API (légale, JSON structuré).

Nécessite une clé : export GOOGLE_PLACES_API_KEY=...
La clé inclut un crédit gratuit (~200 $/mois). On économise la quota : la fiche
détail (téléphone, horaires) n'est appelée que pour les candidats plausibles.
"""

import json
import os
import time
import urllib.parse
import urllib.request

from . import db, verify
from .http import urlopen
from .verify import normalize_phone

BASE = "https://maps.googleapis.com/maps/api/place"


def _get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "lissafi-scraper/0.1"})
    with urlopen(req) as r:
        return json.loads(r.read().decode())


def _key():
    key = os.environ.get("GOOGLE_PLACES_API_KEY")
    if not key:
        raise SystemExit(
            "GOOGLE_PLACES_API_KEY manquant — export GOOGLE_PLACES_API_KEY=..."
        )
    return key


def _from_result(p):
    loc = p["geometry"]["location"]
    return {
        "id": f"api_{p['place_id']}",
        "source": "places_api",
        "name": p.get("name", ""),
        "category": ", ".join(p.get("types") or []),
        "address": p.get("formatted_address", ""),
        "phone": "",
        "website": "",
        "lat": loc["lat"],
        "lng": loc["lng"],
        "rating": p.get("rating"),
        "review_count": p.get("user_ratings_total"),
        "opening_hours": 1 if p.get("opening_hours") else 0,
        "business_status": p.get("business_status") or "",
    }


def _enrich(row, key):
    """Récupère téléphone / site / horaires via Place Details (1 appel en plus)."""
    pid = row["id"][4:]
    url = (
        f"{BASE}/details/json?place_id={pid}"
        f"&fields=formatted_phone_number,international_phone_number,website,"
        f"opening_hours,business_status&key={key}"
    )
    d = _get(url).get("result", {})
    ph = d.get("international_phone_number") or d.get("formatted_phone_number") or ""
    if ph:
        row["phone"] = normalize_phone(ph)
    row["website"] = d.get("website") or ""
    if d.get("opening_hours"):
        row["opening_hours"] = 1
    if d.get("business_status"):
        row["business_status"] = d["business_status"]


def scrape(query, location, max_results, conn):
    key = _key()
    q = urllib.parse.quote(f"{query} in {location}")
    next_token = None
    n = 0
    while n < max_results:
        url = f"{BASE}/textsearch/json?query={q}&key={key}"
        if next_token:
            url += f"&pagetoken={next_token}"
            time.sleep(2)  # délai imposé par Google avant d'utiliser un pagetoken
        data = _get(url)
        for p in data.get("results", []):
            if n >= max_results:
                break
            n += 1
            row = _from_result(p)
            if verify.looks_targetish(row):
                _enrich(row, key)
            row.update(verify.classify_place(row))
            db.upsert_place(conn, row)
        next_token = data.get("next_page_token")
        if not next_token:
            break
    return n
