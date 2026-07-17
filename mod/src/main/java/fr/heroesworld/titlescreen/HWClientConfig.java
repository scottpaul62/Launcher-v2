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
    public static boolean hudBackground = true;      // fond sombre derriere les widgets texte
    public static boolean cosmeticsParticles = true;  // particules auras/ailes (perf)
    public static boolean reduceMotion = false;       // accessibilite : halos/animations des menus
    public static boolean highContrast = false;       // accessibilite : force le fond des widgets HUD
    public static int zoomKey = 67;                    // GLFW_KEY_C
    public static float zoomFactor = 4f;               // x2..x8
    public static boolean titleFx = true;              // effets vivants de l'ecran titre
    public static boolean menuSounds = true;           // sons de survol des menus
    public static String lastSeen = "";                // derniere version vue (quoi de neuf)
    private HWClientConfig() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_client.json"); }
    public static void load() {
        try {
            Path f = file(); if (!Files.exists(f)) return;
            JsonObject o = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            if (o.has("hudBackground")) hudBackground = o.get("hudBackground").getAsBoolean();
            if (o.has("cosmeticsParticles")) cosmeticsParticles = o.get("cosmeticsParticles").getAsBoolean();
            if (o.has("reduceMotion")) reduceMotion = o.get("reduceMotion").getAsBoolean();
            if (o.has("highContrast")) highContrast = o.get("highContrast").getAsBoolean();
            if (o.has("zoomKey")) zoomKey = o.get("zoomKey").getAsInt();
            if (o.has("zoomFactor")) zoomFactor = HWZoom.clampFactor(o.get("zoomFactor").getAsFloat());
            if (o.has("titleFx")) titleFx = o.get("titleFx").getAsBoolean();
            if (o.has("menuSounds")) menuSounds = o.get("menuSounds").getAsBoolean();
            if (o.has("lastSeen")) lastSeen = o.get("lastSeen").getAsString();
        } catch (Exception ignored) {}
    }
    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("hudBackground", hudBackground);
            o.addProperty("cosmeticsParticles", cosmeticsParticles);
            o.addProperty("reduceMotion", reduceMotion);
            o.addProperty("highContrast", highContrast);
            o.addProperty("zoomKey", zoomKey);
            o.addProperty("zoomFactor", zoomFactor);
            o.addProperty("titleFx", titleFx);
            o.addProperty("menuSounds", menuSounds);
            o.addProperty("lastSeen", lastSeen);
            Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
