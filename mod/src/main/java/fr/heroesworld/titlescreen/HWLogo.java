package fr.heroesworld.titlescreen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Logo HEROES-WORLD net dessiné en code, léger (sur l'image), avec aura dorée. */
public final class HWLogo {
    private HWLogo() {}

    public static void draw(DrawContext ctx, TextRenderer tr, int width, int topY) {
        int cx = width / 2;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 650.0);

        // voile sombre doux derrière le titre (lisibilité sans panneau lourd)
        ctx.fillGradient(cx - 300, topY - 6, cx + 300, topY + 54, 0x00000000, 0x66000000);
        ctx.fillGradient(cx - 300, topY + 54, cx + 300, topY + 62, 0x66000000, 0x00000000);
        // aura dorée pulsante
        int ga = (int) (18 + 26 * pulse);
        ctx.fillGradient(cx - 260, topY + 4, cx + 260, topY + 52, (ga << 24) | 0xE8C56A, 0x00000000);

        // titre net (relief : ombre + or)
        drawScaled(ctx, tr, "HEROES-WORLD", cx, topY + 8, 2.7f, 0xFF221A08);
        drawScaled(ctx, tr, "HEROES-WORLD", cx, topY + 6, 2.7f, 0xFFF0CE72);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§7Survie   ·   Mythologie   ·   Progression"),
            cx, topY + 44, 0xFFDDD6E6);
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
