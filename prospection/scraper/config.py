"""Paramètres du scrapeur Lissafi."""

# Mots-clés → type normalisé. Comparés en minuscules et SANS accents.
TARGET_TYPES = {
    "epicerie": "épicerie",
    "grocery": "épicerie",
    "supermarche": "supermarché",
    "supermarket": "supermarché",
    "magasin": "magasin",
    "shop": "magasin",
    "boutique": "boutique",
    "restaurant": "restaurant",
    "resto": "restaurant",
    "boulangerie": "boulangerie",
    "bakery": "boulangerie",
    "patisserie": "pâtisserie",
    "quincaillerie": "quincaillerie",
    "hardware": "quincaillerie",
    "pharmacie": "pharmacie",
    "pharmacy": "pharmacie",
    "salon de coiffure": "salon de coiffure",
    "coiffure": "salon de coiffure",
    "barbershop": "salon de coiffure",
    "tissus": "vente de tissus",
    "tissu": "vente de tissus",
    "fabrics": "vente de tissus",
    "bijouterie": "bijouterie",
    "librairie": "librairie",
    "libraire": "librairie",
    "vetement": "prêt-à-porter",
    "vêtement": "prêt-à-porter",
    "clothing": "prêt-à-porter",
    "electromenager": "électroménager",
    "appliance": "électroménager",
    "telephonie": "téléphonie",
    "telephone": "téléphonie",
    "cyber cafe": "cyber café",
    "internet cafe": "cyber café",
    "marche": "marché",
    "market": "marché",
    "station service": "station-service",
    "station de service": "station-service",
    "essence": "station-service",
}

# Signaux indiquant une structure NON physique (à exclure de la cible).
VIRTUAL_SIGNALS = (
    "en ligne",
    "online",
    "e-shop",
    "boutique en ligne",
    "livraison uniquement",
    "delivery only",
    "dropshipping",
    "plateforme",
)

# Boîte englobante de Niamey (approx.) — restreint aux coordonnées de la ville.
NIAMEY_BBOX = {
    "lat_min": 13.44,
    "lat_max": 13.63,
    "lng_min": 2.01,
    "lng_max": 2.20,
}
