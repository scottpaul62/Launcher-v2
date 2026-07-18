package fr.heroesworld.titlescreen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Fonds d'interface en IMAGES (generees en haute qualite au build, coins lisses, degrades doux)
 * dessines en 9-slice : les coins restent nets a toutes les tailles, le centre s'etire.
 * Remplace les rectangles dessines en code partout ou HWDraw.panel est appele.
 * Textures 96x96, coin de 24 px, dans assets/heroworld/textures/gui/ui/.
 */
public final class HWTex {
    public static final int GLASS = 0, GLASS_HOVER = 1, SOLID = 2, RAISED = 3, GOLD = 4, GOLD_HOVER = 5;
    private static final String[] NAMES = { "glass", "glass_hover", "solid", "raised", "gold", "gold_hover" };
    private static final Identifier[] IDS = new Identifier[NAMES.length];
    private static final boolean[] FAILED = new boolean[NAMES.length];
    private static final int T = 96, TC = 24;
    private HWTex() {}

    private static Identifier tex(int style) {
        if (style < 0 || style >= NAMES.length || FAILED[style]) return null;
        if (IDS[style] != null) return IDS[style];
        try (InputStream in = HWTex.class.getResourceAsStream("/assets/heroworld/textures/gui/ui/" + NAMES[style] + ".png")) {
            if (in == null) { FAILED[style] = true; return null; }
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture t = new NativeImageBackedTexture(img);
            t.setFilter(true, false);
            Identifier id = new Identifier("heroworld", "hw_ui_" + NAMES[style]);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, t);
            IDS[style] = id;
            return id;
        } catch (Throwable t) { FAILED[style] = true; return null; }
    }

    /** Dessine le style en 9-slice. c = taille de coin en px GUI. Retourne false si texture indisponible. */
    public static boolean draw9(DrawContext ctx, int style, int x, int y, int w, int h, int c) {
        Identifier id = tex(style);
        if (id == null || w <= 1 || h <= 1) return id != null;
        c = Math.max(3, Math.min(c, Math.min(w, h) / 2));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // coins
        ctx.drawTexture(id, x, y, c, c, 0f, 0f, TC, TC, T, T);
        ctx.drawTexture(id, x + w - c, y, c, c, (float) (T - TC), 0f, TC, TC, T, T);
        ctx.drawTexture(id, x, y + h - c, c, c, 0f, (float) (T - TC), TC, TC, T, T);
        ctx.drawTexture(id, x + w - c, y + h - c, c, c, (float) (T - TC), (float) (T - TC), TC, TC, T, T);
        // bords
        if (w > 2 * c) {
            ctx.drawTexture(id, x + c, y, w - 2 * c, c, TC, 0f, T - 2 * TC, TC, T, T);
            ctx.drawTexture(id, x + c, y + h - c, w - 2 * c, c, TC, (float) (T - TC), T - 2 * TC, TC, T, T);
        }
        if (h > 2 * c) {
            ctx.drawTexture(id, x, y + c, c, h - 2 * c, 0f, TC, TC, T - 2 * TC, T, T);
            ctx.drawTexture(id, x + w - c, y + c, c, h - 2 * c, (float) (T - TC), TC, TC, T - 2 * TC, T, T);
        }
        // centre
        if (w > 2 * c && h > 2 * c)
            ctx.drawTexture(id, x + c, y + c, w - 2 * c, h - 2 * c, TC, TC, T - 2 * TC, T - 2 * TC, T, T);
        RenderSystem.disableBlend();
        return true;
    }

    // ==================== ASSETS PREMIUM (PNG fournis, dossier ui/hw/) ====================
    // Dessines sans deformer les ornements : H3 = capsules etirees au centre seulement,
    // 9-slice pour les cadres simples, FIT = echelle uniforme (cadres ornementes, dock).
    private static final java.util.Map<String, Object[]> NAMED = new java.util.HashMap<>(); // name -> {Identifier, w, h}
    private static final java.util.Set<String> NAMED_FAILED = new java.util.HashSet<>();

    private static Object[] named(String name) {
        if (NAMED_FAILED.contains(name)) return null;
        Object[] v = NAMED.get(name);
        if (v != null) return v;
        try (InputStream in = HWTex.class.getResourceAsStream("/assets/heroworld/textures/gui/ui/hw/" + name + ".png")) {
            if (in == null) { NAMED_FAILED.add(name); return null; }
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture t = new NativeImageBackedTexture(img);
            t.setFilter(true, false);
            Identifier id = new Identifier("heroworld", "hw_asset_" + name);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, t);
            v = new Object[]{ id, img.getWidth(), img.getHeight() };
            NAMED.put(name, v);
            return v;
        } catch (Throwable t) { NAMED_FAILED.add(name); return null; }
    }

    public static boolean has(String name) { return named(name) != null; }
    public static int[] dims(String name) {
        Object[] v = named(name);
        return v == null ? null : new int[]{ (Integer) v[1], (Integer) v[2] };
    }

    /** Capsule H3 : bouts non deformes (largeur = h * capSrc/texH), centre etire. capSrc en px source. */
    public static boolean drawH3(DrawContext ctx, String name, int x, int y, int w, int h, int capSrc) {
        Object[] v = named(name);
        if (v == null) return false;
        Identifier id = (Identifier) v[0]; int tw = (Integer) v[1], th = (Integer) v[2];
        if (capSrc <= 0) capSrc = th / 2;
        int cap = Math.max(2, Math.min(w / 2, Math.round(h * (float) capSrc / th)));
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        ctx.drawTexture(id, x, y, cap, h, 0f, 0f, capSrc, th, tw, th);
        ctx.drawTexture(id, x + w - cap, y, cap, h, (float) (tw - capSrc), 0f, capSrc, th, tw, th);
        if (w > 2 * cap)
            ctx.drawTexture(id, x + cap, y, w - 2 * cap, h, capSrc, 0f, tw - 2 * capSrc, th, tw, th);
        RenderSystem.disableBlend();
        return true;
    }

    /** 9-slice d'un asset nomme (bordure unie) : cornerSrc en px source, echelle proportionnelle a h. */
    public static boolean draw9n(DrawContext ctx, String name, int x, int y, int w, int h, int cornerSrc) {
        Object[] v = named(name);
        if (v == null) return false;
        Identifier id = (Identifier) v[0]; int tw = (Integer) v[1], th = (Integer) v[2];
        int c = Math.max(4, Math.min(Math.min(w, h) / 2, Math.round(h * (float) cornerSrc / th)));
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        ctx.drawTexture(id, x, y, c, c, 0f, 0f, cornerSrc, cornerSrc, tw, th);
        ctx.drawTexture(id, x + w - c, y, c, c, (float) (tw - cornerSrc), 0f, cornerSrc, cornerSrc, tw, th);
        ctx.drawTexture(id, x, y + h - c, c, c, 0f, (float) (th - cornerSrc), cornerSrc, cornerSrc, tw, th);
        ctx.drawTexture(id, x + w - c, y + h - c, c, c, (float) (tw - cornerSrc), (float) (th - cornerSrc), cornerSrc, cornerSrc, tw, th);
        if (w > 2 * c) {
            ctx.drawTexture(id, x + c, y, w - 2 * c, c, cornerSrc, 0f, tw - 2 * cornerSrc, cornerSrc, tw, th);
            ctx.drawTexture(id, x + c, y + h - c, w - 2 * c, c, cornerSrc, (float) (th - cornerSrc), tw - 2 * cornerSrc, cornerSrc, tw, th);
        }
        if (h > 2 * c) {
            ctx.drawTexture(id, x, y + c, c, h - 2 * c, 0f, cornerSrc, cornerSrc, th - 2 * cornerSrc, tw, th);
            ctx.drawTexture(id, x + w - c, y + c, c, h - 2 * c, (float) (tw - cornerSrc), cornerSrc, cornerSrc, th - 2 * cornerSrc, tw, th);
        }
        if (w > 2 * c && h > 2 * c)
            ctx.drawTexture(id, x + c, y + c, w - 2 * c, h - 2 * c, cornerSrc, cornerSrc, tw - 2 * cornerSrc, th - 2 * cornerSrc, tw, th);
        RenderSystem.disableBlend();
        return true;
    }

    /** Echelle UNIFORME (aucune deformation) : hauteur cible, centre en (cx, cy). Retourne {x,y,w,h} dessine, ou null. */
    public static int[] drawFit(DrawContext ctx, String name, int cx, int cy, int targetH) {
        Object[] v = named(name);
        if (v == null) return null;
        Identifier id = (Identifier) v[0]; int tw = (Integer) v[1], th = (Integer) v[2];
        int h = targetH, w = Math.round(h * (float) tw / th);
        int x = cx - w / 2, y = cy - h / 2;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        ctx.drawTexture(id, x, y, w, h, 0f, 0f, tw, th, tw, th);
        RenderSystem.disableBlend();
        return new int[]{ x, y, w, h };
    }

    /** Bouton avec fondu de survol : base + variante hover en transparence progressive. */
    public static boolean button(DrawContext ctx, int base, int hoverStyle, float hover, int x, int y, int w, int h, int c) {
        if (!draw9(ctx, base, x, y, w, h, c)) return false;
        if (hover > 0.03f) {
            ctx.setShaderColor(1f, 1f, 1f, Math.min(1f, hover));
            draw9(ctx, hoverStyle, x, y, w, h, c);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
        }
        return true;
    }
}
