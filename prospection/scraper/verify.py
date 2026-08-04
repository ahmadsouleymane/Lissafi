"""Vérification « business physique » + classification par type cible."""

import re
import unicodedata

from .config import NIAMEY_BBOX, TARGET_TYPES, VIRTUAL_SIGNALS


def _norm(s):
    s = unicodedata.normalize("NFD", s or "")
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.lower().strip()


def _txt(v):
    if isinstance(v, (list, tuple)):
        return " ".join(str(x) for x in v)
    return str(v or "")


def normalize_phone(raw):
    """Met un numéro au format international +227 pour le Niger."""
    digits = re.sub(r"\D", "", _txt(raw))
    if not digits:
        return ""
    if len(digits) == 8:  # numéro local nigérien
        return "+227" + digits
    if digits.startswith("227") and len(digits) == 11:
        return "+" + digits
    if digits.startswith("00227"):
        return "+" + digits[2:]
    return digits


def looks_targetish(row):
    """Vérification rapide (sans réseau) : nom/catégorie = type cible ?"""
    text = _norm(f"{_txt(row.get('name'))} {_txt(row.get('category'))}")
    return any(kw in text for kw in TARGET_TYPES)


def classify_place(row):
    """Classe une place : physique ? type cible ? score.

    Règles :
      - physique : adresse réelle + coordonnées GPS + pas de signe virtuel
                   + pas fermé définitivement
      - cible    : physique + catégorie cible + dans la boîte de Niamey
    """
    name = _norm(_txt(row.get("name")))
    category = _norm(_txt(row.get("category")))
    address = _norm(_txt(row.get("address")))
    text = f"{name} {category}"

    status = _norm(_txt(row.get("business_status")))
    permanently_closed = bool(
        row.get("permanently_closed") or "permanently_closed" in status
    )
    virtual = any(sig in text for sig in VIRTUAL_SIGNALS)

    target_type = next(
        (t for kw, t in TARGET_TYPES.items() if kw in text), None
    )

    has_address = len(address) > 8  # « niamey » seul = trop court
    has_coords = row.get("lat") is not None and row.get("lng") is not None
    in_bbox = has_coords and (
        NIAMEY_BBOX["lat_min"] <= row["lat"] <= NIAMEY_BBOX["lat_max"]
        and NIAMEY_BBOX["lng_min"] <= row["lng"] <= NIAMEY_BBOX["lng_max"]
    )

    is_physical = (
        has_address and has_coords and not virtual and not permanently_closed
    )
    is_target = is_physical and target_type is not None and in_bbox

    score = 0.0
    if target_type:
        score += 2
    if row.get("phone"):
        score += 2
    if row.get("opening_hours"):
        score += 1
    if has_address:
        score += 1
    if (row.get("review_count") or 0) > 0:
        score += 1
    if virtual:
        score -= 3
    if permanently_closed:
        score -= 5
    score = max(0.0, min(score, 10.0))

    return {
        "target_type": target_type,
        "is_physical": int(is_physical),
        "is_target": int(is_target),
        "score": round(score, 1),
        "virtual": int(virtual),
        "permanently_closed": int(permanently_closed),
    }
