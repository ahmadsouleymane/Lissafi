#!/usr/bin/env python3
"""Orchestrateur de scraping Lissafi.

Exemples :
    python3 -m scraper.main places "épicerie" "Niamey" --max 100
    python3 -m scraper.main maps "restaurant" "Niamey" --max 50
    python3 -m scraper.main export -o leads_niamey.csv
    python3 -m scraper.main stats
"""

import argparse
import asyncio

from . import db


def cmd_places(args):
    from . import places_api
    conn = db.connect()
    n = places_api.scrape(args.query, args.location, args.max, conn)
    print(f"-> {n} places traitées (source places_api)")
    conn.close()


def cmd_maps(args):
    from . import maps_scraper
    conn = db.connect()
    n = asyncio.run(maps_scraper.scrape(args.query, args.location, args.max, conn))
    print(f"-> {n} places traitées (source maps)")
    conn.close()


def cmd_export(args):
    conn = db.connect()
    n = db.export_csv(conn, args.out, only_target=not args.all)
    print(f"-> {n} lignes exportées vers {args.out}")
    conn.close()


def cmd_stats(args):
    conn = db.connect()
    total = conn.execute("SELECT COUNT(*) c FROM places").fetchone()["c"]
    target = conn.execute(
        "SELECT COUNT(*) c FROM places WHERE is_target=1"
    ).fetchone()["c"]
    with_phone = conn.execute(
        "SELECT COUNT(*) c FROM places WHERE is_target=1 AND phone != ''"
    ).fetchone()["c"]
    print(f"Total places : {total}")
    print(f"Places cibles (physiques, type filtré, dans Niamey) : {target}")
    print(f"  dont avec téléphone : {with_phone}")
    print()
    for r in conn.execute(
        "SELECT target_type, COUNT(*) c FROM places WHERE is_target=1 "
        "GROUP BY target_type ORDER BY c DESC"
    ):
        print(f"  {r['target_type'] or '?':<20} {r['c']}")
    conn.close()


def main():
    parser = argparse.ArgumentParser(prog="scraper")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_places = sub.add_parser(
        "places", help="Google Places API (nécessite GOOGLE_PLACES_API_KEY)"
    )
    p_places.add_argument("query")
    p_places.add_argument("location", nargs="?", default="Niamey")
    p_places.add_argument("--max", type=int, default=100)
    p_places.set_defaults(fn=cmd_places)

    p_maps = sub.add_parser(
        "maps", help="Google Maps UI via Playwright (gratuit, sans clé)"
    )
    p_maps.add_argument("query")
    p_maps.add_argument("location", nargs="?", default="Niamey")
    p_maps.add_argument("--max", type=int, default=50)
    p_maps.set_defaults(fn=cmd_maps)

    p_exp = sub.add_parser("export", help="Exporter vers CSV (format feuille leads)")
    p_exp.add_argument("-o", "--out", default="leads_niamey.csv")
    p_exp.add_argument("--all", action="store_true", help="exporter aussi les non-cibles")
    p_exp.set_defaults(fn=cmd_export)

    p_stat = sub.add_parser("stats", help="Résumé de la base")
    p_stat.set_defaults(fn=cmd_stats)

    args = parser.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
