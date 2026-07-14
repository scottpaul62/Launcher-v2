package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Profils Heroes-World : chaque profil possede sa propre disposition de HUD. */
public final class HWProfiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<String> names = new ArrayList<>();
    public static String active = "Par defaut";
    static { names.add("Par defaut"); names.add("Heroes-World"); names.add("PvP"); names.add("Survie"); names.add("Skyblock"); names.add("Performance"); }
    private HWProfiles() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_profiles.json"); }
    public static String slug(String n) { return n == null ? "x" : n.toLowerCase().replaceAll("[^a-z0-9]+", "_"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            if (o.has("active")) active = o.get("active").getAsString();
            if (o.has("names")) { names.clear(); JsonArray a = o.getAsJsonArray("names"); for (int i = 0; i < a.size(); i++) names.add(a.get(i).getAsString()); }
        } catch (Exception ignored) {}
    }
    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("active", active);
            JsonArray a = new JsonArray(); for (String n : names) a.add(n); o.add("names", a);
            Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
    public static void switchTo(String name) {
        if (name == null || name.equals(active)) return;
        HWHudManager.save();       // sauve le profil courant
        active = name; save();
        HWHudManager.load();       // charge la disposition du nouveau profil
    }
    public static void create(String name) { if (name != null && !name.isEmpty() && !names.contains(name)) { names.add(name); save(); } }
}
