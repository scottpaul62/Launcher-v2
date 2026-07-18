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
