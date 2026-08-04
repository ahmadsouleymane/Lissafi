"""Auto-test hors réseau : filtrage, normalisation, DB, export.

Usage : python3 -m scraper.self_test
"""

from . import db, verify


def test_verify():
    cases = [
        # (dict place, attendu is_target)
        (
            {"name": "Épicerie Moussa", "category": "épicerie",
             "address": "Avenue de la Liberté, Niamey", "phone": "96123456",
             "lat": 13.51, "lng": 2.11, "opening_hours": 1, "review_count": 3},
            True,
        ),
        (
            {"name": "Boutique en ligne Kado", "category": "e-shop",
             "address": "Niamey", "phone": "96123457", "lat": 13.51, "lng": 2.11},
            False,
        ),
        (
            {"name": "Pharmacie Centrale", "category": "pharmacy",
             "address": "Rue du Marché, Niamey", "phone": "96123458",
             "lat": 13.51, "lng": 2.11, "business_status": "permanently_closed"},
            False,
        ),
        (
            {"name": "Restaurant Le Gawlo", "category": "restaurant",
             "address": "Boulevard Mali Béro, Niamey", "phone": "90123456",
             "lat": 13.53, "lng": 2.11, "opening_hours": 1},
            True,
        ),
        (
            {"name": "Boutique hors Niamey", "category": "boutique",
             "address": "Route de Tillabéri, Niger", "phone": "96123459",
             "lat": 14.20, "lng": 1.45},
            False,
        ),
    ]
    for row, expected in cases:
        c = verify.classify_place(row)
        got = bool(c["is_target"])
        status = "OK" if got == expected else "ECHEC"
        print(
            f"[{status}] {row['name']:<30} target={int(got)} "
            f"type={c['target_type']} score={c['score']}"
        )
        assert got == expected, f"{row['name']}: attendu {expected}, obtenu {got}"

    assert verify.normalize_phone("96 12 34 56") == "+22796123456"
    assert verify.normalize_phone("+227 90 12 34 56") == "+22790123456"
    print("[OK] normalize_phone")


def test_db():
    conn = db.connect()
    row = {
        "id": "test_epicerie", "source": "test", "name": "Épicerie Moussa",
        "category": "épicerie", "target_type": "épicerie",
        "address": "Avenue de la Liberté, Niamey", "phone": "+22796123456",
        "lat": 13.51, "lng": 2.11, "opening_hours": 1, "is_target": 1, "score": 7.0,
    }
    db.upsert_place(conn, row)
    n = db.export_csv(conn, "lissafi_test_leads.csv")
    conn.execute("DELETE FROM places WHERE id='test_epicerie'")
    conn.commit()
    conn.close()
    import os
    os.remove("lissafi_test_leads.csv")
    print(f"[OK] DB + export CSV ({n} lignes écrites puis nettoyées)")


if __name__ == "__main__":
    test_verify()
    test_db()
    print("\nTous les tests passent.")
