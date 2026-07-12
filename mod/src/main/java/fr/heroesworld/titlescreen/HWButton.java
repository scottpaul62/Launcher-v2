package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Bouton HERO WORLD épuré : fond sombre translucide, bord fin, survol lumineux. */
public class HWButton extends ButtonWidget {
    private final boolean primary;

    public HWButton(int x, int y, int w, int h, Text msg, boolean primary, PressAction onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.primary = primary;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        boolean hov = this.isHovered() || this.isFocused();

        int fill = primary
            ? (hov ? 0xEE2A2110 : 0xDC1D1708)
            : (hov ? 0xDC201B28 : 0xB214121C);
        rrect(ctx, x, y, w, h, fill);

        int border = primary
            ? (hov ? 0xFFF3D889 : 0xFFE8C56A)
            : (hov ? 0xCCE8C56A : 0x3AFFFFFF);
        borderR(ctx, x, y, w, h, border);
        if (hov) ctx.fill(x + 2, y + 1, x + w - 2, y + 2, 0x22FFFFFF); // léger reflet au survol

        int col = primary ? 0xFFFFEFC6 : (hov ? 0xFFFFFFFF : 0xFFD7D1E0);
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawCenteredTextWithShadow(mc.textRenderer, this.getMessage(), x + w / 2, y + (h - 8) / 2, col);
    }

    private static void rrect(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + w, y + h - 1, c);
    }
    private static void borderR(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, c);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + 1, y + h - 1, c);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }
}
