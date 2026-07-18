package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Astuces et lore HERO WORLD, en rotation sur l'ecran titre. Version distante (R2) prioritaire. */
public final class HWTips {
    private static final String[] DEFAULTS = {
        "Maj-droite en jeu ouvre le centre HERO WORLD (mods, reglages, waypoints).",
        "Maintiens C pour zoomer — touche et facteur reglables dans MODS > Zoom.",
        "Clique « Editer le HUD » pour deplacer chaque widget a la souris.",
        "Le vestiaire te montre chaque cosmetique sur TON personnage avant de l'equiper.",
        "Cree un waypoint a ta position depuis l'onglet WAYPOINTS du centre.",
        "Chaque profil (PvP, Survie...) garde sa propre disposition de HUD.",
        "Les drachmes se gagnent en jouant — la boutique arrive bientot.",
        "L'Olympe veille sur ceux qui explorent ses cieux...",
    };
    private static String[] tips = DEFAULTS;
    static {
        try (InputStream in = HWRemote.open("tips.json")) {
            if (in != null) {
                JsonObject j = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
                JsonArray a = j != null ? j.getAsJsonArray("tips") : null;
                if (a != null && a.size() > 0) {
                    String[] arr = new String[a.size()];
                    for (int i = 0; i < a.size(); i++) arr[i] = a.get(i).getAsString();
                    tips = arr;
                }
            }
        } catch (Throwable ignored) {}
    }
    private HWTips() {}
    public static String current() {
        long t = System.currentTimeMillis();
        return tips[(int) ((t / 8000L) % tips.length)];
    }
}
