package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Effets vivants de l'ecran titre : braises, brume, etoiles filantes, eclairs periodiques. */
public final class HWFx {
    private static long stormUntil = 0;
    private HWFx() {}

    /** Pluie d'eclairs pendant 5 s (easter egg). */
    public static void storm() { stormUntil = System.currentTimeMillis() + 5000; }

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

        // brume tres lente (2 nappes translucides qui derivent)
        int mx = (int) ((t / 45) % (w * 2L)) - w;
        ctx.fill(Math.max(0, mx), (int) (h * 0.62f), Math.min(w, mx + w), (int) (h * 0.70f), 0x08FFFFFF);
        int mx2 = w - (int) ((t / 60) % (w * 2L));
        ctx.fill(Math.max(0, mx2 - w), (int) (h * 0.72f), Math.min(w, mx2), (int) (h * 0.79f), 0x08FFFFFF);

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

        // eclair periodique au-dessus du temple (flash + branche) ; tempete = cadence rapide
        boolean storm = t < stormUntil;
        long every = storm ? 650 : 9000;
        long fc = t % every;
        if (fc < 140) {
            int fa = (int) ((storm ? 70 : 48) * (1f - fc / 140f));
            if (fa > 4) ctx.fill(0, 0, w, h, (fa << 24) | 0xFFF3C0);
            long s2 = (t / every) * 31L;
            Random r = new Random(s2);
            int lx = (int) (w * (0.30f + r.nextFloat() * 0.40f)), ly = 0;
            int la = (int) (230 * (1f - fc / 140f));
            for (int seg = 0; seg < 10 && ly < h * 0.34f; seg++) {
                int nx = lx + r.nextInt(27) - 13, ny = ly + 8 + r.nextInt(15);
                int x1 = Math.min(lx, nx), x2 = Math.max(lx, nx) + 2;
                ctx.fill(x1, ly, x2, ny, (la << 24) | 0xFFEFB0);
                lx = nx; ly = ny;
            }
        }
    }
}
