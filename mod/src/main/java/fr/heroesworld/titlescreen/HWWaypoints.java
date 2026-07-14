package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Waypoints Heroes-World : par dimension, couleur, visibilite. Sauvegarde atomique JSON. */
public final class HWWaypoints {
    public static final class WP {
        public String name = "Waypoint";
        public String dim = "minecraft:overworld";
        public double x, y, z;
        public int color = 0xFFE8C56A;
        public boolean visible = true;
    }

    public static final List<WP> LIST = new ArrayList<>();
    public static final int[] COLORS = {0xFFE8C56A, 0xFF49BDF2, 0xFF48C78E, 0xFFE45B68, 0xFFB07CE8, 0xFFE7B84B, 0xFFA9AFBA};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWWaypoints() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_waypoints.json"); }

    public static String curDim(MinecraftClient mc) {
        try { return mc.world.getRegistryKey().getValue().toString(); } catch (Throwable t) { return "minecraft:overworld"; }
    }
    public static String dimShort(String dim) {
        if (dim == null) return "?";
        if (dim.contains("nether")) return "Nether";
        if (dim.contains("the_end")) return "End";
        if (dim.contains("overworld")) return "Monde";
        int i = dim.indexOf(':');
        return i >= 0 ? dim.substring(i + 1) : dim;
    }

    public static void addAtPlayer(MinecraftClient mc) {
        try {
            if (mc == null || mc.player == null) return;
            WP w = new WP();
            w.name = "Waypoint " + (LIST.size() + 1);
            w.dim = curDim(mc);
            w.x = mc.player.getX(); w.y = mc.player.getY(); w.z = mc.player.getZ();
            w.color = COLORS[LIST.size() % COLORS.length];
            LIST.add(w);
            save();
        } catch (Throwable ignored) {}
    }
    public static void remove(WP w) { LIST.remove(w); save(); }
    public static void cycleColor(WP w) {
        int i = 0;
        for (int k = 0; k < COLORS.length; k++) if (COLORS[k] == w.color) { i = k + 1; break; }
        w.color = COLORS[i % COLORS.length];
        save();
    }

    public static int dist(MinecraftClient mc, WP w) {
        try {
            double dx = w.x - mc.player.getX(), dy = w.y - mc.player.getY(), dz = w.z - mc.player.getZ();
            return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
        } catch (Throwable t) { return 0; }
    }
    private static final String[] DIRS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
    public static String dirTo(MinecraftClient mc, WP w) {
        try {
            double dx = w.x - mc.player.getX(), dz = w.z - mc.player.getZ();
            // Minecraft : yaw 0 = sud (+Z) ; angle de la cible dans le meme repere
            double ang = Math.toDegrees(Math.atan2(-dx, dz));
            int idx = (int) Math.floor(((ang + 360 + 22.5) % 360) / 45.0);
            return DIRS[idx % 8];
        } catch (Throwable t) { return ""; }
    }

    /** Les n waypoints visibles les plus proches, dans la dimension courante. */
    public static List<WP> nearest(MinecraftClient mc, int n) {
        List<WP> out = new ArrayList<>();
        try {
            if (mc == null || mc.player == null) return out;
            String dim = curDim(mc);
            for (WP w : LIST) if (w.visible && dim.equals(w.dim)) out.add(w);
            out.sort(Comparator.comparingInt(w -> dist(mc, w)));
            if (out.size() > n) out = new ArrayList<>(out.subList(0, n));
        } catch (Throwable ignored) {}
        return out;
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject root = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("waypoints")) return;
            LIST.clear();
            JsonArray arr = root.getAsJsonArray("waypoints");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                WP w = new WP();
                if (o.has("name")) w.name = o.get("name").getAsString();
                if (o.has("dim")) w.dim = o.get("dim").getAsString();
                if (o.has("x")) w.x = o.get("x").getAsDouble();
                if (o.has("y")) w.y = o.get("y").getAsDouble();
                if (o.has("z")) w.z = o.get("z").getAsDouble();
                if (o.has("color")) w.color = o.get("color").getAsInt();
                if (o.has("visible")) w.visible = o.get("visible").getAsBoolean();
                LIST.add(w);
            }
        } catch (Exception ignored) {}
    }
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (WP w : LIST) {
                JsonObject o = new JsonObject();
                o.addProperty("name", w.name); o.addProperty("dim", w.dim);
                o.addProperty("x", w.x); o.addProperty("y", w.y); o.addProperty("z", w.z);
                o.addProperty("color", w.color); o.addProperty("visible", w.visible);
                arr.add(o);
            }
            root.add("waypoints", arr);
            Path f = file(), tmp = f.resolveSibling(f.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception e2) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception ignored) {}
    }
}
