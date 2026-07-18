package fr.heroesworld.titlescreen.gek;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registre des cosmetiques data-driven : catalog.json + compat.json + chargement paresseux des gek. */
public final class HWGekRegistry {
    public static final Logger LOG = LoggerFactory.getLogger("HeroWorldGek");
    private static final String BASE = "/assets/heroworld/cosmetics/";
    private static final Gson GSON = new Gson();

    public static final class Item {
        public int id; public String key = "", name = "", category = "", renderer = "particle", path = "";
        public int particle = 0; public String rarity = "common";
        public List<String> colors = new ArrayList<>(), tags = new ArrayList<>();
    }

    private static final List<Item> ITEMS = new ArrayList<>();
    private static final Map<String, Item> BY_KEY = new HashMap<>();
    private static final Map<String, HWGekCosmetic> LOADED = new HashMap<>();
    private static final Map<String, List<String>> COMPAT = new HashMap<>(); // preset -> incompatibles resolus
    private static boolean loaded = false;

    private HWGekRegistry() {}

    public static InputStream stream(String rel) {
        InputStream remote = fr.heroesworld.titlescreen.HWRemote.open("cosmetics/" + rel);
        if (remote != null) return remote;
        InputStream in = HWGekRegistry.class.getResourceAsStream(BASE + rel);
        if (in == null) throw new IllegalStateException("resource absente : " + BASE + rel);
        return in;
    }
    public static JsonObject readJson(String rel) throws Exception {
        try (InputStream in = stream(rel)) {
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    /** Charge et valide catalog + compat. Jamais de crash : item invalide = ignore + log. */
    public static void load() {
        if (loaded) return;
        loaded = true;
        int ok = 0, ko = 0;
        try {
            JsonObject cat = readJson("catalog.json");
            int version = cat.has("version") ? cat.get("version").getAsInt() : 0;
            JsonArray items = cat.getAsJsonArray("items");
            for (int i = 0; i < items.size(); i++) {
                try {
                    JsonObject o = items.get(i).getAsJsonObject();
                    Item it = new Item();
                    it.id = o.get("id").getAsInt();
                    it.key = o.get("key").getAsString();
                    it.name = o.get("name").getAsString();
                    it.category = o.get("category").getAsString();
                    it.renderer = o.has("renderer") ? o.get("renderer").getAsString() : "particle";
                    it.particle = o.has("particle") ? o.get("particle").getAsInt() : 0;
                    it.path = o.has("path") ? o.get("path").getAsString() : "";
                    it.rarity = o.has("rarity") ? o.get("rarity").getAsString() : "common";
                    if (o.has("colors")) o.getAsJsonArray("colors").forEach(e -> it.colors.add(e.getAsString()));
                    if (o.has("tags")) o.getAsJsonArray("tags").forEach(e -> it.tags.add(e.getAsString()));
                    if (it.key.isEmpty() || it.category.isEmpty()) throw new IllegalArgumentException("key/category requis");
                    if (it.renderer.equals("gek") && it.path.isEmpty()) throw new IllegalArgumentException("path requis pour gek");
                    if (BY_KEY.containsKey(it.key)) throw new IllegalArgumentException("key dupliquee");
                    ITEMS.add(it); BY_KEY.put(it.key, it); ok++;
                } catch (Throwable t) { ko++; LOG.warn("[GEK] item {} invalide : {}", i, t.getMessage()); }
            }
            LOG.info("[GEK] catalogue v{} : {} items charges, {} ignores", version, ok, ko);
        } catch (Throwable t) { LOG.warn("[GEK] catalog.json illisible", t); }
        try {
            JsonObject comp = readJson("compat.json");
            JsonArray presets = comp.getAsJsonArray("presets");
            Map<String, JsonObject> raw = new HashMap<>();
            for (int i = 0; i < presets.size(); i++) {
                JsonObject p = presets.get(i).getAsJsonObject();
                raw.put(p.get("name").getAsString(), p);
            }
            for (String name : raw.keySet()) {
                List<String> inc = new ArrayList<>();
                JsonObject cur = raw.get(name);
                int guard = 0;
                while (cur != null && guard++ < 8) {
                    if (cur.has("incompatible")) cur.getAsJsonArray("incompatible").forEach(e -> {
                        if (!inc.contains(e.getAsString())) inc.add(e.getAsString());
                    });
                    cur = cur.has("parent") ? raw.get(cur.get("parent").getAsString()) : null;
                }
                COMPAT.put(name, inc);
            }
            LOG.info("[GEK] compat : {} presets resolus", COMPAT.size());
        } catch (Throwable t) { LOG.warn("[GEK] compat.json illisible", t); }
    }

    public static List<Item> items() { load(); return ITEMS; }
    public static Item byKey(String key) { load(); return BY_KEY.get(key); }

    /** L'item "wings" du catalogue correspondant a l'index interne HWCosmetics.wings (1..n). */
    public static Item wingsByIndex(int idx) {
        load();
        int seen = 0;
        for (Item it : ITEMS) {
            if (!it.category.equals("wings")) continue;
            seen++;
            if (seen == idx) return it;
        }
        return null;
    }

    /** Cosmetique gek charge paresseusement (null si echec — repli appelant). */
    public static HWGekCosmetic gek(String key) {
        load();
        if (LOADED.containsKey(key)) return LOADED.get(key);
        HWGekCosmetic c = null;
        try {
            Item it = BY_KEY.get(key);
            if (it != null && it.renderer.equals("gek")) {
                JsonObject hw = readJson(it.path + "/" + it.key + ".hw.json");
                String geo = it.path + "/" + (hw.has("geometry") ? hw.get("geometry").getAsString() : it.key + ".geo.json");
                String tex = it.path + "/" + (hw.has("texture") ? hw.get("texture").getAsString() : it.key + ".png");
                String ani = it.path + "/" + (hw.has("animation") ? hw.get("animation").getAsString() : it.key + ".anim.json");
                HWGekModel model = HWGekModel.load(geo, tex, it.key);
                JsonObject anim = readJson(ani);
                c = new HWGekCosmetic(it.key, hw, model, anim);
                LOG.info("[GEK] '{}' charge (type {}, bone {})", it.key, c.type, c.attachedBone);
            }
        } catch (Throwable t) { LOG.warn("[GEK] chargement '{}' en echec", key, t); }
        LOADED.put(key, c);
        return c;
    }
}
