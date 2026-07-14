package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Systeme de HUD Heroes-World : ancrage semantique (9), offsets normalises, echelle, zone sure, sauvegarde atomique. */
public final class HWHudManager {
    public static final Logger LOG = LoggerFactory.getLogger("HeroWorldClient");
    private static final Set<String> logged = new HashSet<>();

    public static final int SAFE = 12, SNAP = 8, GRID = 8;
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.0f;
    public static final int GUIDE = 0xFF49BDF2, SEL = 0xFF49BDF2;

    /** 0=TL 1=TC 2=TR 3=CL 4=C 5=CR 6=BL 7=BC 8=BR */
    public static final String[] ANCHORS = {
        "top_left", "top_center", "top_right",
        "center_left", "center", "center_right",
        "bottom_left", "bottom_center", "bottom_right"
    };

    public static final class El {
        public final String id, name, category;
        public final int danchor; public final float dox, doy, dscale; public final boolean denabled;
        public int anchor; public float ox, oy, scale; public boolean enabled, locked;
        public int color = 0;   // 0 = couleur par defaut, sinon ARGB
        public int bgMode = 0;  // 0 = suit le reglage global, 1 = force ON, 2 = force OFF
        public int opt = 0;     // option specifique au widget (ex. style du crosshair)
        public El(String id, String name, String category, boolean enabled, int anchor, float ox, float oy) {
            this.id = id; this.name = name; this.category = category;
            this.enabled = enabled; this.anchor = anchor; this.ox = ox; this.oy = oy; this.scale = 1f; this.locked = false;
            this.denabled = enabled; this.danchor = anchor; this.dox = ox; this.doy = oy; this.dscale = 1f;
        }
    }

    /** Vrai pendant le rendu de l'editeur HUD : les widgets sans donnees affichent un apercu. */
    public static boolean editorPreview = false;

    public static final List<El> ELEMENTS = new ArrayList<>();
    static {
        ELEMENTS.add(new El("fps", "Compteur FPS", "Performance", true, 0, 0f, 0f));
        ELEMENTS.add(new El("ping", "Ping", "Performance", false, 0, 0f, 0.018f));
        ELEMENTS.add(new El("coords", "Coordonnees", "Info", false, 0, 0f, 0.036f));
        ELEMENTS.add(new El("direction", "Direction (boussole)", "Info", false, 1, 0f, 0f));
        ELEMENTS.add(new El("time", "Horloge", "Info", false, 2, 0f, 0f));
        ELEMENTS.add(new El("day", "Compteur de jours", "Info", false, 2, 0f, 0.018f));
        ELEMENTS.add(new El("session", "Duree de session", "Info", false, 2, 0f, 0.036f));
        ELEMENTS.add(new El("effects", "Effets de potion", "Info", false, 2, 0f, 0.06f));
        ELEMENTS.add(new El("keystrokes", "KeyStrokes", "Mecanique", false, 6, 0f, -0.10f));
        ELEMENTS.add(new El("cps", "CPS (clics/s)", "Mecanique", false, 6, 0f, -0.02f));
        ELEMENTS.add(new El("armor", "Statut d'armure", "Combat", false, 8, 0f, -0.02f));
        ELEMENTS.add(new El("speed", "Vitesse", "Info", false, 0, 0f, 0.054f));
        ELEMENTS.add(new El("xp", "Niveau XP", "Info", false, 2, 0f, 0.078f));
        ELEMENTS.add(new El("itemcount", "Compteur d'objet", "Combat", false, 8, 0f, -0.06f));
        ELEMENTS.add(new El("zoom", "Zoom (touche)", "Mecanique", false, 1, 0f, 0.05f));
        ELEMENTS.add(new El("togglesprint", "Toggle Sprint/Sneak", "Mecanique", false, 6, 0f, -0.16f));
        ELEMENTS.add(new El("scoreboard", "Scoreboard (deplacable)", "Info", false, 5, 0f, 0f));
        ELEMENTS.add(new El("chat", "Tchat (reglages)", "Info", true, 6, 0f, 0f));
        ELEMENTS.add(new El("crosshair", "Crosshair personnalise", "Combat", false, 4, 0f, 0f));
        ELEMENTS.add(new El("waypoints", "Waypoints (HUD)", "Info", false, 0, 0f, 0.09f));
    }

    private static final long SESSION_START = System.currentTimeMillis();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWHudManager() {}

    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    public static void onClick() { synchronized (CLICKS) { CLICKS.addLast(System.currentTimeMillis()); while (CLICKS.size() > 60) CLICKS.pollFirst(); } }
    private static int cps() { long now = System.currentTimeMillis(); synchronized (CLICKS) { while (!CLICKS.isEmpty() && now - CLICKS.peekFirst() > 1000) CLICKS.pollFirst(); return CLICKS.size(); } }

    public static El byId(String id) { for (El e : ELEMENTS) if (e.id.equals(id)) return e; return null; }
    public static void resetPos(El e) { e.anchor = e.danchor; e.ox = e.dox; e.oy = e.doy; e.scale = e.dscale; e.locked = false; }
    public static void resetAll() { for (El e : ELEMENTS) { e.enabled = e.denabled; e.anchor = e.danchor; e.ox = e.dox; e.oy = e.doy; e.scale = e.dscale; e.locked = false; } }
    public static String anchorName(int a) {
        switch (a) {
            case 1: return "haut-centre"; case 2: return "haut-droite";
            case 3: return "centre-gauche"; case 4: return "centre"; case 5: return "centre-droite";
            case 6: return "bas-gauche"; case 7: return "bas-centre"; case 8: return "bas-droite";
            default: return "haut-gauche";
        }
    }
    private static int anchorFromName(String s) { for (int i = 0; i < ANCHORS.length; i++) if (ANCHORS[i].equals(s)) return i; return 0; }

    public static int baseX(int anchor, int sw, int w) { int c = anchor % 3; if (c == 0) return SAFE; if (c == 1) return (sw - w) / 2; return sw - w - SAFE; }
    public static int baseY(int anchor, int sh, int h) { int r = anchor / 3; if (r == 0) return SAFE; if (r == 1) return (sh - h) / 2; return sh - h - SAFE; }
    public static int actualX(El e, int sw, int w) { int fx = Math.round(baseX(e.anchor, sw, w) + e.ox * sw); int lo = SAFE, hi = Math.max(lo, sw - w - SAFE); return Math.max(lo, Math.min(hi, fx)); }
    public static int actualY(El e, int sh, int h) { int fy = Math.round(baseY(e.anchor, sh, h) + e.oy * sh); int lo = SAFE, hi = Math.max(lo, sh - h - SAFE); return Math.max(lo, Math.min(hi, fy)); }
    public static int scaledW(MinecraftClient mc, El e) { return Math.max(1, Math.round(width(mc, e) * e.scale)); }
    public static int scaledH(El e) { return Math.max(1, Math.round(height(e) * e.scale)); }
    public static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }

    private static Path file() {
        String a = HWProfiles.active;
        String fn = (a == null || a.equals("Par defaut")) ? "heroworld_hud.json" : "heroworld_hud_" + HWProfiles.slug(a) + ".json";
        return FabricLoader.getInstance().getConfigDir().resolve(fn);
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) { LOG.info("[HUD] pas de config, valeurs par defaut ({} widgets)", ELEMENTS.size()); return; }
            JsonObject root = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("widgets")) return;
            JsonArray arr = root.getAsJsonArray("widgets");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                El e = byId(o.get("id").getAsString());
                if (e == null) continue;
                if (o.has("anchor")) e.anchor = anchorFromName(o.get("anchor").getAsString());
                if (o.has("offset")) { JsonObject off = o.getAsJsonObject("offset"); e.ox = (float) off.get("x").getAsDouble(); e.oy = (float) off.get("y").getAsDouble(); }
                if (o.has("scale")) e.scale = clampScale((float) o.get("scale").getAsDouble());
                if (o.has("visible")) e.enabled = o.get("visible").getAsBoolean();
                if (o.has("locked")) e.locked = o.get("locked").getAsBoolean();
                if (o.has("settings")) {
                    JsonObject st = o.getAsJsonObject("settings");
                    if (st.has("color")) e.color = st.get("color").getAsInt();
                    if (st.has("bg")) e.bgMode = st.get("bg").getAsInt();
                    if (st.has("opt")) e.opt = st.get("opt").getAsInt();
                }
            }
            LOG.info("[HUD] profil charge ({} widgets)", ELEMENTS.size());
        } catch (Exception ex) { LOG.warn("[HUD] echec chargement, valeurs par defaut", ex); }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("schemaVersion", 2);
            root.addProperty("profileId", "default");
            root.add("serverMatch", com.google.gson.JsonNull.INSTANCE);
            JsonArray arr = new JsonArray();
            int z = 0;
            for (El e : ELEMENTS) {
                JsonObject o = new JsonObject();
                o.addProperty("id", e.id);
                o.addProperty("anchor", ANCHORS[e.anchor]);
                JsonObject off = new JsonObject(); off.addProperty("x", e.ox); off.addProperty("y", e.oy); o.add("offset", off);
                o.addProperty("scale", e.scale);
                o.add("width", com.google.gson.JsonNull.INSTANCE);
                o.add("height", com.google.gson.JsonNull.INSTANCE);
                o.addProperty("zIndex", z++);
                o.addProperty("visible", e.enabled);
                o.addProperty("locked", e.locked);
                JsonObject st = new JsonObject();
                if (e.color != 0) st.addProperty("color", e.color);
                if (e.bgMode != 0) st.addProperty("bg", e.bgMode);
                if (e.opt != 0) st.addProperty("opt", e.opt);
                o.add("settings", st);
                arr.add(o);
            }
            root.add("widgets", arr);
            Path f = file(), tmp = f.resolveSibling(f.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception atomicFail) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception ex) { LOG.warn("[HUD] echec sauvegarde", ex); }
    }

    public static void renderAll(DrawContext ctx, MinecraftClient mc) {
        if (mc == null || mc.player == null || ctx == null) return;
        if (mc.options != null && mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HWHudEditScreen) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        for (El e : ELEMENTS) {
            if (!e.enabled) continue;
            try {
                int w = scaledW(mc, e), h = scaledH(e);
                int fx = actualX(e, sw, w), fy = actualY(e, sh, h);
                MatrixStack ms = ctx.getMatrices();
                ms.push();
                ms.translate((double) fx, (double) fy, 0.0);
                ms.scale(e.scale, e.scale, 1.0f);
                renderAt(ctx, mc, e, 0, 0);
                ms.pop();
            } catch (Throwable t) { if (logged.add(e.id)) LOG.warn("[HUD] erreur de rendu du widget '{}'", e.id, t); }
        }
    }

    public static int width(MinecraftClient mc, El e) {
        try {
            switch (e.id) {
                case "keystrokes": return 40;
                case "armor": return 18;
                case "effects": return 118;
                case "direction": return 110;
                case "scoreboard": return 95;
                case "crosshair": return 12;
                case "waypoints": return 110;
                case "chat": return 40;
                default: return mc.textRenderer.getWidth(text(mc, e)) + 4;
            }
        } catch (Throwable t) { return 40; }
    }
    public static int height(El e) {
        switch (e.id) {
            case "keystrokes": return 40; case "armor": return 72; case "effects": return 72; case "direction": return 13;
            case "scoreboard": return 85; case "crosshair": return 12; case "waypoints": return 33; case "chat": return 10;
            default: return 10;
        }
    }

    static String text(MinecraftClient mc, El e) {
        switch (e.id) {
            case "fps": return "§e" + mc.getCurrentFps() + " §7FPS";
            case "ping": {
                int p = 0;
                net.minecraft.client.network.ClientPlayNetworkHandler nh = mc.getNetworkHandler();
                if (nh != null) { net.minecraft.client.network.PlayerListEntry pe = nh.getPlayerListEntry(mc.player.getUuid()); if (pe != null) p = pe.getLatency(); }
                return "§7Ping §f" + p + "ms";
            }
            case "cps": return "§bCPS §f" + cps();
            case "coords": return "§7XYZ §f" + fl(mc.player.getX()) + " " + fl(mc.player.getY()) + " " + fl(mc.player.getZ());
            case "time": { String t = java.time.LocalTime.now().toString(); return "§f" + (t.length() >= 5 ? t.substring(0, 5) : t); }
            case "day": return "§7Jour §f" + (mc.world != null ? (mc.world.getTimeOfDay() / 24000L) : 0);
            case "session": { long s = (System.currentTimeMillis() - SESSION_START) / 1000; return String.format("§7Session §f%02d:%02d", s / 60, s % 60); }
            case "speed": return "§7Vitesse §f" + String.format("%.1f", mc.player.getVelocity().horizontalLength() * 20) + " b/s";
            case "xp": return "§7Niveau §f" + mc.player.experienceLevel;
            case "itemcount": { net.minecraft.item.ItemStack h = mc.player.getMainHandStack(); return "§7Objet §f" + (h != null ? h.getCount() : 0); }
            case "zoom": return HWZoom.active ? ("§bZoom §fx" + String.format("%.1f", HWZoom.clampFactor(HWClientConfig.zoomFactor)))
                : (editorPreview ? ("§bZoom §fx" + String.format("%.1f", HWZoom.clampFactor(HWClientConfig.zoomFactor)) + " §8(apercu)") : "");
            case "togglesprint": {
                boolean sp = false, sn = false;
                try { sp = mc.options.getSprintToggled().getValue(); sn = mc.options.getSneakToggled().getValue(); } catch (Throwable ignored) {}
                String run = (mc.player != null && mc.player.isSprinting()) ? "§a" : "§f";
                return "§7Sprint " + run + (sp ? "toggle" : "maintenu") + " §8| §7Sneak §f" + (sn ? "toggle" : "maintenu");
            }
            default: return e.name;
        }
    }

    public static void renderAt(DrawContext ctx, MinecraftClient mc, El e, int ax, int ay) {
        TextRenderer tr = mc.textRenderer;
        switch (e.id) {
            case "keystrokes": drawKeystrokes(ctx, mc, ax, ay); break;
            case "armor": drawArmor(ctx, mc, ax, ay); break;
            case "effects": drawEffects(ctx, mc, e, ax, ay); break;
            case "direction": drawCompass(ctx, mc, ax, ay); break;
            case "crosshair": drawCrosshair(ctx, e, ax, ay); break;
            case "waypoints": drawWaypoints(ctx, mc, ax, ay); break;
            case "chat": break; // le tchat vanilla reste en place (reglages via le menu)
            case "scoreboard": {
                if (editorPreview) {
                    ctx.fill(ax, ay, ax + 95, ay + 85, 0x55000000);
                    ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("§eSCOREBOARD"), ax + 47, ay + 6, 0xFFE8C56A);
                    ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("§7(rendu vanilla"), ax + 47, ay + 34, 0xFFA9AFBA);
                    ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("§7deplace ici)"), ax + 47, ay + 46, 0xFFA9AFBA);
                }
                break; // en jeu : rendu vanilla repositionne par ScoreboardMixin
            }
            default: {
                String s = text(mc, e);
                if (s.isEmpty()) return; // ex. zoom inactif
                if (e.color != 0) s = s.replaceAll("§.", ""); // couleur perso : on retire les codes § qui l'ecraseraient
                int w = tr.getWidth(s);
                boolean bg;
                if (HWClientConfig.highContrast) bg = true;
                else if (e.bgMode == 1) bg = true;
                else if (e.bgMode == 2) bg = false;
                else bg = HWClientConfig.hudBackground;
                if (bg) ctx.fill(ax - 2, ay - 1, ax + w + 2, ay + 9, 0x66000000);
                ctx.drawTextWithShadow(tr, Text.literal(s), ax, ay, e.color != 0 ? e.color : 0xFFFFFFFF);
            }
        }
    }

    private static long fl(double v) { return (long) Math.floor(v); }

    private static final String[] COMPASS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
    private static float wrap180(float a) { a %= 360f; if (a > 180f) a -= 360f; if (a < -180f) a += 360f; return a; }
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

    private static void drawEffects(DrawContext ctx, MinecraftClient mc, El e, int x, int y) {
        int row = 0;
        for (StatusEffectInstance eff : mc.player.getStatusEffects()) {
            if (row >= 4) break;
            int yy = y + row * 18;
            int secs = eff.getDuration() / 20;
            String name = "";
            try { name = eff.getEffectType().value().getName().getString(); } catch (Throwable ignored) {}
            String s = "§f" + name + (eff.getAmplifier() > 0 ? " " + (eff.getAmplifier() + 1) : "")
                + " §7" + String.format("%d:%02d", secs / 60, secs % 60);
            if (e.color != 0) s = s.replaceAll("§.", "");
            int w = mc.textRenderer.getWidth(s);
            boolean bg;
            if (HWClientConfig.highContrast) bg = true;
            else if (e.bgMode == 1) bg = true;
            else if (e.bgMode == 2) bg = false;
            else bg = HWClientConfig.hudBackground;
            if (bg) ctx.fill(x, yy, x + 20 + w + 6, yy + 18, 0x66000000);
            boolean icon = false;
            try {
                net.minecraft.client.texture.Sprite sp = mc.getStatusEffectSpriteManager().getSprite(eff.getEffectType());
                ctx.drawSprite(x + 1, yy + 1, 0, 16, 16, sp);
                icon = true;
            } catch (Throwable ignored) {}
            if (!icon) {
                int c = 0xFFB07CE8;
                try { c = 0xFF000000 | eff.getEffectType().value().getColor(); } catch (Throwable ignored) {}
                ctx.fill(x + 3, yy + 3, x + 15, yy + 15, c);
            }
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(s), x + 21, yy + 5, e.color != 0 ? e.color : 0xFFFFFFFF);
            row++;
        }
    }

    /** Crosshair personnalise : 0 croix, 1 point, 2 cercle. */
    private static void drawCrosshair(DrawContext ctx, El e, int x, int y) {
        int c = e.color != 0 ? e.color : 0xFFFFFFFF;
        int cx = x + 6, cy = y + 6;
        switch (e.opt % 3) {
            case 1: // point
                ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, c);
                break;
            case 2: // cercle
                ctx.fill(cx - 2, cy - 6, cx + 3, cy - 5, c); ctx.fill(cx - 2, cy + 5, cx + 3, cy + 6, c);
                ctx.fill(cx - 6, cy - 2, cx - 5, cy + 3, c); ctx.fill(cx + 5, cy - 2, cx + 6, cy + 3, c);
                ctx.fill(cx - 5, cy - 4, cx - 3, cy - 3, c); ctx.fill(cx + 3, cy - 4, cx + 5, cy - 3, c);
                ctx.fill(cx - 5, cy + 3, cx - 3, cy + 4, c); ctx.fill(cx + 3, cy + 3, cx + 5, cy + 4, c);
                ctx.fill(cx, cy, cx + 1, cy + 1, c);
                break;
            default: // croix
                ctx.fill(cx, cy - 6, cx + 1, cy - 1, c); ctx.fill(cx, cy + 2, cx + 1, cy + 7, c);
                ctx.fill(cx - 6, cy, cx - 1, cy + 1, c); ctx.fill(cx + 2, cy, cx + 7, cy + 1, c);
        }
    }

    /** Les 3 waypoints visibles les plus proches (dimension courante). */
    private static void drawWaypoints(DrawContext ctx, MinecraftClient mc, int x, int y) {
        java.util.List<HWWaypoints.WP> list = HWWaypoints.nearest(mc, 3);
        if (list.isEmpty()) {
            if (editorPreview) ctx.drawTextWithShadow(mc.textRenderer, Text.literal("§7◆ (aucun waypoint)"), x, y, 0xFFA9AFBA);
            return;
        }
        int row = 0;
        for (HWWaypoints.WP w : list) {
            String s = "◆ " + w.name + "  " + HWWaypoints.dist(mc, w) + "m " + HWWaypoints.dirTo(mc, w);
            int tw = mc.textRenderer.getWidth(s);
            int yy = y + row * 11;
            if (HWClientConfig.hudBackground || HWClientConfig.highContrast) ctx.fill(x - 2, yy - 1, x + tw + 2, yy + 9, 0x66000000);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(s), x, yy, 0xFF000000 | (w.color & 0xFFFFFF));
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
        ItemStack[] arm = new ItemStack[4];
        int i = 0;
        for (ItemStack st : mc.player.getArmorItems()) { if (i < 4) arm[i] = st; i++; }
        // colonne gauche : casque, plastron, jambieres, bottes (haut -> bas)
        armorSlot(ctx, x, y, arm[3]);
        armorSlot(ctx, x, y + 18, arm[2]);
        armorSlot(ctx, x, y + 36, arm[1]);
        armorSlot(ctx, x, y + 54, arm[0]);
        // colonne droite : main principale + main secondaire
        armorSlot(ctx, x + 18, y, mc.player.getMainHandStack());
        armorSlot(ctx, x + 18, y + 18, mc.player.getOffHandStack());
    }
    private static void armorSlot(DrawContext ctx, int x, int y, ItemStack st) {
        ctx.fill(x - 1, y - 1, x + 17, y + 17, 0x55000000);
        if (st == null || st.isEmpty()) return;
        ctx.drawItem(st, x, y);
        if (st.isDamageable() && st.getDamage() > 0) {
            int max = st.getMaxDamage();
            float fr = Math.max(0f, 1f - (float) st.getDamage() / max);
            int bw = Math.max(0, (int) (14 * fr));
            int col = fr > 0.5f ? 0xFF48C78E : (fr > 0.2f ? 0xFFE7B84B : 0xFFE45B68);
            ctx.fill(x + 1, y + 15, x + 15, y + 16, 0xFF333333);
            ctx.fill(x + 1, y + 15, x + 1 + bw, y + 16, col);
        }
    }
}
