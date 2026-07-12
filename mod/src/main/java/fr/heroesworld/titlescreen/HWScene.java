package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import java.util.Random;

/** Dessine la scène animée de l'Olympe (100% code, aucune texture). */
public final class HWScene {
    private HWScene() {}

    public static void draw(DrawContext ctx, int w, int h) {
        HWTheme.Theme th = HWTheme.current();
        float t = (System.currentTimeMillis() % 600000L) / 1000f;
        float drift = (float) Math.sin(t * 0.06) * 10f;

        // Ciel
        ctx.fillGradient(0, 0, w, h, th.skyTop, th.skyBottom);
        // Nébuleuse (bande translucide qui s'estompe)
        ctx.fillGradient(0, (int) (h * 0.10), w, (int) (h * 0.52), th.nebula, 0x00000000);
        // Halo lunaire derrière le logo
        int mgx = w / 2, mgy = (int) (h * 0.20);
        ctx.fillGradient(mgx - 160, mgy - 70, mgx + 160, mgy + 70, (0x22 << 24) | (th.accent & 0xFFFFFF), 0x00000000);
        // Étoiles scintillantes
        Random rnd = new Random(4242L);
        for (int i = 0; i < 130; i++) {
            int sx = (int) (rnd.nextFloat() * w);
            int sy = (int) (rnd.nextFloat() * h * 0.72f);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.5 + i * 1.3);
            int a = (int) (35 + 150 * tw);
            ctx.fill(sx, sy, sx + 1, sy + 1, (a << 24) | (th.star & 0xFFFFFF));
        }

        int horizon = (int) (h * 0.74);
        drawColumns(ctx, w, h, horizon, drift, th);
        // Sol en pierre
        ctx.fillGradient(0, horizon, w, h, 0xFF0B0B10, 0xFF040406);
        ctx.fill(0, horizon, w, horizon + 1, 0x55000000);
        // Braseros
        drawBrazier(ctx, (int) (w * 0.24 + drift * 0.5f), horizon, t, th);
        drawBrazier(ctx, (int) (w * 0.76 + drift * 0.5f), horizon, t, th);

        // Éclairs discrets (thèmes orageux)
        if (th.lightning) {
            float phase = t % 13f;
            if (phase < 0.16f) { int a = (int) (110 * (1 - phase / 0.16f)); ctx.fill(0, 0, w, h, (a << 24)); }
        }
        // Vignette
        ctx.fillGradient(0, 0, w, 80, 0x88000000, 0x00000000);
        ctx.fillGradient(0, h - 70, w, h, 0x00000000, 0x66000000);
    }

    private static void drawColumns(DrawContext ctx, int w, int h, int horizon, float drift, HWTheme.Theme th) {
        int count = 6;
        int colH = (int) (h * 0.30);
        int top = horizon - colH;
        int cw = Math.max(10, w / 26);
        for (int i = 0; i < count; i++) {
            float f = (i + 0.5f) / count;
            int cxp = (int) (f * w + drift);
            int x = cxp - cw / 2;
            ctx.fillGradient(x, top + 8, x + cw, horizon, 0xFF20202A, 0xFF0D0D13);
            ctx.fill(x - 3, top, x + cw + 3, top + 8, 0xFF262633);      // chapiteau
            ctx.fill(x - 3, horizon - 4, x + cw + 3, horizon, 0xFF181820); // base
            ctx.fill(x, top + 8, x + 2, horizon, (0x28 << 24) | (th.glow & 0xFFFFFF)); // lueur chaude
        }
    }

    private static void drawBrazier(DrawContext ctx, int bx, int groundY, float t, HWTheme.Theme th) {
        float fl = 0.65f + 0.35f * (float) Math.sin(t * 9 + bx);
        float fl2 = 0.5f + 0.5f * (float) Math.sin(t * 13 + bx * 0.7f);
        // pied + vasque
        ctx.fill(bx - 3, groundY - 26, bx + 3, groundY, 0xFF15130F);
        ctx.fill(bx - 13, groundY - 30, bx + 13, groundY - 24, 0xFF2A2118);
        // halo lumineux pulsant
        int ga = (int) (60 * fl);
        ctx.fillGradient(bx - 42, groundY - 92, bx + 42, groundY - 12, (ga << 24) | (th.glow & 0xFFFFFF), 0x00000000);
        // flammes
        for (int k = 0; k < 6; k++) {
            float ph = t * 10 + k * 1.7f;
            int fx = bx - 9 + k * 3;
            int fh = (int) (18 + 12 * Math.abs(Math.sin(ph)) + 6 * fl2);
            ctx.fillGradient(fx, groundY - 28 - fh, fx + 3, groundY - 28, th.fireCool, th.fireHot);
        }
        // cœur clair
        ctx.fill(bx - 3, groundY - 42, bx + 3, groundY - 30, (0xEE << 24) | (th.fireCool & 0xFFFFFF));
        // lumière au sol
        int la = (int) (42 * fl);
        ctx.fillGradient(bx - 46, groundY, bx + 46, groundY + 10, (la << 24) | (th.glow & 0xFFFFFF), 0x00000000);
    }
}
