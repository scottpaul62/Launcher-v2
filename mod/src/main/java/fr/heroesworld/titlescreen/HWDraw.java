package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;

/** Primitives de dessin premium : rectangles a coins arrondis, panneaux, halo au survol. */
public final class HWDraw {
    private HWDraw() {}

    /** Rectangle plein a coins arrondis (rayon r), dessine ligne par ligne (fill = rectangles). */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int d = r - i;
            int inset = r - (int) Math.round(Math.sqrt((double) r * r - (double) d * d));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /**
     * Panneau arrondi. Depuis la 1.1.7, rendu via des TEXTURES generees (HWTex, 9-slice) pour un
     * look epure et lisse partout ; le style est deduit du remplissage demande :
     * translucide -> verre ; opaque sombre -> panneau ; opaque plus clair -> carte.
     * Repli automatique sur l'ancien dessin en code si la texture manque.
     */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, int r, int fill, int border) {
        int a = fill >>> 24;
        int lum = ((fill >> 16) & 0xFF) + ((fill >> 8) & 0xFF) + (fill & 0xFF);
        int style = a >= 0xEE ? (lum >= 75 ? HWTex.RAISED : HWTex.SOLID) : HWTex.GLASS;
        if (HWTex.draw9(ctx, style, x, y, w, h, Math.max(4, r + 2))) return;
        panelRaw(ctx, x, y, w, h, r, fill, border);
    }

    /** Ancien panneau dessine en code (repli). */
    public static void panelRaw(DrawContext ctx, int x, int y, int w, int h, int r, int fill, int border) {
        roundRect(ctx, x, y, w, h, r, border);
        roundRect(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Halo doux derriere un element (effet survol). a dans [0..1]. */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int r, int rgb, float a) {
        int alpha = (int) (Math.max(0f, Math.min(1f, a)) * 70);
        if (alpha <= 2) return;
        roundRect(ctx, x - 2, y - 2, w + 4, h + 4, r + 2, (alpha << 24) | (rgb & 0xFFFFFF));
    }
}
