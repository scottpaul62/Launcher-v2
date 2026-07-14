package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Bouton HERO WORLD premium : styles (principal / secondaire / dock), icônes dessinées, survol animé. */
public class HWButton extends ButtonWidget {
    public static final int PRIMARY = 0, SECONDARY = 1, DOCK = 2;

    private final int style;
    private final int icon; // 0=aucune, 1=amis, 2=thèmes, 3=perso, 4=options, 5=paramètres
    private float hover = 0f;

    public HWButton(int x, int y, int w, int h, Text msg, int style, int icon, PressAction onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
        this.icon = icon;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean over = isHovered() || isFocused();
        hover += ((over ? 1f : 0f) - hover) * 0.3f;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        if (style == PRIMARY) renderPrimary(ctx, tr, x, y, w, h);
        else if (style == DOCK) renderDock(ctx, tr, x, y, w, h);
        else renderSecondary(ctx, tr, x, y, w, h);
    }

    private void renderPrimary(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        if (hover > 0.02f) HWDraw.glow(ctx, x, y, w, h, 5, 0xE8C56A, hover);
        int fill = lerp(0xE01E1710, 0xE02C2114, hover);
        HWDraw.panel(ctx, x, y, w, h, 5, fill, lerp(0xFFCBA24E, 0xFFE8C56A, hover));
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, lerp(0xFFE9D4A0, 0xFFFFEFC6, hover));
    }

    private void renderSecondary(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        if (hover > 0.02f) HWDraw.glow(ctx, x, y, w, h, 4, 0x49BDF2, hover * 0.55f);
        int fill = lerp(0x9014121C, 0xC0221D2A, hover);
        HWDraw.panel(ctx, x, y, w, h, 4, fill, lerp(0x33FFFFFF, 0xB049BDF2, hover));
        boolean hasText = !getMessage().getString().isEmpty();
        if (icon > 0) HWIcons.draw(ctx, icon, hasText ? x + 15 : x + w / 2, y + h / 2, 16);
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, lerp(0xFFCFC9DA, 0xFFFFFFFF, hover));
    }

    private void renderDock(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        if (hover > 0.02f) rflat(ctx, x + 2, y + 2, w - 4, h - 4, lerp(0x00000000, 0x552A2233, hover));
        int iconCol = lerp(0xFFCBB98A, 0xFFF3D889, hover);
        if (!HWIcons.draw(ctx, icon, x + w / 2, y + 14, 20)) drawIcon(ctx, icon, x + w / 2, y + 13, iconCol);
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + h - 12, lerp(0xFFB9B3C4, 0xFFFFFFFF, hover));
        int uw = (int) ((w - 18) * hover);
        if (uw > 1) {
            ctx.fill(x + w / 2 - uw / 2, y + h - 3, x + w / 2 + uw / 2, y + h - 1, 0xFFE8C56A);
            ctx.fill(x + w / 2 - uw / 2, y + h - 1, x + w / 2 + uw / 2, y + h, 0x66E8C56A);
        }
    }

    private static void drawIcon(DrawContext ctx, int kind, int cx, int cy, int col) {
        switch (kind) {
            case 1 -> {
                ctx.fill(cx - 6, cy - 3, cx - 1, cy + 1, col); ctx.fill(cx - 7, cy + 1, cx, cy + 5, col);
                ctx.fill(cx + 1, cy - 3, cx + 6, cy + 1, col); ctx.fill(cx, cy + 1, cx + 7, cy + 5, col);
            }
            case 2 -> {
                ctx.fill(cx - 6, cy - 5, cx - 1, cy, 0xFFE8C56A);
                ctx.fill(cx + 1, cy - 5, cx + 6, cy, 0xFF5AA9E6);
                ctx.fill(cx - 6, cy + 1, cx - 1, cy + 6, 0xFF7CCB6E);
                ctx.fill(cx + 1, cy + 1, cx + 6, cy + 6, 0xFFB98CFF);
            }
            case 3 -> {
                ctx.fill(cx - 1, cy - 7, cx + 1, cy + 7, col);
                ctx.fill(cx - 7, cy - 1, cx + 7, cy + 1, col);
                ctx.fill(cx - 3, cy - 3, cx - 2, cy - 2, col); ctx.fill(cx + 2, cy - 3, cx + 3, cy - 2, col);
                ctx.fill(cx - 3, cy + 2, cx - 2, cy + 3, col); ctx.fill(cx + 2, cy + 2, cx + 3, cy + 3, col);
            }
            case 4 -> {
                ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, col);
                ctx.fill(cx - 2, cy - 7, cx + 2, cy - 5, col); ctx.fill(cx - 2, cy + 5, cx + 2, cy + 7, col);
                ctx.fill(cx - 7, cy - 2, cx - 5, cy + 2, col); ctx.fill(cx + 5, cy - 2, cx + 7, cy + 2, col);
                ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF141018);
            }
            case 5 -> {
                ctx.fill(cx - 7, cy - 4, cx + 7, cy - 3, col); ctx.fill(cx + 2, cy - 6, cx + 4, cy - 1, col);
                ctx.fill(cx - 7, cy, cx + 7, cy + 1, col);     ctx.fill(cx - 4, cy - 2, cx - 2, cy + 3, col);
                ctx.fill(cx - 7, cy + 4, cx + 7, cy + 5, col); ctx.fill(cx, cy + 2, cx + 2, cy + 7, col);
            }
            default -> { }
        }
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
    private static void rgrad(DrawContext ctx, int x, int y, int w, int h, int top, int bot) {
        ctx.fillGradient(x + 1, y, x + w - 1, y + h, top, bot);
        ctx.fillGradient(x, y + 1, x + w, y + h - 1, top, bot);
    }
    private static void rflat(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + w, y + h - 1, c);
    }
    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, c);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + 1, y + h - 1, c);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }
}
