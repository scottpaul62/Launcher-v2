#!/usr/bin/env python3
"""Importe les PNG de ui-drop/ vers les assets du mod (noms canoniques + optimisation + mesures)."""
import os, sys, re
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(os.path.dirname(ROOT), "ui-drop")
DST = os.path.join(ROOT, "mod/src/main/resources/assets/heroworld/textures/gui/ui/hw")

RULES = [  # (regex sur le nom, nom canonique, hauteur max)
    (r"primary|principal",              "btn_primary",   192),
    (r"secondary|secondaire",           "btn_secondary", 192),
    (r"menu-?panel|panel|cadre",        "menu_frame",    1024),
    (r"compact|profile-compact|profil", "chip",          160),
    (r"dropdown|deroul",                "dropdown",      1024),
    (r"status|statut",                  "status",        160),
    (r"dock|toolbar",                   "dock",          512),
    (r"tooltip|aide",                   "tooltip",       160),
]

def canon(name):
    n = name.lower()
    for rx, out, mh in RULES:
        if re.search(rx, n):
            return out, mh
    return None, None

def main():
    os.makedirs(DST, exist_ok=True)
    done = {}
    for f in sorted(os.listdir(SRC)):
        if not f.lower().endswith(".png"): continue
        out, mh = canon(f)
        if not out or out in done:
            print("ignore :", f); continue
        im = Image.open(os.path.join(SRC, f)).convert("RGBA")
        if im.height > mh:
            im = im.resize((round(im.width * mh / im.height), mh), Image.LANCZOS)
        im.save(os.path.join(DST, out + ".png"), optimize=True)
        done[out] = im.size
        print(f"{f} -> {out}.png {im.size}")
    # le marbre du dropdown sert aussi de fond de panneau generique
    if "dropdown" in done:
        Image.open(os.path.join(DST, "dropdown.png")).save(os.path.join(DST, "panel_marble.png"), optimize=True)
        print("dropdown -> panel_marble.png (fond des grands panneaux)")
    # mesure des logements du dock (colonnes de pixels dores)
    if "dock" in done:
        im = Image.open(os.path.join(DST, "dock.png")).convert("RGBA")
        w, h = im.size; px = im.load()
        cols = []
        for x in range(w):
            g = 0
            for y in range(int(h*0.25), int(h*0.95), 2):
                r, gg, b, a = px[x, y]
                if a > 100 and r > 120 and gg > 80 and b < 110 and r > b + 40: g += 1
            cols.append(g)
        # clusters de colonnes dorees -> bords des logements
        thr = max(cols) * 0.35 if max(cols) else 1
        runs, cur = [], None
        for x, v in enumerate(cols):
            if v >= thr and cur is None: cur = x
            elif v < thr and cur is not None: runs.append((cur, x)); cur = None
        if cur is not None: runs.append((cur, w))
        centers = []
        # bords des slots par paires internes (ignore le cadre exterieur = 1er/dernier run)
        inner = runs[1:-1] if len(runs) >= 12 else runs
        for i in range(0, len(inner) - 1, 2):
            centers.append(((inner[i][1] + inner[i+1][0]) / 2) / w)
        print("fractions centres logements :", [round(c, 3) for c in centers])
    missing = [r[1] for r in RULES if r[1] not in done]
    if missing: print("MANQUANTS :", missing)

if __name__ == "__main__":
    main()
