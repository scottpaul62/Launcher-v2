package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Systeme de HUD extensible du client HERO WORLD : mods activables, ancres aux coins, deplacables. */
public final class HWHudManager {
    public static final Logger LOG = LoggerFactory.getLogger("HeroWorldClient");
    private static final Set<String> logged = new HashSet<>();

    /** Ancres : 0=haut-gauche, 1=haut-droite, 2=bas-gauche, 3=bas-droite. */
    public static final class El {
        public final String id, name, category;
        public final int danchor, dox, doy; public final boolean de;
        public boolean enabled; public int anchor, ox, oy;
        public El(String id, String name, String category, boolean enabled, int anchor, int ox, int oy) {
            this.id = id; this.name = name; this.category = category;
            this.enabled = enabled; this.anchor = anchor; this.ox = ox; this.oy = oy;
            this.de = enabled; this.danchor = anchor; this.dox = ox; this.doy = oy;
        }
    }

    public static final List<El> ELEMENTS = new ArrayList<>();
    static {
        ELEMENTS.add(new El("fps", "Compteur FPS", "Performance", true, 0, 4, 4));
        ELEMENTS.add(new El("ping", "Ping", "Performance", false, 0, 4, 18));
        ELEMENTS.add(new El("coords", "Coordonnees", "Info", false, 0, 4, 32));
        ELEMENTS.add(new El("direction", "Direction (boussole)", "Info", false, 4, 0, 4));
        ELEMENTS.add(new El("time", "Horloge", "Info", false, 1, 4, 4));
        ELEMENTS.add(new El("day", "Compteur de jours", "Info", false, 1, 4, 18));
        ELEMENTS.add(new El("session", "Duree de session", "Info", false, 1, 4, 32));
        ELEMENTS.add(new El("effects", "Effets de potion", "Info", false, 1, 4, 46));
        ELEMENTS.add(new El("keystrokes", "KeyStrokes", "Mecanique", false, 2, 10, 52));
        ELEMENTS.add(new El("cps", "CPS (clics/s)", "Mecanique", false, 2, 10, 96));
        ELEMENTS.add(new El("armor", "Statut d'armure", "Combat", false, 3, 12, 52));
    }

    private static final long SESSION_START = System.currentTimeMillis();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWHudManager() {}

    // ---- CPS : file des clics recents (alimentee par MouseClickMixin) ----
    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    public static void onClick() {
        synchronized (CLICKS) {
            CLICKS.addLast(System.currentTimeMillis());
            while (CLICKS.size() > 60) CLICKS.pollFirst();
        }
    }
    private static int cps() {
        long now = System.currentTimeMillis();
        synchronized (CLICKS) {
            while (!CLICKS.isEmpty() && now - CLICKS.peekFirst() > 1000) CLICKS.pollFirst();
            return CLICKS.size();
        }
    }

    public static El byId(String id) { for (El e : ELEMENTS) if (e.id.equals(id)) return e; return null; }
    public static void resetPos(El e) { e.anchor = e.danchor; e.ox = e.dox; e.oy = e.doy; }
    public static void resetAll() { for (El e : ELEMENTS) { e.enabled = e.de; e.anchor = e.danchor; e.ox = e.dox; e.oy = e.doy; } }
    public static String anchorName(int a) {
        switch (a) { case 1: return "haut-droite"; case 2: return "bas-gauche"; case 3: return "bas-droite";
                     case 4: return "haut-centre"; case 5: return "bas-centre"; default: return "haut-gauche"; }
    }

    public static int actualX(El e, int screenW, int w) {
        if (e.anchor == 1 || e.anchor == 3) return Math.max(0, screenW - w - e.ox);
        if (e.anchor == 4 || e.anchor == 5) return Math.max(0, (screenW - w) / 2 + e.ox);
        return e.ox;
    }
    public static int actualY(El e, int screenH, int h) {
        return (e.anchor == 2 || e.anchor == 3 || e.anchor == 5) ? Math.max(0, screenH - h - e.oy) : e.oy;
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_hud.json"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) { LOG.info("[HUD] pas de config, valeurs par defaut ({} mods)", ELEMENTS.size()); return; }
            JsonArray arr = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonArray.class);
            if (arr == null) return;
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                El e = byId(o.get("id").getAsString());
                if (e != null) {
                    if (o.has("enabled")) e.enabled = o.get("enabled").getAsBoolean();
                    if (o.has("anchor")) e.anchor = o.get("anchor").getAsInt();
                    if (o.has("ox")) e.ox = o.get("ox").getAsInt();
                    if (o.has("oy")) e.oy = o.get("oy").getAsInt();
                }
            }
            LOG.info("[HUD] config chargee ({} mods)", ELEMENTS.size());
        } catch (Exception ex) { LOG.warn("[HUD] echec chargement config", ex); }
    }

    public static void save() {
        try {
            JsonArray arr = new JsonArray();
            for (El e : ELEMENTS) {
                JsonObject o = new JsonObject();
                o.addProperty("id", e.id); o.addProperty("enabled", e.enabled);
                o.addProperty("anchor", e.anchor); o.addProperty("ox", e.ox); o.addProperty("oy", e.oy);
                arr.add(o);
            }
            Files.writeString(file(), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (Exception ex) { LOG.warn("[HUD] echec sauvegarde config", ex); }
    }

    // ---- Rendu en jeu (appele par HudMixin) ----
    public static void renderAll(DrawContext ctx, MinecraftClient mc) {
        if (mc == null || mc.player == null || ctx == null) return;
        if (mc.options != null && mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HWHudEditScreen) return; // l'editeur dessine lui-meme
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        for (El e : ELEMENTS) {
            if (!e.enabled) continue;
            try {
                int w = width(mc, e), h = height(e);
                renderAt(ctx, mc, e, actualX(e, sw, w), actualY(e, sh, h));
            } catch (Throwable t) {
                if (logged.add(e.id)) LOG.warn("[HUD] erreur de rendu du mod '{}'", e.id, t);
            }
        }
    }

    public static int width(MinecraftClient mc, El e) {
        try {
            switch (e.id) {
                case "keystrokes": return 40;
                case "armor": return 18;
                case "effects": return 74;
                case "direction": return 110;
                default: return mc.textRenderer.getWidth(text(mc, e)) + 4;
            }
        } catch (Throwable t) { return 40; }
    }
    public static int height(El e) {
        switch (e.id) { case "keystrokes": return 40; case "armor": return 74; case "effects": return 66; case "direction": return 13; default: return 10; }
    }

    static String text(MinecraftClient mc, El e) {
        switch (e.id) {
            case "fps": return "§e" + mc.getCurrentFps() + " §7FPS";
            case "ping": {
                int p = 0;
                net.minecraft.client.network.ClientPlayNetworkHandler nh = mc.getNetworkHandler();
                if (nh != null) {
                    net.minecraft.client.network.PlayerListEntry pe = nh.getPlayerListEntry(mc.player.getUuid());
                    if (pe != null) p = pe.getLatency();
                }
                return "§7Ping §f" + p + "ms";
            }
            case "cps": return "§bCPS §f" + cps();
            case "coords": return "§7XYZ §f" + fl(mc.player.getX()) + " " + fl(mc.player.getY()) + " " + fl(mc.player.getZ());
            case "time": { String t = java.time.LocalTime.now().toString(); return "§f" + (t.length() >= 5 ? t.substring(0, 5) : t); }
            case "day": return "§7Jour §f" + (mc.world != null ? (mc.world.getTimeOfDay() / 24000L) : 0);
            case "session": { long s = (System.currentTimeMillis() - SESSION_START) / 1000; return String.format("§7Session §f%02d:%02d", s / 60, s % 60); }
            default: return e.name;
        }
    }

    public static void renderAt(DrawContext ctx, MinecraftClient mc, El e, int ax, int ay) {
        TextRenderer tr = mc.textRenderer;
        switch (e.id) {
            case "keystrokes": drawKeystrokes(ctx, mc, ax, ay); break;
            case "armor": drawArmor(ctx, mc, ax, ay); break;
            case "effects": drawEffects(ctx, mc, ax, ay); break;
            case "direction": drawCompass(ctx, mc, ax, ay); break;
            default: {
                String s = text(mc, e);
                int w = tr.getWidth(s);
                ctx.fill(ax - 2, ay - 1, ax + w + 2, ay + 9, 0x66000000);
                ctx.drawTextWithShadow(tr, Text.literal(s), ax, ay, 0xFFFFFFFF);
            }
        }
    }

    private static long fl(double v) { return (long) Math.floor(v); }

    private static final String[] COMPASS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
    private static float wrap180(float a) { a %= 360f; if (a > 180f) a -= 360f; if (a < -180f) a += 360f; return a; }

    /** Boussole facon Lunar : ruban horizontal, cardinal courant au centre. */
    private static void drawCompass(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int W = 110, H = 13;
        ctx.fill(x, y, x + W, y + H, 0x88000000);
        float yaw = mc.player.getYaw();
        float fov = 140f, ppd = W / fov;
        for (int i = 0; i < 8; i++) {
            float d = wrap180(i * 45f - yaw);
            if (Math.abs(d) <= fov / 2f) {
                int px = (int) (x + W / 2f + d * ppd);
                ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(COMPASS[i]), px, y + 3, (i % 2 == 0) ? 0xFFFFFFFF : 0xFF9A93A6);
            }
        }
        ctx.fill(x + W / 2, y - 1, x + W / 2 + 1, y + H + 1, 0xFFE8C56A);
    }

    private static void drawEffects(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int row = 0;
        for (StatusEffectInstance eff : mc.player.getStatusEffects()) {
            if (row >= 6) break;
            int secs = eff.getDuration() / 20;
            String s = "§d" + String.format("%d:%02d", secs / 60, secs % 60) + (eff.getAmplifier() > 0 ? " §7x" + (eff.getAmplifier() + 1) : "");
            int w = mc.textRenderer.getWidth(s);
            int yy = y + row * 11;
            ctx.fill(x - 2, yy - 1, x + w + 2, yy + 9, 0x66000000);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(s), x, yy, 0xFFFFFFFF);
            row++;
        }
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
