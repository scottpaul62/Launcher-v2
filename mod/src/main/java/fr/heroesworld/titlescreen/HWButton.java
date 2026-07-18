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
    private boolean sounded = false;

    public HWButton(int x, int y, int w, int h, Text msg, int style, int icon, PressAction onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
        this.icon = icon;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean over = isHovered() || isFocused();
        if (over && !sounded && HWClientConfig.menuSounds) {
            sounded = true;
            try {
                MinecraftClient.getInstance().getSoundManager().play(
                    net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.85f, 0.06f));
            } catch (Throwable ignored) {}
        }
        if (!over) sounded = false;
        if (HWClientConfig.reduceMotion) hover = over ? 1f : 0f;
        else hover += ((over ? 1f : 0f) - hover) * 0.3f;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        if (style == PRIMARY) renderPrimary(ctx, tr, x, y, w, h);
        else if (style == DOCK) renderDock(ctx, tr, x, y, w, h);
        else renderSecondary(ctx, tr, x, y, w, h);
    }

    private void renderPrimary(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        if (hover > 0.02f && !HWClientConfig.reduceMotion) HWDraw.glow(ctx, x, y, w, h, 5, 0xE8C56A, hover);
        if (w < 170 && HWTex.drawH3(ctx, "control_active", x, y, w, h, 150)) {
            if (hover > 0.03f) HWDraw.roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 6, ((int) (hover * 0x1C) << 24) | 0xFFE8B0);
        } else if (HWTex.drawH3(ctx, "btn_primary", x, y, w, h, 0)) {
            if (hover > 0.03f) HWDraw.roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 6, ((int) (hover * 0x20) << 24) | 0xFFE8B0);
        } else if (!HWTex.button(ctx, HWTex.GOLD, HWTex.GOLD_HOVER, hover, x, y, w, h, 8))
            HWDraw.panelRaw(ctx, x, y, w, h, 5, lerp(0xE01E1710, 0xE02C2114, hover), lerp(0xFFCBA24E, 0xFFE8C56A, hover));
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, lerp(0xFFE9D4A0, 0xFFFFEFC6, hover));
    }

    private void renderSecondary(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        if (hover > 0.02f && !HWClientConfig.reduceMotion) HWDraw.glow(ctx, x, y, w, h, 4, 0x49BDF2, hover * 0.4f);
        if (w < 170 && HWTex.drawH3(ctx, "control_neutral", x, y, w, h, 150)) {
            if (hover > 0.03f) HWDraw.roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 5, ((int) (hover * 0x12) << 24) | 0xFFFFFF);
        } else if (HWTex.draw9n(ctx, "btn_secondary", x, y, w, h, 44)) {
            if (hover > 0.03f) HWDraw.roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 5, ((int) (hover * 0x14) << 24) | 0xFFFFFF);
        } else if (!HWTex.button(ctx, HWTex.GLASS, HWTex.GLASS_HOVER, hover, x, y, w, h, 7))
            HWDraw.panelRaw(ctx, x, y, w, h, 4, lerp(0x9014121C, 0xC0221D2A, hover), lerp(0x33FFFFFF, 0xB049BDF2, hover));
        boolean hasText = !getMessage().getString().isEmpty();
        if (icon > 0) HWIcons.draw(ctx, icon, hasText ? x + 15 : x + w / 2, y + h / 2, 16);
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, lerp(0xFFCFC9DA, 0xFFFFFFFF, hover));
    }

    private void renderDock(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h) {
        // icone seule, centree ; zoom leger au survol ; label dans une bulle au-dessus du dock
        int cx = x + w / 2, cy = y + h / 2;
        int iconCol = lerp(0xFFCBB98A, 0xFFF3D889, hover);
        boolean small = h < 32; // dock compact (petite fenetre)
        int iconSize = (small ? 14 : 20) + Math.round(hover * 3f);
        if (!HWIcons.draw(ctx, icon, cx, cy, iconSize)) drawIcon(ctx, icon, cx, cy, iconCol);
        // point dore discret sous l'icone survolee (remplace la barre pleine largeur)
        if (hover > 0.15f) {
            int da = (int) (hover * 255f);
            ctx.fill(cx - 1, y + h - 2, cx + 1, y + h, (da << 24) | 0xE8C56A);
        }
        // label : bulle sombre au-dessus, en fondu
        int la = (int) (hover * 255f);
        if (la > 40) {
            String label = getMessage().getString();
            int lw2 = tr.getWidth(label);
            int bx = cx - lw2 / 2 - 7, by = y - 22, bw = lw2 + 14, bh = 16;
            HWDraw.panel(ctx, bx, by, bw, bh, 8, ((int) (hover * 0xE0) << 24) | 0x101318, ((int) (hover * 0x38) << 24) | 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(tr, getMessage(), cx, by + 4, (la << 24) | 0xFFF4E9C8);
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
