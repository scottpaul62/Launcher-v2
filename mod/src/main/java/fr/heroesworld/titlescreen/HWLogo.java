package fr.heroesworld.titlescreen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Logo HERO WORLD dessiné en code : panneau, aura pulsante, texte net doré. */
public final class HWLogo {
    private HWLogo() {}

    public static void draw(DrawContext ctx, TextRenderer tr, int width, int topY) {
        int cx = width / 2;
        int pw = Math.min(width - 100, 560);
        int ph = 100;
        int px = cx - pw / 2;
        int py = topY;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 600.0);

        // Aura dorée pulsante
        int ga = (int) (26 + 34 * pulse);
        ctx.fillGradient(px - 14, py - 8, px + pw + 14, py + ph + 8, (ga << 24) | 0xE8C56A, 0x00000000);
        // Panneau (sépare le logo du décor)
        ctx.fillGradient(px, py, px + pw, py + ph, 0xC0060810, 0xC00A0A14);
        frame(ctx, px, py, pw, ph, 0x66E8C56A);
        ctx.fill(px + 16, py + 14, px + pw - 16, py + 15, 0x33E8C56A);

        // Titre net (double passe = relief)
        drawScaled(ctx, tr, "HEROES-WORLD", cx, py + 28, 3.0f, 0xFF3A2E10);
        drawScaled(ctx, tr, "HEROES-WORLD", cx, py + 26, 3.0f, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(tr, net.minecraft.text.Text.literal("§7Survie   ·   Mythologie   ·   Progression"),
            cx, py + 70, 0xFFCFC9DA);
    }

    private static void frame(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    private static void drawScaled(DrawContext ctx, TextRenderer tr, String text, int centerX, int y, float scale, int color) {
        int w = tr.getWidth(text);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX - (w * scale) / 2f, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }
}
