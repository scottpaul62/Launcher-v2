package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration du client HEROES-WORLD (config/heroworld.json).
 * Permet de changer l'adresse du serveur sans recompiler le mod
 * (dev : 192.168.1.109 — prod : play.heroesworld.fr).
 */
public final class HWConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_ADDRESS = "192.168.1.109:25565";

    private static String address = DEFAULT_ADDRESS;

    private HWConfig() {}

    public static String address() {
        return address;
    }

    public static void load() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("heroworld.json");
            if (Files.exists(file)) {
                JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
                if (json != null && json.has("address")) {
                    String value = json.get("address").getAsString().trim();
                    if (!value.isEmpty()) {
                        address = value;
                    }
                }
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("address", DEFAULT_ADDRESS);
                Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Configuration illisible : on garde l'adresse par défaut, jamais de crash.
            address = DEFAULT_ADDRESS;
        }
    }
}
