package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reglages client Heroes-World persistants (vraies options, pas de donnees fictives). */
public final class HWClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean hudBackground = true;   // fond sombre derriere les widgets texte
    private HWClientConfig() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_client.json"); }
    public static void load() {
        try {
            Path f = file(); if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (o != null && o.has("hudBackground")) hudBackground = o.get("hudBackground").getAsBoolean();
        } catch (Exception ignored) {}
    }
    public static void save() {
        try { JsonObject o = new JsonObject(); o.addProperty("hudBackground", hudBackground); Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8); }
        catch (Exception ignored) {}
    }
}
