"""Requêtes HTTPS avec certificats certifi — corrige le SSL du Python macOS.

Le Python framework d'Apple n'a pas toujours les certificats racine → sur les
appels HTTPS (api.deepseek.com, maps.googleapis.com…), on pointe le contexte
SSL vers certifi.
"""

import ssl
import urllib.request

try:
    import certifi
except ImportError:  # certifi absent : on retombe sur le contexte par défaut
    certifi = None

_context = None


def get_ssl_context():
    global _context
    if _context is None:
        if certifi is not None:
            _context = ssl.create_default_context(cafile=certifi.where())
        else:
            _context = ssl.create_default_context()
    return _context


def urlopen(req, timeout=30):
    return urllib.request.urlopen(req, timeout=timeout, context=get_ssl_context())
