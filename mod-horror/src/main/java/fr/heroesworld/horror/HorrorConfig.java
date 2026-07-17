package fr.heroesworld.horror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config client du mod horreur — .minecraft/config/heroworld-horror.json.
 * Doit rester alignée avec la config du plugin serveur HeroesHorror (matériau + custom model data).
 */
public final class HorrorConfig {
    /** Touche toggle lampe torche (code GLFW). 82 = R — libre en vanilla 1.20.6 (F = main secondaire). */
    public static int torchKey = 82;
    public static boolean hud = true;
    public static boolean particles = true;
    /** Identifiant de l'item vanilla porteur de l'item ItemsAdder heroesworld:lampe_torche. */
    public static String torchMaterial = "minecraft:prismarine_shard";
    /** CustomModelData assigné par ItemsAdder (1er item sur matériau vierge = 10000). */
    public static int torchCustomModelData = 10000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HorrorConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("heroworld-horror.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) { save(); return; }
            JsonObject o = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            if (o.has("torchKey")) torchKey = o.get("torchKey").getAsInt();
            if (o.has("hud")) hud = o.get("hud").getAsBoolean();
            if (o.has("particles")) particles = o.get("particles").getAsBoolean();
            if (o.has("torchMaterial")) torchMaterial = o.get("torchMaterial").getAsString();
            if (o.has("torchCustomModelData")) torchCustomModelData = o.get("torchCustomModelData").getAsInt();
        } catch (Throwable t) {
            HWHorrorClient.LOG.warn("[HeroesWorld-Horror] config illisible, valeurs par défaut", t);
        }
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("torchKey", torchKey);
            o.addProperty("hud", hud);
            o.addProperty("particles", particles);
            o.addProperty("torchMaterial", torchMaterial);
            o.addProperty("torchCustomModelData", torchCustomModelData);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            HWHorrorClient.LOG.warn("[HeroesWorld-Horror] impossible d'écrire la config", t);
        }
    }
}
