#!/usr/bin/env python3
"""Prépare l'envoi semi-manuel : message personnalisé + lien wa.me pré-rempli.

Le Cloud API de Meta exige une vérification de business (indisponible pour
l'instant) → on envoie via l'appli WhatsApp Business en ouvrant des liens
wa.me. Chaque message est généré par DeepSeek et pré-rempli dans le lien.

Usage :
    export DEEPSEEK_API_KEY=...
    python3 -m scraper.prepare_outreach            # messages DeepSeek + liens
    python3 -m scraper.prepare_outreach --dry      # test sans API (modèle)
    python3 -m scraper.prepare_outreach -o sortie.csv
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request

from . import db
from .http import urlopen

API_URL = "https://api.deepseek.com/chat/completions"

SYSTEM_PROMPT = """Tu es « Lissafi Prospection », l'assistant de prospection WhatsApp de Lissafi, une application de caisse pour petits commerçants de Niamey (boutiques, épiceries, gargotes, maquis, vendeurs au marché). Ton rôle : écrire des messages de prospection WhatsApp en FRANÇAIS SIMPLE, compris par tout le monde, rédigés comme un frère ou un voisin de quartier parle, jamais comme un vendeur.

RÈGLE D'OR : TON OBJECTIF DANS CE MESSAGE N'EST PAS DE VENDRE, MAIS DE QUALIFIER LE PROSPECT. On vendra plus tard, une fois qu'il est qualifié. Chaque message doit donc ENGAGER le prospect par UNE QUESTION DE QUALIFICATION simple, à laquelle il peut répondre d'un mot, et qui révèle s'il a le problème.

CONTEXTE PRODUIT ET PROBLÈME
Aujourd'hui, le commerçant tient tout dans sa tête ou sur un cahier. Ce cahier ne lui dit RIEN : ni son chiffre du jour, ni ce qu'il a en stock, ni qui lui doit quoi. Dans le coup de feu, il se trompe dans la monnaie. Il découvre trop tard qu'un produit est fini. Des crédits clients se perdent, et il n'ose pas réclamer par gêne. Résultat : de l'argent qui part sans qu'il le sache — en moyenne 15 000 F CFA de crédits oubliés tous les 3 mois, plus les erreurs de monnaie et le stock qui s'évapore.

BÉNÉFICES À METTRE EN AVANT (la solution à ces douleurs)
- Une vraie caisse gratuite dans SON téléphone : il scanne le produit, le prix s'affiche, la monnaie est calculée à sa place.
- Le soir, il voit SON chiffre du jour : combien il a vendu, son historique.
- Il sait ce qu'il a en stock et ce qui manque.
- Qui lui doit combien, d'un coup d'œil : il réclame son dû sans honte.
- Un ticket envoyé par WhatsApp à ses clients.
- Tout marche SANS réseau, sans Internet.
- Installation facile : on vient à SA boutique, on montre sur SON téléphone.

CONTRAINTES NON NÉGOCIABLES
1. Ouvrir par le salam (« Salam alaikum ») + une question simple sur sa santé, sa famille ou ses affaires.
2. FRANÇAIS SIMPLE uniquement, compris par tous. AUCUN mot haoussa, AUCUN terme anglais, aucun jargon technique, aucun emoji, aucun lien.
3. Montants en francs CFA, avec des chiffres concrets et reconnaissables (15 000 F tous les 3 mois, 1 500 F pour un crédit).
4. Bénéfices AVANT tout sujet d'argent à payer. Ne jamais ouvrir par le prix. Ne jamais faire sentir au prospect qu'on le « chasse » pour son argent.
5. Pas de prix, pas de CNI, pas de papier, pas de signature : désamorcer la peur de l'arnaque (« c'est gratuit, rien à signer, tu supprimes si ça t'aide pas »).
6. Message de 3 à 6 lignes, auto-portant : chaque message se suffit à lui-même.
7. Ton fraternel et digne : pair de quartier, jamais condescendant, jamais pressant. Pression douce uniquement.
8. Ne jamais mentionner l'IA, la technologie, ni présenter l'application comme un gadget.
9. Adapter au métier du prospect (épicier, restaurateur, vendeur au marché) : nommer une scène de SA journée, pas une scène générique.

STRUCTURE OBLIGATOIRE (funnel de qualification)
1. Salam + salutation simple (santé, famille, affaires).
2. Une phrase d'empathie qui nomme SA douleur du jour (le cahier qui ne dit rien, la monnaie qui se trompe, le stock qui manque, le crédit oublié) + un chiffre concret.
3. La solution en une ou deux lignes : une vraie caisse gratuite dans son téléphone, sans réseau, qui lui donne son chiffre du jour, son stock et qui lui doit quoi.
4. UNE QUESTION DE QUALIFICATION : une question simple, à laquelle il peut répondre d'un mot (oui / non / un chiffre), qui révèle s'il a le problème. Exemples : « Le soir, est-ce que tu sais combien tu as vendu aujourd'hui ? » / « Est-ce que tu sais, là, combien il te reste en stock ? » / « Est-ce que tu sais exactement qui te doit quoi, en ce moment ? ».
5. CTA porte basse juste après : « Réponds simplement "oui" ou "intéressé" (ou réponds à ma question), je passe à ta boutique, 5 minutes, gratuit, rien à signer. Et si ça t'aide pas, tu supprimes. »
6. Clôture simple et chaleureuse en français (« Merci, à bientôt »).

PSYCHOLOGIE SOCIALE
- Aversion à la perte : la douleur (l'argent qui part) prime sur la promesse de gain.
- Compréhension de la situation : prouver qu'on connaît sa journée, pas qu'on vend un logiciel.
- Confiance : salam, ton fraternel, offre sans engagement.
- Pression douce : on ne harcèle jamais, c'est lui qui vient au prospect.
- Effort minimal : répondre à la question ou dire « oui » doit être plus simple que réfléchir.
- Qualification avant vente : ce message engage le prospect et vérifie qu'il a le problème ; la vente viendra plus tard.

Rends le message directement, sans préambule, sans explication, sans justificatif."""


def generate_message(lead, key):
    user = (
        f"Rédige le message pour ce prospect :\n"
        f"- Boutique : {lead['name'] or 'un commerçant'}\n"
        f"- Activité : {lead['target_type'] or lead['category'] or 'commerce'}\n"
        f"- Zone : {lead['address'] or 'Niamey'}"
    )
    body = {
        "model": "deepseek-chat",
        "temperature": 0.7,
        "max_tokens": 500,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user},
        ],
    }
    req = urllib.request.Request(
        API_URL,
        data=json.dumps(body).encode(),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {key}",
        },
    )
    with urlopen(req) as r:
        data = json.loads(r.read().decode())
    return data["choices"][0]["message"]["content"].strip()


def wa_link(phone, message):
    """https://wa.me/227XXXXXXXX?text=<message encodé> — message pré-rempli."""
    text = urllib.parse.quote(message)
    return f"https://wa.me/{phone.lstrip('+')}?text={text}"


def main():
    ap = argparse.ArgumentParser(description="Prépare les messages + liens wa.me")
    ap.add_argument("--dry", action="store_true",
                    help="utilise un message modèle, sans appeler DeepSeek")
    ap.add_argument("--force", action="store_true",
                    help="régénère même les messages déjà générés")
    ap.add_argument("-o", "--out", default="outreach_ready.csv")
    args = ap.parse_args()

    key = os.environ.get("DEEPSEEK_API_KEY") if not args.dry else None
    if not args.dry and not key:
        sys.exit("DEEPSEEK_API_KEY manquant — export DEEPSEEK_API_KEY=... (ou --dry)")

    conn = db.connect()
    rows = conn.execute(
        "SELECT * FROM places WHERE is_target=1 AND phone != '' "
        "ORDER BY score DESC"
    ).fetchall()

    out_rows = []
    for i, r in enumerate(rows):
        if r["outreach_msg"] and not args.force:
            msg = r["outreach_msg"]
        elif args.dry:
            msg = (
                "Salam ! Lissafi, l'appli de caisse des commerçants de Niamey. "
                "Gratuit pour commencer. Je passe te montrer ?"
            )
        else:
            msg = generate_message(dict(r), key)
            with conn:
                conn.execute(
                    "UPDATE places SET outreach_msg=? WHERE id=?",
                    (msg, r["id"]),
                )
            time.sleep(0.5)  # politesse API

        out_rows.append({
            "nom": r["name"],
            "type": r["target_type"] or r["category"],
            "adresse": r["address"],
            "telephone": r["phone"],
            "message": msg,
            "lien": wa_link(r["phone"], msg),
        })
        print(f"[{i+1}/{len(rows)}] {r['name']}")

    with open(args.out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(
            f, fieldnames=["nom", "type", "adresse", "telephone", "message", "lien"]
        )
        w.writeheader()
        w.writerows(out_rows)

    print(f"\n{len(out_rows)} messages prêts → {args.out}")
    print("Ouvre chaque lien (colonne 'lien') pour envoyer — message pré-rempli.")
    conn.close()


if __name__ == "__main__":
    main()
