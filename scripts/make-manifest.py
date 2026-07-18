#!/usr/bin/env python3
"""Genere data/manifest.json : liste {path, sha1, size} de tous les fichiers de data/ (sauf manifest)."""
import hashlib, json, os, sys

root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")
files = []
for base, _, names in os.walk(root):
    for n in sorted(names):
        rel_dir = os.path.relpath(base, root).replace(os.sep, "/")
        if rel_dir.startswith("pack"):  # pack serveur vanilla : distribue par server.properties, pas par le mod
            continue
        if n == "manifest.json" or n.endswith(".tmp"):
            continue
        p = os.path.join(base, n)
        rel = os.path.relpath(p, root).replace(os.sep, "/")
        h = hashlib.sha1()
        with open(p, "rb") as f:
            for chunk in iter(lambda: f.read(65536), b""):
                h.update(chunk)
        files.append({"path": rel, "sha1": h.hexdigest(), "size": os.path.getsize(p)})
out = {"version": 1, "files": files}
json.dump(out, open(os.path.join(root, "manifest.json"), "w", encoding="utf-8"), indent=1)
print(f"manifest.json : {len(files)} fichiers")
