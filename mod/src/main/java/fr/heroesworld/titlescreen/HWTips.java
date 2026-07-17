package fr.heroesworld.titlescreen;

/** Astuces et lore HERO WORLD, en rotation sur l'ecran titre. */
public final class HWTips {
    private static final String[] TIPS = {
        "Maj-droite en jeu ouvre le centre HERO WORLD (mods, reglages, waypoints).",
        "Maintiens C pour zoomer — touche et facteur reglables dans MODS > Zoom.",
        "Clique « Editer le HUD » pour deplacer chaque widget a la souris.",
        "Le vestiaire te montre chaque cosmetique sur TON personnage avant de l'equiper.",
        "Cree un waypoint a ta position depuis l'onglet WAYPOINTS du centre.",
        "Chaque profil (PvP, Survie...) garde sa propre disposition de HUD.",
        "Les drachmes se gagnent en jouant — la boutique arrive bientot.",
        "L'Olympe veille sur ceux qui explorent ses cieux...",
    };
    private HWTips() {}
    public static String current() {
        long t = System.currentTimeMillis();
        return TIPS[(int) ((t / 8000L) % TIPS.length)];
    }
}
