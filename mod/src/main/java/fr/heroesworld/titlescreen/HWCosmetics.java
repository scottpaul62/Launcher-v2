package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cosmétiques du client HERO WORLD (config/heroworld_cosmetics.json). */
public final class HWCosmetics {
    public static final String[] AURA_NAMES = {
        "Aucune", "Éclairs de Zeus", "Halo d'Apollon", "Flammes d'Héphaïstos", "Ombre d'Hadès", "Pétales d'Olympe"
    };
    public static final String[] WING_NAMES = {
        "Aucune", "Ailes d'Hermès", "Ailes de foudre", "Ailes de flammes", "Ailes d'Hadès"
    };
    public static int aura = 0;
    public static int wings = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWCosmetics() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_cosmetics.json"); }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject j = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
                if (j != null) {
                    if (j.has("aura")) aura = clamp(j.get("aura").getAsInt(), AURA_NAMES.length);
                    if (j.has("wings")) wings = clamp(j.get("wings").getAsInt(), WING_NAMES.length);
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject j = new JsonObject();
            j.addProperty("aura", aura);
            j.addProperty("wings", wings);
            Files.writeString(file(), GSON.toJson(j), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public static void setAura(int a) { aura = clamp(a, AURA_NAMES.length); save(); }
    public static void setWings(int w) { wings = clamp(w, WING_NAMES.length); save(); }
    private static int clamp(int a, int n) { return ((a % n) + n) % n; }
}
