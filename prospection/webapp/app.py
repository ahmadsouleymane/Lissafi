#!/usr/bin/env python3
"""Interface web Lissafi — gestion de la prospection.

Lancement :
    export DEEPSEEK_API_KEY=...
    cd prospection && python3 webapp/app.py
    → http://localhost:5001
"""

import asyncio
import csv
import io
import os
import sys
import threading

# Ajoute la racine du projet (prospection/) au chemin d'import pour que
# « from scraper import ... » fonctionne, quel que soit le mode de lancement.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Response, flash, redirect, render_template, request, url_for
from flask import Flask

from scraper import db
from scraper.prepare_outreach import generate_message, wa_link

app = Flask(__name__)
app.secret_key = "lissafi-local-dev"

STATUTS = ["", "à prospecter", "envoyé", "rdv", "refusé"]
SCRAPE_STATUS = {"running": False, "last": ""}


def get_conn():
    return db.connect()


def deepseek_key():
    return os.environ.get("DEEPSEEK_API_KEY", "")


@app.route("/")
def index():
    conn = get_conn()

    def count(where="", params=()):
        return conn.execute(
            f"SELECT COUNT(*) c FROM places {where}", params
        ).fetchone()["c"]

    stats = {
        "total": count(),
        "cibles": count("WHERE is_target=1"),
        "telephones": count("WHERE is_target=1 AND phone != ''"),
        "envoyes": count("WHERE statut='envoyé'"),
        "rdv": count("WHERE statut='rdv'"),
        "refuses": count("WHERE statut='refusé'"),
    }
    types = conn.execute(
        "SELECT target_type, COUNT(*) c FROM places WHERE is_target=1 "
        "GROUP BY target_type ORDER BY c DESC"
    ).fetchall()
    top = conn.execute(
        "SELECT * FROM places WHERE is_target=1 AND phone != '' "
        "ORDER BY score DESC LIMIT 8"
    ).fetchall()
    conn.close()
    return render_template(
        "index.html", stats=stats, types=types, top=top, scrape=SCRAPE_STATUS
    )


@app.route("/leads")
def leads():
    conn = get_conn()
    t = request.args.get("type", "")
    s = request.args.get("statut", "")
    q = request.args.get("q", "").strip()
    only_phone = request.args.get("phone", "") == "1"

    sql = "SELECT * FROM places WHERE 1=1"
    params = []
    if t:
        sql += " AND target_type=?"
        params.append(t)
    if s:
        sql += " AND COALESCE(statut,'')=?"
        params.append(s)
    if q:
        sql += " AND (name LIKE ? OR address LIKE ?)"
        params += [f"%{q}%", f"%{q}%"]
    if only_phone:
        sql += " AND phone != ''"
    sql += " ORDER BY score DESC, name"
    rows = conn.execute(sql, params).fetchall()
    types = [r["target_type"] for r in conn.execute(
        "SELECT DISTINCT target_type FROM places WHERE target_type IS NOT NULL "
        "ORDER BY target_type"
    ).fetchall()]
    conn.close()
    return render_template(
        "leads.html", rows=rows, types=types,
        cur_type=t, cur_statut=s, cur_q=q, only_phone=only_phone, statuts=STATUTS,
    )


@app.route("/leads/<lead_id>", methods=["GET", "POST"])
def lead_detail(lead_id):
    conn = get_conn()
    row = conn.execute("SELECT * FROM places WHERE id=?", (lead_id,)).fetchone()
    if row is None:
        conn.close()
        flash("Prospect introuvable", "error")
        return redirect(url_for("leads"))
    if request.method == "POST":
        statut = request.form.get("statut", "")
        msg = request.form.get("message", "").strip()
        with conn:
            conn.execute(
                "UPDATE places SET statut=?, outreach_msg=? WHERE id=?",
                (statut, msg or None, lead_id),
            )
        flash("Enregistré ✓", "ok")
        conn.close()
        return redirect(url_for("lead_detail", lead_id=lead_id))
    link = wa_link(row["phone"], row["outreach_msg"] or "") if row["phone"] else ""
    conn.close()
    return render_template("lead.html", lead=row, link=link, statuts=STATUTS)


@app.route("/leads/<lead_id>/generate", methods=["POST"])
def generate(lead_id):
    key = deepseek_key()
    if not key:
        flash("DEEPSEEK_API_KEY non définie — relance le serveur avec la clé", "error")
        return redirect(url_for("lead_detail", lead_id=lead_id))
    conn = get_conn()
    row = conn.execute("SELECT * FROM places WHERE id=?", (lead_id,)).fetchone()
    if row is None:
        conn.close()
        return redirect(url_for("leads"))
    try:
        msg = generate_message(dict(row), key)
        with conn:
            conn.execute(
                "UPDATE places SET outreach_msg=? WHERE id=?", (msg, lead_id)
            )
        flash("Message généré ✓", "ok")
    except Exception as e:  # noqa: BLE001
        flash(f"Erreur DeepSeek : {e}", "error")
    conn.close()
    return redirect(url_for("lead_detail", lead_id=lead_id))


@app.route("/scrape", methods=["POST"])
def scrape():
    if SCRAPE_STATUS["running"]:
        flash("Un scrape est déjà en cours — patiente", "error")
        return redirect(url_for("index"))
    query = request.form.get("query", "").strip()
    try:
        maxr = int(request.form.get("max", "25"))
    except ValueError:
        maxr = 25
    if not query:
        flash("Requête vide", "error")
        return redirect(url_for("index"))

    def run():
        SCRAPE_STATUS["running"] = True
        try:
            from scraper import maps_scraper
            conn = db.connect()
            n = asyncio.run(maps_scraper.scrape(query, "Niamey", maxr, conn))
            conn.close()
            SCRAPE_STATUS["last"] = f"{query}: {n} places"
        except Exception as e:  # noqa: BLE001
            SCRAPE_STATUS["last"] = f"{query}: ERREUR {e}"
        finally:
            SCRAPE_STATUS["running"] = False

    threading.Thread(target=run, daemon=True).start()
    flash("Scrape lancé en arrière-plan — rafraîchis dans ~2 min", "ok")
    return redirect(url_for("index"))


@app.route("/export")
def export():
    conn = get_conn()
    rows = conn.execute(
        "SELECT * FROM places WHERE is_target=1 ORDER BY score DESC"
    ).fetchall()
    conn.close()
    buf = io.StringIO()
    w = csv.writer(buf)
    w.writerow(["nom", "type", "adresse", "telephone", "statut"])
    for r in rows:
        w.writerow([
            r["name"], r["target_type"] or r["category"], r["address"],
            r["phone"] or "", r["statut"] or "",
        ])
    return Response(
        buf.getvalue(),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=leads_lissafi.csv"},
    )


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5001, debug=False)
