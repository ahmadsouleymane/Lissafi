"""Charge le fichier .env à la racine du projet (prospection/.env)."""

import os


def load_env():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    env_path = os.path.join(root, ".env")
    if not os.path.exists(env_path):
        return
    with open(env_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            # Le .env du projet a priorité sur une éventuelle variable de shell.
            os.environ[k.strip()] = v.strip()
