# heroworld-horror — mod client (Failles de l'Hadès, phase 1)

Mod Fabric 1.20.6 (même stack que `../mod`) : lampe torche côté client.
- **Touche R** (configurable) : allumer/éteindre quand la lampe ItemsAdder est en main → envoie `/hwtorch on|off` au serveur.
- **HUD** discret en bas à gauche (chip touche + état), uniquement lampe en main.
- **Particules** de poussière dans le faisceau, son de clic local.

La vraie lumière dans le monde est posée par le plugin serveur `HeroesHorror` (blocs LIGHT suivant le regard).

## Build
Comme le mod titlescreen : depuis ce dossier, `gradle build` (JDK 21). Jar : `build/libs/heroworld-horror-0.1.0.jar`.

## Test local rapide
1. Copier le jar dans `.minecraft/mods` du profil Fabric 1.20.6 (avec le jar titlescreen).
2. Serveur : plugin `HeroesHorror` installé + item ItemsAdder `heroesworld:lampe_torche` (`/ia` → onglet heroesworld, ou `/iaget lampe_torche`).
3. `/hwtorch debug` doit afficher `PRISMARINE_SHARD | CMD: 10000 | reconnu lampe: true`. Si le CMD diffère, aligner : `plugins/HeroesHorror/config.yml` + `.minecraft/config/heroworld-horror.json`.
4. Lampe en main : le HUD apparaît ; **R** → clic sonore, poussière dans le faisceau, et le noir se perce là où on regarde.

## Intégration launcher
Étape 2 du launcher v2 : copier CE jar dans `mods/` en plus de `heroworld-titlescreen` (même mécanisme).

## Config client
`.minecraft/config/heroworld-horror.json` : `torchKey` (code GLFW, 82=R), `hud`, `particles`, `torchMaterial`, `torchCustomModelData`.
