"""Base SQLite locale + export vers le format de la feuille « leads »."""

import csv
import os
import sqlite3

DB_PATH = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "lissafi.db")
)

_SCHEMA = """
CREATE TABLE IF NOT EXISTS places (
    id TEXT PRIMARY KEY,
    source TEXT NOT NULL,
    name TEXT NOT NULL,
    category TEXT,
    target_type TEXT,
    address TEXT,
    phone TEXT,
    website TEXT,
    lat REAL,
    lng REAL,
    rating REAL,
    review_count INTEGER,
    opening_hours INTEGER DEFAULT 0,
    business_status TEXT,
    is_physical INTEGER DEFAULT 1,
    is_target INTEGER DEFAULT 0,
    score REAL DEFAULT 0,
    outreach_msg TEXT,
    statut TEXT DEFAULT '',
    scraped_at TEXT DEFAULT (datetime('now')),
    UNIQUE(name, address)
);
"""

_FIELDS = (
    "id", "source", "name", "category", "target_type", "address", "phone",
    "website", "lat", "lng", "rating", "review_count", "opening_hours",
    "business_status", "is_physical", "is_target", "score", "outreach_msg",
    "statut",
)


def connect():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute(_SCHEMA)
    # Migrations légères : colonnes manquantes sur base existante.
    cols = [c[1] for c in conn.execute("PRAGMA table_info(places)").fetchall()]
    if "outreach_msg" not in cols:
        conn.execute("ALTER TABLE places ADD COLUMN outreach_msg TEXT")
    if "statut" not in cols:
        conn.execute("ALTER TABLE places ADD COLUMN statut TEXT DEFAULT ''")
    return conn


def upsert_place(conn, place):
    row = {k: place.get(k) for k in _FIELDS}
    cols = ", ".join(row.keys())
    ph = ", ".join(":" + k for k in row.keys())
    with conn:
        conn.execute(f"INSERT OR IGNORE INTO places ({cols}) VALUES ({ph})", row)


def export_csv(conn, out_path, only_target=True):
    """Export au format de la feuille Google Sheets « leads »."""
    where = "WHERE is_target = 1" if only_target else ""
    rows = conn.execute(
        f"SELECT * FROM places {where} ORDER BY score DESC"
    ).fetchall()
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["nom", "type", "adresse", "telephone", "statut"])
        for r in rows:
            w.writerow([
                r["name"],
                r["target_type"] or r["category"],
                r["address"],
                r["phone"] or "",
                "",
            ])
    return len(rows)
