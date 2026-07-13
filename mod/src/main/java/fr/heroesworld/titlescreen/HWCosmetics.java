package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cosmétiques du client HERO WORLD (persistés dans config/heroworld_cosmetics.json). Étape 1 : auras. */
public final class HWCosmetics {
    public static final String[] AURA_NAMES = {
        "Aucune", "Éclairs de Zeus", "Halo d'Apollon", "Flammes d'Héphaïstos", "Ombre d'Hadès", "Pétales d'Olympe"
    };
    public static int aura = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWCosmetics() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_cosmetics.json"); }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject j = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
                if (j != null && j.has("aura")) aura = clamp(j.get("aura").getAsInt());
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject j = new JsonObject();
            j.addProperty("aura", aura);
            Files.writeString(file(), GSON.toJson(j), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public static void setAura(int a) { aura = clamp(a); save(); }
    private static int clamp(int a) { int n = AURA_NAMES.length; return ((a % n) + n) % n; }
}
