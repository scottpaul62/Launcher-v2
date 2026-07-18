package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Effets vivants de l'ecran titre : braises, brume, etoiles filantes, eclairs periodiques. */
public final class HWFx {
    private HWFx() {}

    /** Conserve pour compatibilite (easter egg) : sans effet depuis le retrait des eclairs. */
    public static void storm() {}

    public static void draw(DrawContext ctx, int w, int h) {
        if (!HWClientConfig.titleFx) return;
        long t = System.currentTimeMillis();

        // braises dorees montantes depuis les braseros (bas gauche/droite)
        for (int i = 0; i < 24; i++) {
            long seed = i * 7919L;
            float ph = ((t + seed * 13L) % 6000L) / 6000f;
            boolean left = (i % 2 == 0);
            int bx = left ? (int) (w * 0.11f) : (int) (w * 0.89f);
            int x = bx + (int) (Math.sin(t * 0.001 + i) * (8 + (i % 12)));
            int y = (int) (h * 0.82f - ph * h * 0.45f);
            int a = (int) ((1f - ph) * 140);
            if (a > 8) ctx.fill(x, y, x + 2, y + 2, (a << 24) | 0xE8C56A);
        }

        // etoile filante (cycle 13 s, visible ~1,2 s)
        long cyc = t % 13000L;
        if (cyc < 1200) {
            float p = cyc / 1200f;
            int sx = (int) (w * (0.15f + 0.55f * p)), sy = (int) (h * (0.06f + 0.16f * p));
            for (int k = 0; k < 10; k++) {
                int a = (int) (200 * (1f - p) * (1f - k / 10f));
                if (a > 10) ctx.fill(sx - k * 3, sy - k, sx - k * 3 + 2, sy - k + 1, (a << 24) | 0xFFFFFF);
            }
        }

    }
}
