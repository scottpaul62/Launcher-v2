package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Systeme de HUD extensible du client HERO WORLD (mods activables + deplacables). */
public final class HWHudManager {

    /** Un element de HUD (mod affiche a l'ecran). */
    public static final class El {
        public final String id, name, category;
        public final boolean de; public final int dx, dy; // valeurs par defaut
        public boolean enabled; public int x, y;
        public El(String id, String name, String category, boolean enabled, int x, int y) {
            this.id = id; this.name = name; this.category = category;
            this.enabled = enabled; this.x = x; this.y = y;
            this.de = enabled; this.dx = x; this.dy = y;
        }
    }

    public static final List<El> ELEMENTS = new ArrayList<>();
    static {
        ELEMENTS.add(new El("fps", "Compteur FPS", "Performance", true, 4, 4));
        ELEMENTS.add(new El("coords", "Coordonnees", "Info", false, 4, 16));
        ELEMENTS.add(new El("direction", "Direction", "Info", false, 4, 28));
        ELEMENTS.add(new El("time", "Horloge", "Info", false, 4, 40));
        ELEMENTS.add(new El("day", "Compteur de jours", "Info", false, 4, 52));
        ELEMENTS.add(new El("session", "Duree de session", "Info", false, 4, 64));
        ELEMENTS.add(new El("keystrokes", "KeyStrokes", "Mecanique", false, 10, 120));
        ELEMENTS.add(new El("armor", "Statut d'armure", "Combat", false, 12, 160));
    }

    private static final long SESSION_START = System.currentTimeMillis();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWHudManager() {}

    public static El byId(String id) { for (El e : ELEMENTS) if (e.id.equals(id)) return e; return null; }
    public static void resetPos(El e) { e.x = e.dx; e.y = e.dy; }
    public static void resetAll() { for (El e : ELEMENTS) { e.enabled = e.de; e.x = e.dx; e.y = e.dy; } }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_hud.json"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonArray arr = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonArray.class);
            if (arr == null) return;
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                El e = byId(o.get("id").getAsString());
                if (e != null) {
                    if (o.has("enabled")) e.enabled = o.get("enabled").getAsBoolean();
                    if (o.has("x")) e.x = o.get("x").getAsInt();
                    if (o.has("y")) e.y = o.get("y").getAsInt();
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonArray arr = new JsonArray();
            for (El e : ELEMENTS) {
                JsonObject o = new JsonObject();
                o.addProperty("id", e.id); o.addProperty("enabled", e.enabled);
                o.addProperty("x", e.x); o.addProperty("y", e.y);
                arr.add(o);
            }
            Files.writeString(file(), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    // ---- Rendu en jeu (appele par HudMixin) ----
    public static void renderAll(DrawContext ctx, MinecraftClient mc) {
        if (mc == null || mc.player == null || ctx == null) return;
        if (mc.options != null && mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HWHudEditScreen) return; // l'editeur dessine lui-meme
        for (El e : ELEMENTS) if (e.enabled) { try { renderOne(ctx, mc, e); } catch (Throwable ignored) {} }
    }

    public static int width(MinecraftClient mc, El e) {
        try {
            switch (e.id) {
                case "keystrokes": return 40;
                case "armor": return 18;
                default: return mc.textRenderer.getWidth(text(mc, e)) + 4;
            }
        } catch (Throwable t) { return 40; }
    }
    public static int height(El e) {
        switch (e.id) { case "keystrokes": return 40; case "armor": return 74; default: return 10; }
    }

    static String text(MinecraftClient mc, El e) {
        switch (e.id) {
            case "fps": return "§e" + mc.getCurrentFps() + " §7FPS";
            case "coords": return "§7XYZ §f" + fl(mc.player.getX()) + " " + fl(mc.player.getY()) + " " + fl(mc.player.getZ());
            case "direction": return "§7Dir §f" + dir(mc.player.getYaw());
            case "time": { String t = java.time.LocalTime.now().toString(); return "§f" + (t.length() >= 5 ? t.substring(0, 5) : t); }
            case "day": return "§7Jour §f" + (mc.world != null ? (mc.world.getTimeOfDay() / 24000L) : 0);
            case "session": { long s = (System.currentTimeMillis() - SESSION_START) / 1000; return String.format("§7Session §f%02d:%02d", s / 60, s % 60); }
            default: return e.name;
        }
    }

    static void renderOne(DrawContext ctx, MinecraftClient mc, El e) {
        TextRenderer tr = mc.textRenderer;
        switch (e.id) {
            case "keystrokes": drawKeystrokes(ctx, mc, e.x, e.y); break;
            case "armor": drawArmor(ctx, mc, e.x, e.y); break;
            default: {
                String s = text(mc, e);
                int w = tr.getWidth(s);
                ctx.fill(e.x - 2, e.y - 1, e.x + w + 2, e.y + 9, 0x66000000);
                ctx.drawTextWithShadow(tr, Text.literal(s), e.x, e.y, 0xFFFFFFFF);
            }
        }
    }

    private static long fl(double v) { return (long) Math.floor(v); }

    private static String dir(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        String[] d = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
        return d[(int) Math.round(y / 45f) % 8];
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient mc, int x, int y) {
        keyBox(ctx, mc, x + 14, y, 12, 12, "W", mc.options.forwardKey.isPressed());
        keyBox(ctx, mc, x, y + 14, 12, 12, "A", mc.options.leftKey.isPressed());
        keyBox(ctx, mc, x + 14, y + 14, 12, 12, "S", mc.options.backKey.isPressed());
        keyBox(ctx, mc, x + 28, y + 14, 12, 12, "D", mc.options.rightKey.isPressed());
        keyBox(ctx, mc, x, y + 28, 19, 12, "LMB", mc.options.attackKey.isPressed());
        keyBox(ctx, mc, x + 21, y + 28, 19, 12, "RMB", mc.options.useKey.isPressed());
    }
    private static void keyBox(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, boolean pressed) {
        ctx.fill(x, y, x + w, y + h, pressed ? 0xD0E8C56A : 0x88000000);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(label), x + w / 2, y + 2, pressed ? 0xFF141018 : 0xFFFFFFFF);
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int ay = y + 54;
        for (ItemStack st : mc.player.getArmorItems()) {
            if (st != null && !st.isEmpty()) ctx.drawItem(st, x, ay);
            ay -= 18;
        }
    }
}
