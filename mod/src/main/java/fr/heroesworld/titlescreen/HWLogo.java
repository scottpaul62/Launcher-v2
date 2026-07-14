package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;

/** Logo anime HERO WORLD : planete Jupiter a bandes + meteorites en orbite + etoiles. Dessine en code. */
public final class HWLogo {
    private HWLogo() {}

    private static final int[] BANDS = {
        0xFFE3CBA0, 0xFFC89A66, 0xFFEAD6B0, 0xFFB07C4E,
        0xFFDCC298, 0xFFC89A66, 0xFFE3CBA0, 0xFFB98A58
    };

    public static void draw(DrawContext ctx, int cx, int cy, int size) {
        float t = System.currentTimeMillis() / 1000f;
        int pr = Math.max(6, (int) (size * 0.30f));

        drawSats(ctx, cx, cy, pr, t, false); // meteorites derriere

        // halo / atmosphere
        ctx.fillGradient(cx - pr - 4, cy - pr - 4, cx + pr + 4, cy + pr + 4, 0x2AFFE0A0, 0x00000000);

        // planete a bandes horizontales
        for (int dy = -pr; dy <= pr; dy++) {
            int hw = (int) Math.sqrt((double) pr * pr - dy * dy);
            int band = (int) (((dy + pr) / (float) (2 * pr)) * BANDS.length);
            if (band < 0) band = 0;
            if (band >= BANDS.length) band = BANDS.length - 1;
            ctx.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, BANDS[band]);
        }
        // grande tache rouge
        int spotX = cx + pr / 3, spotY = cy + pr / 4, sr = Math.max(2, pr / 4);
        for (int dy = -sr; dy <= sr; dy++)
            for (int dx = -sr; dx <= sr; dx++)
                if (dx * dx * 2 + dy * dy * 3 <= sr * sr * 2)
                    ctx.fill(spotX + dx, spotY + dy, spotX + dx + 1, spotY + dy + 1, 0xFFB4502E);
        // ombrage spherique (volume) : assombrit le bord droit
        for (int dy = -pr; dy <= pr; dy++) {
            int hw = (int) Math.sqrt((double) pr * pr - dy * dy);
            ctx.fillGradient(cx + hw / 3, cy + dy, cx + hw, cy + dy + 1, 0x00000000, 0x66000000);
        }

        drawSats(ctx, cx, cy, pr, t, true); // meteorites devant

        // etoiles scintillantes
        int R = size / 2;
        int[][] star = {{-R + 3, -R + 6}, {R - 8, -R + 3}, {-R + 5, R - 7}, {R / 4, -R + 7}, {-R / 2, R / 2}};
        for (int i = 0; i < star.length; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.2 + i * 1.7);
            int a = (int) (70 + 160 * tw);
            int col = (a << 24) | 0xFFFFFF;
            int sx = cx + star[i][0], sy = cy + star[i][1];
            int s = 1 + (int) (tw * 1.5f);
            ctx.fill(sx - s, sy, sx + s + 1, sy + 1, col);
            ctx.fill(sx, sy - s, sx + 1, sy + s + 1, col);
        }
    }

    private static void drawSats(DrawContext ctx, int cx, int cy, int pr, float t, boolean front) {
        float[][] orbits = {
            {pr + 8f, pr * 0.42f, 1.1f, 0f, 2f},
            {pr + 15f, pr * 0.62f, -0.8f, 2.1f, 2f},
            {pr + 5f, pr * 0.30f, 1.6f, 4.2f, 1f}
        };
        for (float[] o : orbits) {
            float rx = o[0], ry = o[1], sp = o[2], ph = o[3];
            int rad = (int) o[4];
            float a = t * sp + ph;
            if ((Math.sin(a) > 0) != front) continue;
            int sx = cx + (int) (rx * Math.cos(a));
            int sy = cy + (int) (ry * Math.sin(a));
            int col = front ? 0xFFCFC9BE : 0xAA8A857A;
            ctx.fill(sx - rad, sy - rad, sx + rad + 1, sy + rad + 1, col);
            ctx.fill(sx - rad - 2, sy, sx - rad, sy + 1, 0x55FFFFFF);
        }
    }
}
