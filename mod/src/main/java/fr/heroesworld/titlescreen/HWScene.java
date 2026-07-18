package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import java.util.Random;

/** Fond du menu : image "fond.png" si présente, sinon une scène propre dessinée en code. */
public final class HWScene {
    private HWScene() {}

    public static void draw(DrawContext ctx, int w, int h) {
        if (HWBg2.draw(ctx, w, h)) return; // fond « Cosmic Olympus » du kit (temple retire temporairement)
        if (HWBg.draw(ctx, w, h)) return;
        fallback(ctx, w, h);
    }

    private static void fallback(DrawContext ctx, int w, int h) {
        HWTheme.Theme th = HWTheme.current();
        float t = (System.currentTimeMillis() % 600000L) / 1000f;
        ctx.fillGradient(0, 0, w, h, th.skyTop, th.skyBottom);
        ctx.fillGradient(0, (int) (h * 0.08), w, (int) (h * 0.55), th.nebula, 0x00000000);
        Random rnd = new Random(4242L);
        for (int i = 0; i < 120; i++) {
            int sx = (int) (rnd.nextFloat() * w);
            int sy = (int) (rnd.nextFloat() * h * 0.85f);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.5 + i * 1.3);
            int a = (int) (35 + 150 * tw);
            ctx.fill(sx, sy, sx + 1, sy + 1, (a << 24) | (th.star & 0xFFFFFF));
        }
        ctx.fillGradient(0, 0, w, 80, 0x88000000, 0x00000000);
        ctx.fillGradient(0, h - 120, w, h, 0x00000000, 0x88000000);
    }
}
