#!/usr/bin/env python3
"""Génère les messages pour les NOUVEAUX prospects, en continu.

Toutes les N minutes, cherche les prospects cibles avec téléphone qui n'ont pas
encore de message (outreach_msg vide), génère le message DeepSeek, et met à
jour outreach_ready.csv. Les nouveaux prospects ajoutés par le scraper sont
donc pris en charge automatiquement.

Usage :
    python3 -m scraper.message_loop --interval 300 -o outreach_ready.csv
"""

import argparse
import csv
import os
import time

from . import db, prepare_outreach

_CSV_COLS = ["nom", "type", "adresse", "telephone", "message", "lien"]


def _generate_missing(conn):
    key = os.environ.get("DEEPSEEK_API_KEY", "")
    if not key:
        return 0
    rows = conn.execute(
        "SELECT * FROM places WHERE is_target=1 AND phone != '' "
        "AND (outreach_msg IS NULL OR outreach_msg = '') "
        "ORDER BY score DESC"
    ).fetchall()
    if not rows:
        return 0
    for r in rows:
        try:
            msg = prepare_outreach.generate_message(dict(r), key)
            with conn:
                conn.execute(
                    "UPDATE places SET outreach_msg=? WHERE id=?", (msg, r["id"])
                )
            print(f"  + message : {r['name']}", flush=True)
            time.sleep(0.5)
        except Exception as e:  # noqa: BLE001
            print(f"  ! erreur {r['name']}: {e}", flush=True)
    return len(rows)


def _write_csv(conn, out):
    rows = conn.execute(
        "SELECT * FROM places WHERE is_target=1 AND phone != '' "
        "AND outreach_msg IS NOT NULL AND outreach_msg != '' "
        "ORDER BY score DESC"
    ).fetchall()
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=_CSV_COLS)
        w.writeheader()
        for r in rows:
            w.writerow({
                "nom": r["name"],
                "type": r["target_type"] or r["category"],
                "adresse": r["address"],
                "telephone": r["phone"],
                "message": r["outreach_msg"],
                "lien": prepare_outreach.wa_link(r["phone"], r["outreach_msg"]),
            })
    return len(rows)


def run(interval: int, out: str) -> None:
    conn = db.connect()
    while True:
        n = _generate_missing(conn)
        total = _write_csv(conn, out)
        print(f"[tick] {n} nouveaux messages · {total} dans {out}", flush=True)
        time.sleep(interval)


def main() -> None:
    ap = argparse.ArgumentParser(description="Génère les messages des nouveaux prospects")
    ap.add_argument("--interval", type=int, default=300,
                    help="secondes entre deux passages (défaut 300)")
    ap.add_argument("-o", "--out", default="outreach_ready.csv")
    args = ap.parse_args()
    try:
        run(args.interval, args.out)
    except KeyboardInterrupt:
        print("\nArrêt du générateur continu.")


if __name__ == "__main__":
    main()
