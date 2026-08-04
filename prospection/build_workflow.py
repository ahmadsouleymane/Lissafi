#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Génère le workflow n8n « Lissafi — Prospection WhatsApp ».

Usage :
    python3 build_workflow.py
Produit : lissafi-prospection-workflow.json (importable dans n8n).
"""

import json
import pathlib

OUT = pathlib.Path(__file__).parent / "lissafi-prospection-workflow.json"


def node(nid, name, ntype, tver, pos, params, creds=None):
    n = {
        "parameters": params,
        "id": nid,
        "name": name,
        "type": ntype,
        "typeVersion": tver,
        "position": pos,
    }
    if creds:
        n["credentials"] = creds
    return n


nodes = []
connections = {}


def add(name, ntype, tver, params, creds=None):
    nid = "00000000-0000-4000-8000-{:012d}".format(len(nodes) + 1)
    pos = [140 + 260 * len(nodes), 0]
    nodes.append(node(nid, name, ntype, tver, pos, params, creds))
    return name


def link(src, dst, output_index=0):
    connections.setdefault(src, {"main": []})
    while len(connections[src]["main"]) <= output_index:
        connections[src]["main"].append([])
    connections[src]["main"][output_index].append(
        {"node": dst, "type": "main", "index": 0}
    )


# ---------------------------------------------------------------------------
# Nœuds
# ---------------------------------------------------------------------------

add("Déclencheur manuel", "n8n-nodes-base.manualTrigger", 1, {})

add(
    "Leads (Google Sheets)",
    "n8n-nodes-base.googleSheets",
    4,
    {
        "operation": "read",
        "documentId": {
            "__rl": True,
            "mode": "id",
            "value": "={{ $env.LISSAFI_SHEET_ID }}",
        },
        "sheetName": {"__rl": True, "mode": "name", "value": "leads"},
        "options": {},
    },
    {"googleSheetsOAuth2Api": {"id": "GOOGLE-CRED-ID", "name": "Google Sheets (Lissafi)"}},
)

add(
    "Filtrer nouveaux",
    "n8n-nodes-base.code",
    2,
    {
        "jsCode": (
            "// Garde uniquement les prospects pas encore envoyés\n"
            "const NOUVEAU = new Set(['', 'nouveau', 'à traiter']);\n"
            "return items.filter((it) => {\n"
            "  const s = String(it.json.statut || '').trim().toLowerCase();\n"
            "  return NOUVEAU.has(s);\n"
            "});"
        )
    },
)

add("Par lots", "n8n-nodes-base.splitInBatches", 3, {"batchSize": 3, "options": {}})

add(
    "Pause entre lots",
    "n8n-nodes-base.wait",
    2,
    {"resume": "timeInterval", "amount": 45, "unit": "seconds"},
)

add(
    "Vérif WhatsApp",
    "n8n-nodes-base.httpRequest",
    4.2,
    {
        "method": "POST",
        "url": "https://graph.facebook.com/v21.0/{{ $env.WA_PHONE_NUMBER_ID }}/contacts",
        "authentication": "none",
        "sendHeaders": True,
        "headerParameters": {
            "parameters": [
                {"name": "Authorization", "value": "=Bearer {{ $env.WA_ACCESS_TOKEN }}"}
            ]
        },
        "sendBody": True,
        "specifyBody": "json",
        "jsonBody": "={{ JSON.stringify({ contacts: [$json.telephone] }) }}",
        "options": {},
    },
)

add(
    "Garde WhatsApp valides",
    "n8n-nodes-base.code",
    2,
    {
        "jsCode": (
            "// Ne garde que les numéros 'valid' et recolle le contexte du prospect\n"
            "const leads = $('Par lots').all();\n"
            "const out = [];\n"
            "items.forEach((it, i) => {\n"
            "  const c = it.json.contacts && it.json.contacts[0];\n"
            "  if (!c || c.status !== 'valid') return;\n"
            "  const lead = (leads[i] && leads[i].json) || {};\n"
            "  out.push({ json: { ...lead, waId: c.wa_id } });\n"
            "});\n"
            "return out;"
        )
    },
)

add(
    "Prompt DeepSeek",
    "n8n-nodes-base.code",
    2,
    {
        "jsCode": (
            "const item = items[0].json;\n"
            "const system = `Tu es Lissafi, assistant de prospection WhatsApp d'une application de caisse "
            "pour petits commerçants de Niamey, au Niger.\n\n"
            "Consignes :\n"
            "- Écris en français simple, ton chaleureux et respectueux.\n"
            "- Maximum 180 caractères pour le message.\n"
            "- Structure : salutation, une accroche sur le vrai souci du commerçant "
            "(crédits oubliés, caisse tenue au cahier), une proposition concrète gratuite.\n"
            "- Termine impérativement par : « Je passe te montrer ? »\n"
            "- Interdits : lien, prix, mention d'IA ou de robot, rafale d'émojis (un au plus), "
            "arguments trop marketing.`;\n"
            "const user = `Rédige le message pour ce prospect :\n"
            "- Boutique : ${item.nom || 'un commerçant'}\n"
            "- Activité : ${item.type || 'commerce'}\n"
            "- Zone : ${item.adresse || 'Niamey'}`;\n"
            "const deepseekBody = {\n"
            "  model: 'deepseek-chat',\n"
            "  temperature: 0.7,\n"
            "  max_tokens: 220,\n"
            "  messages: [\n"
            "    { role: 'system', content: system },\n"
            "    { role: 'user', content: user }\n"
            "  ]\n"
            "};\n"
            "return [{ json: { ...item, deepseekBody } }];"
        )
    },
)

add(
    "Générer message",
    "n8n-nodes-base.httpRequest",
    4.2,
    {
        "method": "POST",
        "url": "https://api.deepseek.com/chat/completions",
        "authentication": "none",
        "sendHeaders": True,
        "headerParameters": {
            "parameters": [
                {"name": "Authorization", "value": "=Bearer {{ $env.DEEPSEEK_API_KEY }}"}
            ]
        },
        "sendBody": True,
        "specifyBody": "json",
        "jsonBody": "={{ $json.deepseekBody }}",
        "options": {},
    },
)

add(
    "Extraire message",
    "n8n-nodes-base.code",
    2,
    {
        "jsCode": (
            "// Recolle le contexte du prospect et extrait le texte généré\n"
            "const ctxs = $('Prompt DeepSeek').all();\n"
            "const out = items.map((it, i) => {\n"
            "  const ctx = (ctxs[i] && ctxs[i].json) || {};\n"
            "  const c = it.json.choices && it.json.choices[0];\n"
            "  const message = (c && c.message && c.message.content)\n"
            "    ? c.message.content.trim() : '';\n"
            "  return { json: { ...ctx, message, finishReason: c ? c.finish_reason : null } };\n"
            "});\n"
            "return out;"
        )
    },
)

add(
    "Envoi WhatsApp",
    "n8n-nodes-base.httpRequest",
    4.2,
    {
        "method": "POST",
        "url": "https://graph.facebook.com/v21.0/{{ $env.WA_PHONE_NUMBER_ID }}/messages",
        "authentication": "none",
        "sendHeaders": True,
        "headerParameters": {
            "parameters": [
                {"name": "Authorization", "value": "=Bearer {{ $env.WA_ACCESS_TOKEN }}"}
            ]
        },
        "sendBody": True,
        "specifyBody": "json",
        "jsonBody": (
            "={{ JSON.stringify({ messaging_product: 'whatsapp', recipient_type: 'individual', "
            "to: $json.waId, type: 'template', template: { name: 'prospection_lissafi_v1', "
            "language: { code: 'fr' }, components: [ { type: 'body', parameters: [ "
            "{ type: 'text', text: $json.message } ] } ] } }) }}"
        ),
        "options": {},
    },
)

add(
    "Log (Google Sheets)",
    "n8n-nodes-base.googleSheets",
    4,
    {
        "operation": "append",
        "documentId": {
            "__rl": True,
            "mode": "id",
            "value": "={{ $env.LISSAFI_SHEET_ID }}",
        },
        "sheetName": {"__rl": True, "mode": "name", "value": "log"},
        "columns": {
            "mappingMode": "defineBelow",
            "value": {
                "Date": "={{ $now.toISOString() }}",
                "Téléphone": "={{ $json.waId }}",
                "Boutique": "={{ $json.nom }}",
                "Type": "={{ $json.type }}",
                "Message": "={{ $json.message }}",
                "Statut": "envoyé",
            },
        },
        "options": {},
    },
    {"googleSheetsOAuth2Api": {"id": "GOOGLE-CRED-ID", "name": "Google Sheets (Lissafi)"}},
)

# ---------------------------------------------------------------------------
# Connexions
# ---------------------------------------------------------------------------

link("Déclencheur manuel", "Leads (Google Sheets)")
link("Leads (Google Sheets)", "Filtrer nouveaux")
link("Filtrer nouveaux", "Par lots")
link("Par lots", "Pause entre lots", output_index=0)
# La sortie « done » de splitInBatches reste vide
connections["Par lots"]["main"].append([])
link("Pause entre lots", "Vérif WhatsApp")
link("Vérif WhatsApp", "Garde WhatsApp valides")
link("Garde WhatsApp valides", "Prompt DeepSeek")
link("Prompt DeepSeek", "Générer message")
link("Générer message", "Extraire message")
link("Extraire message", "Envoi WhatsApp")
link("Envoi WhatsApp", "Log (Google Sheets)")

workflow = {
    "name": "Lissafi — Prospection WhatsApp",
    "nodes": nodes,
    "connections": connections,
    "settings": {"executionOrder": "v1"},
    "pinData": {},
    "active": False,
    "meta": {},
}

OUT.write_text(json.dumps(workflow, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print("Écrit :", OUT)
print("Nœuds :", len(nodes))
