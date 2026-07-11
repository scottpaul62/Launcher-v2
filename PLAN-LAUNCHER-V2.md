# Launcher HEROES-WORLD v2 — Electron + Vue 3 + XMCL

Décision (2026-07-11) : migrer l'INTERFACE vers une vraie base moderne (Vue 3), pour un rendu
propre, responsive et sans les bugs du patching sur Helios. Le launcher Helios actuel reste la
version de secours JOUABLE tant que la v2 n'a pas rattrapé (login + téléchargement + lancement).

## Stack
- **Electron 29** (fenêtre desktop) + **electron-vite 5** (build/dev) + **Vue 3.5** (interface).
- **XMCL Core / installer / user** (`@xmcl/core`, `@xmcl/installer`, `@xmcl/user`) : moteur Minecraft
  officiel (auth Microsoft, install version + Fabric, lancement) — évite de recoder les parties
  dangereuses. À câbler à l'étape 2.

## Structure
```
Launcher-v2/
  package.json              deps + scripts (dev/build/dist)
  electron.vite.config.mjs  config electron-vite (+ plugin Vue)
  src/
    main/index.js           process principal Electron (fenêtre, IPC, XMCL à venir)
    preload/index.js        pont sécurisé (window.hw)
    renderer/
      index.html
      src/
        main.js             bootstrap Vue
        App.vue             INTERFACE complète (accueil + pages + dock + compte, RESPONSIVE)
        style.css           variables + reset
        assets/             logo.png (logo détouré de Scott), planet.png, seal.png
```

## Ce qui est FAIT (fondation, étape 1)
- Interface Vue responsive : barre de titre custom, décor (planète + espace + voile), statut serveur,
  compte (tête qui suit le curseur + pseudo coloré), logo, LANCER, dock 6 pages, widgets accueil,
  pages Actus (pagination) / Mods / Boutique / Amis / Aide, molette + Échap, toast.
- **Une seule page active par construction** (Vue `v-if`) → plus de superpositions possibles.
- **Responsive natif** (clamp/vw/vmin) → s'adapte à toutes les tailles, plus de bugs de redimensionnement.
- LANCER = honnête : affiche « câblage XMCL en cours » (PAS un faux lancement).

## Étape 2 — à câbler (prochaines sessions)
1. **Auth Microsoft** via `@xmcl/user` (device code ou OAuth) → récupérer profil + UUID (→ vraie tête).
2. **Install + launch** via `@xmcl/core` + `@xmcl/installer` :
   - installer Minecraft 1.20.6 + Fabric loader 0.19.3 (comme la distro actuelle),
   - copier le mod `heroworld-titlescreen` dans mods/,
   - lancer avec les bons args (RAM, Java 21), SANS auto-connexion (menu custom).
   - IPC `mc:launch` dans main/index.js ↔ bouton LANCER.
3. **Réglages** (page settings) : RAM slider, résolution, dossier, Java — persistés (JSON).
4. **Mode édition** (déplacer/redimensionner) : reporter depuis la v1 (localStorage, en %).
5. **Statut serveur réel** : ping 127.0.0.1:25565 (ou play.heroesworld.fr) → pastille.
6. **Build .exe** : `npm run dist` (electron-builder NSIS).

## Réutilisé de la v1 (rien de perdu)
- Logo détouré (banner-v25 → logo.png), planète de Scott, sceau.
- distribution.json / mod Fabric / config serveur : réutilisables tels quels par XMCL.
- Toute la DA (couleurs, thème Olympe) et l'expérience acquise.

## Lancer (Scott)
1. `Installer-Launcher-v2.bat` (une fois — installe Node deps + Electron).
2. `Lancer-Launcher-v2-DEV.bat` → la nouvelle interface s'ouvre (hot-reload en dev).
```
```
